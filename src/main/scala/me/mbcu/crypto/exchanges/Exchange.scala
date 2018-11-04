package me.mbcu.crypto.exchanges

import java.math.MathContext
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import akka.actor.{Actor, ActorRef, ActorSystem, Cancellable, PoisonPill}
import akka.dispatch.ExecutionContexts.global
import akka.stream.ActorMaterializer
import me.mbcu.crypto.RootActor.Complete
import me.mbcu.crypto.exchanges.Exchange.AccountBalance.WriteFile
import me.mbcu.crypto.exchanges.Exchange.{AccountBalance, BaseCoin, Finalize, GetTicker, PrepareGetPrice, SendRest}
import me.mbcu.scala.MyLogging
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.collection.immutable.Queue
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try

object Exchange {
  private implicit val ec: ExecutionContextExecutor = global

  val zero = BigDecimal(0)

  trait SendRest{}

  case class GetAccountBalances() extends SendRest

  object PrepareGetPrice

  object Finalize

  case class GetTicker(pair:(String, String)) extends SendRest

  case class AccountBalance(currency: String, available: BigDecimal)

  case class WriteOut(content: Out, fileOutPath: String)

  object AccountBalance {
    implicit val jsonFormat: OFormat[AccountBalance] = Json.format[AccountBalance]

    object Implicits {
      implicit val writes: Writes[AccountBalance] {def writes(env: AccountBalance): JsValue} = new Writes[AccountBalance] {
        def writes(ab: AccountBalance): JsValue = Json.obj(
          "currency" -> ab.currency,
          "available" -> ab.available.bigDecimal.toPlainString
        )
      }

      implicit val reads: Reads[AccountBalance] = (
          (JsPath \ "available").read[String] and
          (JsPath \ "currency").read[BigDecimal]
        ) (AccountBalance.apply _)
    }

    case class WriteFile(path: String, content: String)

  }

  object BaseCoin extends Enumeration {
    type BaseCoin = Value
    val btc,eth,usd,usdt = Value
    def withNameOpt(s: String): Option[Value] = values.find(_.toString == s)
  }

  val startingBases = Queue(BaseCoin.btc, BaseCoin.eth, BaseCoin.usd, BaseCoin.usdt)

  val exchangeMap: Map[String, String] = Map[String, String](elems =
    "hitbtc" -> "me.mbcu.crypto.exchanges.HitBTC",
    "abc" -> "me.mbcu.crypto.exchanges.abc"
  )

  def credsOf(js: JsValue): Seq[Option[(String, String, String, String)]] =
    exchangeMap.map(p => {
      val root = js \ p._1
      if (root.isDefined) {
        val apikey    = (root \ "apikey").as[String]
        val apisecret = (root \ "apisecret").as[String]
        Some(p._1, p._2, apikey, apisecret)
      } else {
        None
      }
    })
    .toSeq

  def filterBalancesForAltCoins(balances: Map[String, AccountBalance]): Seq[String] = balances.filter(p => BaseCoin.withNameOpt(p._1.toLowerCase).isEmpty).keys.toSeq.map(_.toLowerCase)

  def writeFile(path: String, content: String): Unit = Files.write(Paths.get(path), content.getBytes(StandardCharsets.UTF_8))

  def totalAlt(coin: String, coinBalance: BigDecimal, prices: Map[String, BigDecimal], balances: Map[String, AccountBalance]): BigDecimal =
    // totalAlt sums amount of this coin and base coins' equivalent of this coin
    coinBalance + startingBases.map(p => {
      val amount = balances.get(p.toString) match {
        case Some(a) => a.available
        case _ => BigDecimal(0)
      }
      val price  = prices(s"${coin.toLowerCase}_${p.toString.toLowerCase}")
      Try(amount / price).getOrElse(BigDecimal(0))
    }).sum

