package me.mbcu.crypto.exchanges

import java.util.Base64

import me.mbcu.crypto.RootActor.Shutdown
import me.mbcu.crypto.exchanges.Exchange.{AccountBalance, GetAccountBalances, GetTicker, SendRest}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSAuthScheme

import scala.util.{Failure, Success, Try}

object Fcoin {

  def parseAccountBalances(js: JsValue): Map[String, AccountBalance] =
    (js \ "data").as[Array[JsValue]]
      .filter(p => (p \ "balance").as[BigDecimal] > BigDecimal(0))
      .map(p => {
        val currency  = (p \ "currency").as[String]
        val available = (p \ "balance").as[BigDecimal]
        currency -> AccountBalance((p \ "currency").as[String], available)
      })
      .toMap

  def parseTickerForPrice(js: JsValue): BigDecimal = (js \ "data" \ "ticker").as[Array[JsValue]].head.as[BigDecimal]
}

class Fcoin(apikey: String, apisecret: String, outpath: String, reqMillis: String) extends Exchange(apikey, apisecret, outpath, reqMillis) {
  import play.api.libs.ws.DefaultBodyReadables._

  import scala.concurrent.duration._
  import scala.language.postfixOps

  override def sendRequest(r: Exchange.SendRest): Unit =
  r match {
    case a: GetAccountBalances => httpGet(a, getBalance)

    case a: GetTicker => httpGet(a, s"https://api.fcoin.com/v2/market/ticker/${a.pair._1.toLowerCase}${a.pair._2.toLowerCase}")

  }

  def httpGet(a: SendRest, url: String): Unit = {
    ws.url(url)
      .withRequestTimeout(10 seconds)
      .withAuth(apikey, apisecret, WSAuthScheme.BASIC)
      .get()
      .map(response => parse(a, url, response.body[String]))
      .recover {
        case e: Exception => queue(a)
      }
  }

  def httpGet(a: SendRest, f: FcoinParams): Unit = {
    ws.url(f.url)
      .addHttpHeaders("FC-ACCESS-KEY" -> apikey)
      .addHttpHeaders("FC-ACCESS-SIGNATURE" -> f.sign)
      .addHttpHeaders("FC-ACCESS-TIMESTAMP" -> f.ts.toString)
      .withRequestTimeout(10 seconds)
      .get()
      .map(response => parse(a, f.url, response.body[String]))
      .recover {
        case e: Exception => queue(a)
      }
  }


  override def parse(a: Exchange.SendRest, url: String, raw: String): Unit = {
    info(raw)
    val x = Try(Json parse raw)
    x match {
      case Success(js) =>
        val status = (js \ "status").as[Int]

        status match {
          case v if v == 0 || v == 40003 =>
            a match {

              case a: GetAccountBalances =>
                val b = Fcoin.parseAccountBalances(js)
                handleBalances(b)

              case a: GetTicker =>
                val price = if(status == 40003) BigDecimal(0) else Fcoin.parseTickerForPrice(js)
                handleTicker(a, price)

            }

          case 6005 => root.foreach(_ ! Shutdown(Some( s"Fcoin wrong API key $url")))
        }

      case Failure(e) => root.foreach(_ ! Shutdown(Some( s"failed to parse json: $url")))

    }

  }

  def getBalance: FcoinParams = sign(apisecret, "https://api.fcoin.com/v2/accounts/balance")

  case class FcoinParams(sign: String, ts: Long, url: String)

  def sign(secret: String, baseUrl: String) : FcoinParams = {
    val ts = System.currentTimeMillis()
    val template = "%s%s%s%s"
    val formatted = template.format("GET", baseUrl, ts, "")

    val first = Base64.getEncoder.encodeToString(formatted.getBytes)
    val second = Exchange.signHmacSHA1(secret, first)
    val signed = Base64.getEncoder.encodeToString(second)
    FcoinParams(signed, ts, baseUrl)
  }
}
