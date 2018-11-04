package me.mbcu.crypto

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, Props}
import me.mbcu.crypto.RootActor.{Complete, Shutdown}
import me.mbcu.crypto.exchanges.Exchange
import me.mbcu.crypto.exchanges.Exchange.ESettings
import me.mbcu.scala.MyLogging
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

object RootActor{

  case class Shutdown(message: Option[String])

  case class Complete()

}

class RootActor(cfgPath: String, resPath: String) extends Actor with MyLogging{
  var child = 0

  override def receive: Receive = {
    case "start"=>
      val jsCfg = readConfig()
      val m = Exchange.credsOf(jsCfg).flatten
      child = m.size
      initExchanges(m)

    case Complete =>
      child -= 1
      if (child == 0) {
        info("All done")
        self ! Shutdown(None)
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
