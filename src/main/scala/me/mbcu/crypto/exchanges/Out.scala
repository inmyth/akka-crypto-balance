package me.mbcu.crypto.exchanges

import play.api.libs.json.{Json, OFormat}


object Out {
  implicit val jsonFormat: OFormat[Out] = Json.format[Out]
}
case class Out(balances: Seq[AccountBalanceString], prices: Map[String, String], totals: Seq[Total])


object Total {
  implicit val jsonFormat: OFormat[Total] = Json.format[Total]
}

case class Total(name: String, total: String)


case class AccountBalanceString(currency: String, has: String)
object AccountBalanceString {
  implicit val jsonFormat: OFormat[AccountBalanceString] = Json.format[AccountBalanceString]
}