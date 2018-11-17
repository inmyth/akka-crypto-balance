package me.mbcu.crypto

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, Props}
import akka.dispatch.ExecutionContexts.global
import me.mbcu.crypto.CsvReport.CsvCoin
import me.mbcu.crypto.exchanges.Exchange
import me.mbcu.crypto.exchanges.Exchange.{BaseCoin, ESettings}
import me.mbcu.scala.MyLogging
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.{HttpMultipartMode, MultipartEntityBuilder}
import org.apache.http.entity.mime.content.{FileBody, StringBody}
import org.apache.http.impl.client.HttpClients
import play.api.libs.json.{JsValue, Json}

import scala.collection.mutable
import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor}
import scala.language.postfixOps

object RootActor{

  case class Shutdown(message: Option[String])

  case class Complete(result: Result)

  def read(path: String ): JsValue = {
    val source = scala.io.Source.fromFile(path)
    val rawJson = try source.mkString finally source.close()
    Json.parse(rawJson)
  }

  def writeFile(path: String, content: String): Unit = Files.write(Paths.get(path), content.getBytes(StandardCharsets.UTF_8))

  def buildAssetsSum(in: Seq[Asset]): Seq[Asset] = {
    val allBalances =  in.groupBy(_.currency).map(p => {
      val sum  = p._2.map(a => BigDecimal(a.has)).sum
      Asset(p._1, sum.bigDecimal.toPlainString)
    }).toSeq

    val usdt  = allBalances.find(_.currency == BaseCoin.usdt.toString).getOrElse(Asset(BaseCoin.usdt.toString, "0"))
    val usd   = allBalances.find(_.currency == BaseCoin.usd.toString).getOrElse(Asset(BaseCoin.usd.toString, "0"))
    val usdnt = Asset(CsvCoin.usdnt.toString, (BigDecimal(usdt.has) + BigDecimal(usd.has)).bigDecimal.toPlainString)
    val noUsdAllBalances = allBalances.filterNot(p => p.currency == BaseCoin.usdt.toString || p.currency == BaseCoin.usd.toString)
    noUsdAllBalances :+ usdnt
  }

  def currentTime: String = LocalDateTime.now(ZoneId.of("Asia/Tokyo")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))

  def telegram(apiKey: String, channel: String, resultPath: String, fileName: String): Unit = {
    val client = HttpClients.createDefault()
    val file = new File(s"$resultPath/$fileName")
    val post = new HttpPost(s"https://api.telegram.org/bot$apiKey/sendDocument")
    val fileBody =  new FileBody(file, ContentType.DEFAULT_BINARY)
    val builder = MultipartEntityBuilder.create()
    builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
    builder.addBinaryBody("document", file, ContentType.APPLICATION_OCTET_STREAM, fileName)
    builder.addPart("chat_id", new StringBody(channel, ContentType.MULTIPART_FORM_DATA))
    val entity = builder.build
    post.setEntity(entity)
    client.execute(post)
    client.close()
  }

}

class RootActor(cfgPath: String, resPath: String) extends Actor with MyLogging{
  private implicit val ec: ExecutionContextExecutor = global
  import RootActor._
  var startingBalance: Seq[Asset] = Seq.empty
  var telegramApiKey  = ""
  var telegramChannel = ""
  var creds: Seq[(String, ESettings, String, String)] = Seq.empty
  var child = 0
  val res: mutable.Buffer[Result] = mutable.Buffer.empty
  var intervalSec: Int = 0
  var lastFile: String = "fresh"


  override def receive: Receive = {
    case "start"=>
      val jsCfg = read(cfgPath)
      creds           ++= Exchange.credsOf((jsCfg \ "apiKeys").as[JsValue]).flatten
      startingBalance ++= (jsCfg \ "startingBalances").as[Array[Asset]]
      telegramApiKey  = (jsCfg \ "telegram" \ "apiKey").as[String]
      telegramChannel = (jsCfg \ "telegram" \ "channel").as[String]
      intervalSec     = if ((jsCfg \ "env" \ "isProduction").as[Boolean]) 24 * 86400 else 10
      self ! "reset"

    case "reset" =>
      child = creds.size
      res.clear
      initExchanges(creds)

    case Complete(r) =>
      res += r
      if (res.size == child){
        val allBalances = buildAssetsSum(res.flatMap(_.balances.values))
        val allTotals = buildAssetsSum(res.flatMap(_.totals))
        val out = Out(res, allBalances, allTotals)
        val json = Json.prettyPrint(Json.toJson(out))

        val newFileName = currentTime
        RootActor.writeFile(s"$resPath/$newFileName.json", json)
        val oldTotalBalances = if (lastFile == "fresh") startingBalance else read(s"$resPath/$lastFile.json").as[Out].totalBalances
        val csvReport = CsvReport.build(out, oldTotalBalances.toVector)
        val csvFileName = s"$newFileName.csv"
        RootActor.writeFile(s"$resPath/$csvFileName", csvReport)
        telegram(telegramApiKey, telegramChannel, resPath, csvFileName)
        lastFile = newFileName
        context.system.scheduler.scheduleOnce(intervalSec second, self, "reset")
      }

    case Shutdown(message: Option[String]) =>
      message.foreach(error)
      implicit val executionContext: ExecutionContext = context.system.dispatcher
      Await.ready(context.system.terminate(), Duration(2, TimeUnit.SECONDS))
  }



  def initExchanges(m : Seq[(String, ESettings, String, String)]) : Unit =
    m.map(p => {
      val o = Class.forName(p._2.classPath).getConstructors()(0)
      val args = Array[AnyRef](p._3, p._4, s"$resPath/${p._1}", p._2.rateMillis.toString)
      context.actorOf(Props(o.newInstance(args:_*).asInstanceOf[Exchange]), p._1)
    }).foreach(_ ! "start")

}
