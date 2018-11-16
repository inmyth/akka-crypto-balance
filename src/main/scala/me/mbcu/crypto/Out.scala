package me.mbcu.crypto

import play.api.libs.json.{Json, OFormat}


object Out {
  implicit val jsonFormat: OFormat[Out] = Json.format[Out]
}
case class Out(report: Seq[Result], totalBalances: Seq[Asset], totalValues: Seq[Asset])

object Result {
  implicit val jsonFormat: OFormat[Result] = Json.format[Result]
}
case class Result(name: String, balances: Map[String, Asset], prices: Map[String, String], totals: Seq[Asset])


object Asset {
  implicit val jsonFormat: OFormat[Asset] = Json.format[Asset]
}
case class Asset(currency: String, has: String)



