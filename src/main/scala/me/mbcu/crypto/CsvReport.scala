package me.mbcu.crypto

import me.mbcu.crypto.CsvReport.CsvCoin.CsvCoin
import me.mbcu.crypto.exchanges.Exchange
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
      val d = if (o.compare(BigDecimal("0")) == 0) n else (n - o) / o * 100
      AmountChange(p.toString, o, n, d)
    })

  def averageOf(in: Vector[BigDecimal]): BigDecimal = in.sum / in.size

  def build(now: Out, oldTotalBalances: Vector[Asset], startTotalBalances: Vector[Asset]): String = {
    val sb = new StringBuffer("Exchange,")
    sb.append(CsvReport.csvCoins.mkString(","))
    sb.append("\n")
    val report = now.report
    report.map(p => {
      val r = new StringBuffer(p.name)
      r.append(",")
      val usd = p.balances.getOrElse("usd", Asset("usd", "0"))
      val usdt = p.balances.getOrElse("usdt", Asset("usdt", "0"))
      val usdnt = CsvCoin.usdnt.toString -> Asset(CsvCoin.usdnt.toString, (BigDecimal(usd.has) + BigDecimal(usdt.has)).bigDecimal.toPlainString)
      val balancesWithUsdnt = p.balances + usdnt
      val z = CsvReport.csvCoins.map(q => balancesWithUsdnt.find(_._1 == q.toString).getOrElse(q.toString -> Asset(q.toString, "0"))._2.has)
      r.append(z.mkString(","))
      r.append("\n")
      r.toString
    }).foreach(sb.append)
    val totalBalances = now.totalBalances
    val totals = CsvReport.csvCoins.map(q => BigDecimal(totalBalances.find(_.currency == q.toString).getOrElse(Asset(q.toString, "0")).has)).mkString(",")
    sb.append("total,")
    sb.append(totals)
    sb.append("\n")
    sb.append("\n")

    val ext = report.find(_.name == "external").get
    sb.append("hitbtc prices,")
    sb.append(csvCoins.mkString(","))
    sb.append("\n")

    val prices = ext.prices
      .map(p => {
        val z = p._1.split("_")
        (z(0), z(1), BigDecimal(p._2))
      })
    val usdtPrices = prices.filter(p => p._2 == "usd").map(p => (p._1, CsvCoin.usdnt.toString, p._3))
    val pricesWithUsdt = prices ++ usdtPrices

    def buildPrices(left: CsvCoin): String = {
      csvCoins.map(p => pricesWithUsdt.find(q => q._1 == left.toString && q._2 == p.toString).getOrElse(("", "", BigDecimal("0")))._3.bigDecimal.toPlainString).mkString(",")
    }
    csvCoins.foreach(p => {
      sb.append(p.toString)
      sb.append(",")
      sb.append(buildPrices(p))
      sb.append("\n")
    })
    sb.append("\n")

    sb.append("Values,Aggregated Value\n")
    val totalValues = now.totalValues
    CsvReport.csvCoins.foreach(p => {
      val v = totalValues.find(_.currency == p.toString).getOrElse(Asset(p.toString, "0")).has
      sb.append(p.toString)
      sb.append(",")
      sb.append(v)
      sb.append("\n")
    })
    sb.append("\n")
    sb.append("Daily change, Total Amount, Start Amount, Change %\n")
    val newTotalBalances = now.totalBalances.toVector
    val changes = CsvReport.changeOf(oldTotalBalances, newTotalBalances)
    CsvReport.csvCoins.foreach(p => {
      val v = changes.find(_.currency == p.toString).getOrElse(AmountChange(p.toString, BigDecimal("0"), BigDecimal("0"), BigDecimal("0")))
      sb.append(v.currency)
      sb.append(",")
      sb.append(v.now)
      sb.append(",")
      sb.append(v.old)
      sb.append(",")
      sb.append(v.diff)
      sb.append("\n")
    })
    sb.append("Average daily change,,,")
    val avgDailyChange = CsvReport.averageOf(changes.map(_.diff))
    sb.append(avgDailyChange)
    sb.append("\n")
    sb.append("\n")
    sb.append("Total change (from start), Total Amount, Start Amount, Change %\n")
    val changesFromStart = CsvReport.changeOf(startTotalBalances, newTotalBalances)
    CsvReport.csvCoins.foreach(p => {
      val v = changesFromStart.find(_.currency == p.toString).getOrElse(AmountChange(p.toString, BigDecimal("0"), BigDecimal("0"), BigDecimal("0")))
      sb.append(v.currency)
      sb.append(",")
      sb.append(v.now)
      sb.append(",")
      sb.append(v.old)
      sb.append(",")
      sb.append(v.diff)
      sb.append("\n")
    })
    sb.append("Total average change,,,")
    val avgTotalChange = CsvReport.averageOf(changes.map(_.diff))
    sb.append(avgTotalChange)
    sb.append("\n")
    sb.toString
  }


}



