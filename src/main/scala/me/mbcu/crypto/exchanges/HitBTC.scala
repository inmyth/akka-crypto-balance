package me.mbcu.crypto.exchanges

import me.mbcu.crypto.RootActor.Shutdown
import me.mbcu.crypto.exchanges.Exchange.{AccountBalance, GetAccountBalances, GetTicker, SendRest}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSAuthScheme

import scala.util.{Failure, Success, Try}

object HitBTC {

  def parseAccountBalances(js: JsValue): Map[String, AccountBalance] =
    js.as[Array[JsValue]]
      .filter(p => (p \ "available").as[BigDecimal] > BigDecimal(0))
      .map(p => {
        val currency = (p \ "currency").as[String]
        val available = (p \ "available").as[BigDecimal] + (p \ "reserved").as[BigDecimal]
        currency -> AccountBalance(currency, available)
      })
    .toMap


}

class HitBTC(apikey: String, apisecret: String, outpath: String, reqMillis: String) extends Exchange(apikey, apisecret, outpath, reqMillis) {
  import play.api.libs.ws.DefaultBodyReadables._

  import scala.concurrent.duration._
  import scala.language.postfixOps

  var basePrices: Map[String, BigDecimal]  = Map.empty


  override def sendRequest(r: Exchange.SendRest): Unit = {
    r match {
      case a: GetAccountBalances => httpGet(a, "https://api.hitbtc.com/api/2/trading/balance")

      case a: GetTicker => httpGet(a, s"https://api.hitbtc.com/api/2/public/ticker/${a.pair._1.toUpperCase}${a.pair._2.toUpperCase}")

    }

    def httpGet(a: SendRest, url: String): Unit =
      ws.url(url)
        .withRequestTimeout(10 seconds)
        .withAuth(apikey, apisecret, WSAuthScheme.BASIC)
        .get()
        .map(response => parse(a, url, response.body[String]))
        .recover {
          case e: Exception => queue(a)
        }
  }

  override def parse(a: SendRest, url: String, raw: String): Unit = {
    info(s"$name: $url $raw")
    val x = Try(Json parse raw)
    x match {
      case Success(js) =>
        val error = js \ "error"

        a match {

          case a: GetAccountBalances =>
            if (error.isDefined) {
              root.foreach(_ ! Shutdown(Some( s"$name wrong API access $url")))
            } else {
              val b = HitBTC.parseAccountBalances(js)
              handleBalances(b)
            }

          case a: GetTicker =>
            val price = if(error.isDefined) BigDecimal(0) else (js \ "last").as[BigDecimal]
            handleTicker(a, price)

        }

      case Failure(e) => root.foreach(_ ! Shutdown(Some( s"$name failed to parse json: $url")))

    }

  }


}


