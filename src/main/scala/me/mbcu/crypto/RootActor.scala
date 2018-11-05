package me.mbcu.crypto

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, PoisonPill, Props}
import me.mbcu.crypto.RootActor.{Complete, Shutdown}
import me.mbcu.crypto.exchanges.Exchange.{BaseCoin, ESettings}
import me.mbcu.crypto.exchanges.Exchange
import me.mbcu.scala.MyLogging
import play.api.libs.json.{JsValue, Json}

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

object RootActor{

  case class Shutdown(message: Option[String])

  case class Complete(result: Result)

  def writeFile(path: String, content: String): Unit = Files.write(Paths.get(path), content.getBytes(StandardCharsets.UTF_8))

}

class RootActor(cfgPath: String, resPath: String) extends Actor with MyLogging{
  var child = 0
  val res: mutable.Buffer[Result] = mutable.Buffer.empty

  override def receive: Receive = {
    case "start"=>
      val jsCfg = readConfig()
      val m = Exchange.credsOf(jsCfg).flatten
      child = m.size
      initExchanges(m)

    case Complete(r) =>
      res += r
      if (res.size == child){
       val allBalances =  res.flatMap(_.balances.values).groupBy(_.currency).map(p => {
          val sum  = p._2.map(a => BigDecimal(a.has)).sum
          AccountBalanceString(p._1, sum.bigDecimal.toPlainString)
        }).toSeq

        val usdt = allBalances.find(_.currency == BaseCoin.usdt.toString).getOrElse(AccountBalanceString(BaseCoin.usdt.toString, "0"))
        val usd  = allBalances.find(_.currency == BaseCoin.usd.toString).getOrElse(AccountBalanceString(BaseCoin.usd.toString, "0"))
        val bUsd = AccountBalanceString("usdAndT", (BigDecimal(usdt.has) + BigDecimal(usd.has)).bigDecimal.toPlainString)
        val noUsdAllBalances = allBalances.filterNot(p => p.currency == BaseCoin.usdt.toString || p.currency == BaseCoin.usd.toString)
        val out = Out(res, noUsdAllBalances :+ bUsd)
        val json = Json.prettyPrint(Json.toJson(out))
        RootActor.writeFile(s"$resPath/out.txt", json)
        info("All Done, Plz Shut Down")
        self ! Shutdown(None)
        self ! PoisonPill
      }

    case Shutdown(message: Option[String]) =>
      message.foreach(error)
      implicit val executionContext: ExecutionContext = context.system.dispatcher
      Await.ready(context.system.terminate(), Duration(2, TimeUnit.SECONDS))
  }

  def readConfig(): JsValue = {
    val source = scala.io.Source.fromFile(cfgPath)
    val rawJson = try source.mkString finally source.close()
    Json.parse(rawJson)
  }

  def initExchanges(m : Seq[(String, ESettings, String, String)]) : Unit =
    m.map(p => {
      val o = Class.forName(p._2.classPath).getConstructors()(0)
      val args = Array[AnyRef](p._3, p._4, s"$resPath/${p._1}", p._2.rateMillis.toString)
      context.actorOf(Props(o.newInstance(args:_*).asInstanceOf[Exchange]), p._1)
    }).foreach(_ ! "start")

}
