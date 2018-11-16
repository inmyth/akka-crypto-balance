package me.mbcu.crypto

import me.mbcu.crypto.CsvReport.CsvCoin.CsvCoin
import me.mbcu.crypto.exchanges.Exchange.BaseCoin.BaseCoin

object CsvReport {

  case class AmountChange(currency: String, old: BigDecimal, now: BigDecimal, diff: BigDecimal)

  object CsvCoin extends Enumeration {
    type CsvCoin = Value
    val btc, eth, usdnt, noah = Value
    def withNameOpt(s: String): Option[Value] = values.find(_.toString == s)
    def fromBaseCoinOpt(c: BaseCoin): Option[Value] = values.find(_.toString == c.toString)
  }

  val csvCoins = Vector(CsvCoin.noah, CsvCoin.btc, CsvCoin.eth, CsvCoin.usdnt)

  def changeOf(old: Vector[Asset], now: Vector[Asset]): Vector[AmountChange] =
    csvCoins.map(p => {
      val o = BigDecimal(old.find(_.currency == p.toString).getOrElse(Asset(p.toString, "0")).has)
      val n = BigDecimal(now.find(_.currency == p.toString).getOrElse(Asset(p.toString, "0")).has)
      val d = (n - o) / o * 100
      AmountChange(p.toString, o, n, d)
    })

  def averageOf(in: Vector[BigDecimal]): BigDecimal = in.sum / in.size


}


