package me.mbcu.crypto.exchanges

import me.mbcu.crypto.Asset
import me.mbcu.crypto.RootActor.Shutdown
import me.mbcu.crypto.exchanges.Exchange.{AccountBalance, BaseCoin, GetAccountBalances, GetTicker, SendRest}
import play.api.libs.json.Json
import play.api.libs.ws.WSAuthScheme

import scala.util.{Failure, Success, Try}

// use apiKey as externalBalances
class External(apikey: String, apisecret: String = "", reqMillis: String = "400") extends Exchange(apikey, apisecret, reqMillis) {
  import play.api.libs.ws.DefaultBodyReadables._

  import scala.concurrent.duration._
  import scala.language.postfixOps


  override def sendRequest(r: Exchange.SendRest): Unit = {
    r match {
      case a: GetAccountBalances => parse(a, "", "")

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

  override def parse(a: Exchange.SendRest, url: String, raw: String): Unit = {
    info(s"$name: $url $raw")
    a match {
      case a: GetAccountBalances =>
        var b = Json.parse(apikey).as[Array[Asset]].map(p => p.currency -> AccountBalance(p.currency, BigDecimal(p.has))).toMap
        if (!b.exists(_._1 == "noah")) b = b + ("noah" -> AccountBalance("noah", BigDecimal(0)))
        if (!b.exists(_._1 == "usdnt")) b = b + ("usdnt" -> AccountBalance("usdnt", BigDecimal(0)))
        val usdBalance = b.get("usdnt")
        b = b + ("usd" -> AccountBalance("usd", usdBalance.get.available))
        b = b - "usdnt"
        handleBalances(b)

      case a: GetTicker =>
        val x = Try(Json parse raw)
        x match {

          case Success(js) =>
            val error = js \ "error"
            val price = if(error.isDefined) BigDecimal(0) else (js \ "last").as[BigDecimal]
            handleTicker(a, price)

          case Failure(e) => root.foreach(_ ! Shutdown(Some( s"$name failed to parse json: $url")))
        }
    }
  }

}
