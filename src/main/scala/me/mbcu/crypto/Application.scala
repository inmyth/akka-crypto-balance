package me.mbcu.crypto

import java.util.TimeZone
import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Props}
import akka.dispatch.ExecutionContexts.global
import akka.stream.ActorMaterializer
import me.mbcu.scala.MyLoggingSingle

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor}

object Application extends App {
  implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()
  private implicit val ec: ExecutionContextExecutor = global

  if (args.length != 3){
    println("Requires two arguments : <config file path>  <log dir path> <result dir path>")
    System.exit(-1)
  }

  MyLoggingSingle.init(args(1), TimeZone.getTimeZone("Asia/Tokyo"))
  val rootActor = system.actorOf(Props(new RootActor(args(0), args(2))), name = "root")
  rootActor ! "start"
//  Await.ready(system.whenTerminated, Duration(5, TimeUnit.MINUTES))
}


