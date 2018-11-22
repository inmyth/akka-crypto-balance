package me.mbcu.crypto.exchanges

import me.mbcu.crypto.RootActor.Shutdown
import me.mbcu.crypto.exchanges.Exchange.{AccountBalance, GetAccountBalances, GetTicker, SendRest}
import me.mbcu.crypto.exchanges.Yobit.YobitParams
import play.api.libs.json.{JsObject, JsValue, Json}

import scala.util.{Failure, Success, Try}

object Yobit {

  case class YobitParams(sign: String, params: String)

  def nonce : Long = System.currentTimeMillis() / 1000 - 1530165626l

  def addNonce(params: Map[String, String]): Map[String, String] = params + ("nonce" -> nonce.toString)

  def body(params: Map[String, String]): String = params.toSeq.sortBy(_._1).map(v => s"${v._1}=${v._2}").mkString("&")

  def toYobitParams(params : Map[String, String], secret: String) : YobitParams = {
    val withNonce = body(addNonce(params))
    YobitParams(Exchange.toHex(Exchange.signHmacSHA512(secret, withNonce), isCapital = false), withNonce)
  }

  def getAccountBalance(secret:String) : YobitParams = toYobitParams(Map("method"-> "getInfo"), secret)

  def parseAccountBalances(js: JsValue): Map[String, AccountBalance] =
    (js\ "return" \ "funds_incl_orders").as[JsObject].fields
      .filter(p => p._2.as[BigDecimal] > BigDecimal(0))
      .map(p => p._1 -> AccountBalance(p._1, p._2.as[BigDecimal]))
      .toMap

  def parseTickerForPrice(js: JsValue): BigDecimal = (js.as[JsObject].fields.head._2 \ "last").as[BigDecimal]

}

class Yobit(apikey: String, apisecret: String, reqMillis: String) extends Exchange(apikey, apisecret, reqMillis) {
  import play.api.libs.ws.DefaultBodyReadables._
  import play.api.libs.ws.DefaultBodyWritables._

  import scala.concurrent.duration._
  import scala.language.postfixOps

  override def sendRequest(r: Exchange.SendRest): Unit = {
    r match {

      case a: GetAccountBalances => httpPost(a, Yobit.getAccountBalance(apisecret))

      case a: GetTicker => httpGet(a, s"https://yobit.net/api/3/ticker/${a.pair._1.toLowerCase}_${a.pair._2.toLowerCase}")
    }
  }

  override def parse(a: Exchange.SendRest, url: String, raw: String): Unit = {
    info(s"$name: $url $raw")
    if (raw contains "Ddos"){
      queue(a)
    } else {
      val x = Try(Json parse raw)
      x match {
        case Success(js) =>

          a match {
            case a: GetAccountBalances =>
              val success = (js \ "success").as[Int]
              success match {
                case 1 =>
                  val b = Yobit.parseAccountBalances(js)
                  handleBalances(b)

                case 0 =>
                  val error = (js \ "error").as[String]
                  root.foreach(_ ! Shutdown(Some(s"$name error $error")))
              }

            case a: GetTicker =>
              val error = js \ "error"
              val price = if(error.isDefined) BigDecimal(0) else Yobit.parseTickerForPrice(js)
              handleTicker(a, price)

          }

        case Failure(e) => root.foreach(_ ! Shutdown(Some( s"$name failed to parse json: $url")))

      }
    }

  }

  def httpGet(a: SendRest, url: String): Unit =
    ws.url(url)
      .withRequestTimeout(10 seconds)
      .get()
      .map(response => parse(a, url, response.body[String]))
      .recover {
        case e: Exception => queue(a)
      }

  def httpPost(a: SendRest, r: YobitParams): Unit =
    ws.url("https://yobit.net/tapi/")
      .addHttpHeaders("Content-Type" -> "application/x-www-form-urlencoded")
      .addHttpHeaders("Key" -> apikey )
      .addHttpHeaders("Sign" -> r.sign)
      .withRequestTimeout(10 seconds)
      .post(r.params)
      .map(response => parse(a, r.params, response.body[String]))
      .recover {
        case e: Exception => queue(a)
      }
}
