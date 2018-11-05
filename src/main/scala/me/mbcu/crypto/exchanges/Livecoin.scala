package me.mbcu.crypto.exchanges

import me.mbcu.crypto.RootActor.Shutdown
import me.mbcu.crypto.exchanges.Exchange.{AccountBalance, GetAccountBalances, GetTicker, SendRest}
import me.mbcu.crypto.exchanges.Livecoin.LivecoinParams
import play.api.libs.json.{JsValue, Json}

import scala.util.{Failure, Success, Try}

object Livecoin{

  case class LivecoinParams(sign: String, url: String)

  def sign(sorted:String, secret: String): String = Exchange.toHex(Exchange.signHmacSHA256(secret, sorted))

  def getBalances(secret: String): LivecoinParams = LivecoinParams(sign("", secret), "https://api.livecoin.net/payment/balances")

  def parseAccountBalances(js: JsValue): Map[String, AccountBalance] =
    js.as[Array[JsValue]]
    .filter(p => (p \"type").as[String] == "total" && (p \"value").as[BigDecimal] > BigDecimal("0"))
    .map(p => {
      val currency = (p \ "currency").as[String]
      val available = (p \"value").as[BigDecimal]
      currency -> AccountBalance(currency, available)
    }).toMap

  def parseTickerForPrice(js: JsValue): BigDecimal = (js \ "last").as[BigDecimal]

}

class Livecoin(apikey: String, apisecret: String, outpath: String, reqMillis: String) extends Exchange(apikey, apisecret, outpath, reqMillis) {
  import play.api.libs.ws.DefaultBodyReadables._

  import scala.concurrent.duration._
  import scala.language.postfixOps

  override def sendRequest(r: Exchange.SendRest): Unit =
    r match {

      case a: GetAccountBalances => httpGet(a, Livecoin.getBalances(apisecret))

      case a: GetTicker => httpGet(a, s"https://api.livecoin.net/exchange/ticker?currencyPair=${a.pair._1.toUpperCase}/${a.pair._2.toUpperCase}")

    }

  override def parse(a: Exchange.SendRest, url: String, raw: String): Unit = {
    info(s"${self.path.name}: $url $raw")
    val x = Try(Json parse raw )
    x match {
      case Success(js) =>

        a match {
          case a: GetAccountBalances =>
            val success = js \ "success"
            if (success.isDefined && !success.as[Boolean]){
              root.foreach(_ ! Shutdown(Some("Livecoin wrong API access")))
            } else {
              val b = Livecoin.parseAccountBalances(js)
              handleBalances(b)
            }

          case a: GetTicker =>
            val success = js \ "success"
            val price = if (success.isDefined && !success.as[Boolean])BigDecimal(0) else Livecoin.parseTickerForPrice(js)
            handleTicker(a, price)

        }

      case Failure(e) => root.foreach(_ ! Shutdown(Some( s"Livecoin failed to parse json: $raw")))

    }

  }

  def httpGet(a: SendRest, r: LivecoinParams): Unit = {
    ws.url(r.url)
      .addHttpHeaders("Content-Type" -> "application/x-www-form-urlencoded")
      .addHttpHeaders("Sign" -> r.sign)
      .addHttpHeaders("API-key" -> apikey)
      .withRequestTimeout(10 seconds)
      .get()
      .map(response => parse(a, r.url, response.body[String]))
      .recover {
        case e: Exception => queue(a)
      }
  }

  def httpGet(a: SendRest, url: String): Unit = {
    ws.url(url)
      .withRequestTimeout(10 seconds)
      .get()
      .map(response => parse(a, url, response.body[String]))
      .recover {
        case e: Exception => queue(a)
      }
  }
}
