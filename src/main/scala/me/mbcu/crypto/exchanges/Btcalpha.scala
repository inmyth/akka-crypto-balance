package me.mbcu.crypto.exchanges

import me.mbcu.crypto.RootActor.Shutdown
import me.mbcu.crypto.exchanges.Btcalpha.BtcalphaParams
import me.mbcu.crypto.exchanges.Exchange.{AccountBalance, GetAccountBalances, GetTicker, SendRest}
import play.api.libs.json.{JsValue, Json}

import scala.util.{Failure, Success, Try}


object Btcalpha {

  case class BtcalphaParams(sign: String, nonce: String, url: String)

  def getNonce: String = System.currentTimeMillis().toString

  def sanitizeSecret(s: String): String =  StringContext treatEscapes s

  def sign(payload: String, secret: String): String = Exchange.toHex(Exchange.signHmacSHA256(sanitizeSecret(secret), payload), isCapital = false)

  def getBalance(key:String, secret: String): BtcalphaParams = BtcalphaParams(sign(key, secret), getNonce, "https://btc-alpha.com/api/v1/wallets/")

  def parseAccountBalances(js: JsValue): Map[String, AccountBalance] =
    js.as[Array[JsValue]]
      .filter(p => (p \ "balance").as[BigDecimal] > BigDecimal(0))
      .map(p => {
        val currency = (p \ "currency").as[String]
        val available = (p \ "balance").as[BigDecimal]
        currency -> AccountBalance(currency, available)
      })
      .toMap

  def parseTickerForPrice(js: Array[JsValue]): BigDecimal = (js.head \ "close").as[BigDecimal]


}

class Btcalpha(apikey: String, apisecret: String, outpath: String, reqMillis: String) extends Exchange(apikey, apisecret, outpath, reqMillis) {
  import play.api.libs.ws.DefaultBodyReadables._
  import scala.concurrent.duration._
  import scala.language.postfixOps

  override def sendRequest(r: Exchange.SendRest): Unit = {

    r match {
      case a: GetAccountBalances => httpGet(r, Btcalpha.getBalance(apikey, apisecret))

      case a: GetTicker => httpGet(r, s"https://btc-alpha.com/api/charts/${a.pair._1.toUpperCase}_${a.pair._2.toUpperCase}/1/chart/?format=json&limit=1")

    }

  }

  override def parse(a: Exchange.SendRest, url: String, raw: String): Unit = {
    info(s"${self.path.name}: $url $raw")
    val x = Try(Json parse raw)
    x match {

      case Success(js) =>
        val detail = js \ "detail"

        a match {
          case a: GetAccountBalances =>
            if (detail.isDefined){
              val detailString = detail.as[String]
              root.foreach(_ ! Shutdown(Some( s"Btcalpha: $detailString")))
            } else {
              val b = Btcalpha.parseAccountBalances(js)
              handleBalances(b)
            }

          case a: GetTicker =>
            val array = js.as[Array[JsValue]]
            val price = if(array.isEmpty) BigDecimal(0) else Btcalpha.parseTickerForPrice(array)
            handleTicker(a, price)
        }

      case Failure(e) => root.foreach(_ ! Shutdown(Some( s"Btcalpha failed to parse json: $raw")))

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

  def httpGet(a: SendRest, r: BtcalphaParams): Unit = {
    ws.url(r.url)
      .addHttpHeaders("X-KEY"   -> apikey)
      .addHttpHeaders("X-SIGN"  -> r.sign)
      .addHttpHeaders("X-NONCE" -> r.nonce)
      .withRequestTimeout(10 seconds)
      .get()
      .map(response => parse(a, r.url, response.body[String]))
      .recover {
        case e: Exception => queue(a)
      }
  }
}
