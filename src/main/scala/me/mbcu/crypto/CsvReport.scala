package me.mbcu.crypto

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

  def build(now: Out, oldTotalBalances: Vector[Asset]): String = {
    val sb = new StringBuffer("Exchange,")
    sb.append(CsvReport.csvCoins.mkString(","))
    sb.append("\n")
    val report = now.report
    report.map(p => {
      val r = new StringBuffer(p.name)
      r.append(",")
      val z = CsvReport.csvCoins.map(q => p.balances.find(_._1 == q.toString).getOrElse(q.toString -> Asset(q.toString, "0"))._2.has)
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
    report.foreach(p => {
      sb.append(p.name)
      sb.append(",prices\n")
      p.prices
        .map(p => {
          val z = p._1.split("_")
          (z(0), z(1), BigDecimal(p._2))
        })
        .foreach(p => {
        sb.append(p._1)
        sb.append("/")
        sb.append(p._2)
        sb.append(",")
        sb.append(p._3)
        sb.append("\n")
      })
      sb.append("\n")
      //      CsvReport.csvCoins
      //        .flatMap(p => CsvReport.csvCoins.map(q => (p, q)))
      //        .foreach(println)
    })

    sb.append("header,Aggregated Value\n")
    val totalValues = now.totalValues
    CsvReport.csvCoins.foreach(p => {
      val v = totalValues.find(_.currency == p.toString).getOrElse(Asset(p.toString, "0")).has
      sb.append(p.toString)
      sb.append(",")
      sb.append(v)
      sb.append("\n")
    })
    sb.append("\n")
    sb.append("header, Total Amount, Start Amount, Change %\n")
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
    sb.append("\n")
    sb.append("Total Change,,,")
    val totalChange = CsvReport.averageOf(changes.map(_.diff))
    sb.append(totalChange)
    sb.append("\n")
    sb.toString
  }

}