  def totalGeneral(coin: String, coinBalance:BigDecimal, prices: Map[String, BigDecimal], balances: Map[String, AccountBalance]): BigDecimal = {
    //In BTC = TotalBALANCE.BTC + TotalBALANCE.ETH * PriceHITBTC.ETH-BTC + TotalBALANCE.USDT / PriceHITBTC.BTC-USDT + TotalBALANCE.NOAH * PriceHITBTC.NOAH-BTC
    coinBalance +
      prices.map(p => {
        val priceOpt = if (p._1.contains(coin)) {
          val d = p._1.split("_")
          if (p._1.startsWith(coin)) Some(d.last, p._2, false) else Some(d.head, p._2, true)
        } else {
          None
        }
        priceOpt match {
          case Some(m) =>
            val amount = balances.get(m._1) match {
              case Some(a) => a.available
              case _ => BigDecimal(0)
            }
            if (m._3) amount * m._2 else Try(amount / m._2).getOrElse(BigDecimal(0))

          case _ => zero
        }
    }).sum

  }

}

abstract class Exchange (apikey : String, apisecret: String, outPath: String) extends Actor with MyLogging {
  implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()
  private implicit val ec: ExecutionContextExecutor = global

  val q = new scala.collection.mutable.Queue[SendRest]
  var root: Option[ActorRef] = None
  private var dqCancellable: Option[Cancellable] = None

  var prices  : Map[String, BigDecimal] = Map.empty
  var altCoins: Vector[String] = Vector.empty
  var balances: Map[String, AccountBalance] = Map.empty
  var ticCounts: Int = 0
  val zero = BigDecimal(0)

  def sendRequest(r: SendRest)

  def start()

  override def receive: Receive = {

    case "start" => start()

    case "dequeue" =>
      if (q.nonEmpty) {
        val next = q.dequeue()
        sendRequest(next)
      }

    case PrepareGetPrice =>
      val altCoinPairs = altCoins.flatMap(p => Exchange.startingBases.map(q => (p, q.toString)))
      val baseCoinPairs = Vector(
        (BaseCoin.eth.toString, BaseCoin.btc.toString),
        (BaseCoin.btc.toString, BaseCoin.usdt.toString),
        (BaseCoin.eth.toString, BaseCoin.usdt.toString),
        (BaseCoin.btc.toString, BaseCoin.usd.toString),
        (BaseCoin.eth.toString, BaseCoin.usd.toString)
        /*
        ETH-BTC
        BTC-USDT
        ETH-USDT
         */
      )
      val allPairs = altCoinPairs ++ baseCoinPairs
      ticCounts = allPairs.size
      allPairs.foreach(p => queue(GetTicker(p)))

    case Finalize =>
      val alt = altCoins.map(p => {
        val balance = balances(p.toLowerCase()).available
        Total(p, Exchange.totalAlt(p, balance, prices, balances).bigDecimal.toPlainString)
       }).filter(p => BigDecimal(p.total) > zero)

      val bas = Exchange.startingBases.map(p => {
        val s = p.toString.toLowerCase
        val balance = balances.get(s) match {
          case Some(l) => l.available
          case _ => zero
        }
        Total(s, Exchange.totalGeneral(s, balance, prices, balances).bigDecimal.toPlainString)
      }).filter(p => BigDecimal(p.total) > zero)

      val noZeroPrices = prices.filter(_._2 > zero).map(p => p._1 -> p._2.bigDecimal.toPlainString)
      val balanceString = balances.values.map(p => AccountBalanceString(p.currency, p.available.bigDecimal.toPlainString)).toSeq
      val out = json(Out(balanceString, noZeroPrices, alt ++ bas))
      Exchange.writeFile(outPath, out)
      info(s"Result saved to $outPath")
      root.foreach(_ ! Complete)
  }

  def handleBalances(b: Map[String, AccountBalance]): Unit = {
    balances ++= b.map(p => p._1.toLowerCase -> p._2)
    altCoins ++= Exchange.filterBalancesForAltCoins(b)
    self ! PrepareGetPrice
  }


  def handleTicker(a: GetTicker, price: BigDecimal): Unit = {
    ticCounts -= 1
    val pair = s"${a.pair._1.toLowerCase}_${a.pair._2.toLowerCase}"
    prices += pair -> price
    if(ticCounts == 0) self ! Finalize
  }

  def json(o: Out): String = Json.prettyPrint(Json.toJson(o))

  def queue(a: SendRest): Unit = q += a

  def initDeq = Some(context.system.scheduler.schedule(1 second, 500 millisecond, self, "dequeue"))

  def parse(a: SendRest, url: String, raw: String)



}