package me.mbcu.crypto

import java.math.MathContext
import java.time.{LocalDateTime, ZoneId}
import java.time.format.{DateTimeFormatter, FormatStyle}
import java.util.Locale

import me.mbcu.crypto.CsvReport.{AmountChange, CsvCoin}
import org.joda.time.DateTime
import org.scalatest.FunSuite
import play.api.libs.json.{JsValue, Json}

import scala.math.BigDecimal.RoundingMode

class CsvReportTest extends FunSuite {
  val in =
    """
      |{
      |  "report" : [ {
      |    "name" : "hitbtc",
      |    "balances" : {
      |      "btc" : {
      |        "currency" : "btc",
      |        "has" : "0.000000007"
      |      },
      |      "eth" : {
      |        "currency" : "eth",
      |        "has" : "0.00000001"
      |      },
      |      "usd" : {
      |        "currency" : "usd",
      |        "has" : "0.0485310"
      |      }
      |    },
      |    "prices" : {
      |      "eth_btc" : "0.031674",
      |      "btc_usd" : "5762.41",
      |      "eth_usd" : "182.54"
      |    },
      |    "totals" : [ {
      |      "currency" : "btc",
      |      "has" : "0.000008429313786374693921466886250718016"
      |    }, {
      |      "currency" : "eth",
      |      "has" : "0.0002660960173392241502378450775149561967"
      |    }, {
      |      "currency" : "usd",
      |      "has" : "0.04857316227"
      |    } ]
      |  }, {
      |    "name" : "fcoin",
      |    "balances" : {
      |      "eth" : {
      |        "currency" : "eth",
      |        "has" : "0.487928765203200000"
      |      },
      |      "usdt" : {
      |        "currency" : "usdt",
      |        "has" : "102.865510495682600000"
      |      },
      |      "noah" : {
      |        "currency" : "noah",
      |        "has" : "9056455.360000000000000000"
      |      }
      |    },
      |    "prices" : {
      |      "btc_usdt" : "5758.620000000",
      |      "noah_eth" : "0.000002640",
      |      "eth_btc" : "0.031657000",
      |      "noah_usdt" : "0.000465100",
      |      "eth_usdt" : "182.300000000"
      |    },
      |    "totals" : [ {
      |      "currency" : "noah",
      |      "has" : "9462445.4504372230018959754500498427"
      |    }, {
      |      "currency" : "btc",
      |      "has" : "0.03330923613939279094551958628977081"
      |    }, {
      |      "currency" : "eth",
      |      "has" : "24.9612359210649805814591332967635765"
      |    }, {
      |      "currency" : "usd",
      |      "has" : "4301.106801832543360000000000000"
      |    }, {
      |      "currency" : "usdt",
      |      "has" : "4403.972312328225960000000000000"
      |    } ]
      |  }, {
      |    "name" : "btcalpha",
      |    "balances" : {
      |      "btc" : {
      |        "currency" : "btc",
      |        "has" : "0.00000003"
      |      },
      |      "usd" : {
      |        "currency" : "usd",
      |        "has" : "0.00000005"
      |      },
      |      "eth" : {
      |        "currency" : "eth",
      |        "has" : "18.80591162"
      |      },
      |      "noah" : {
      |        "currency" : "noah",
      |        "has" : "23634791.19241778"
      |      }
      |    },
      |    "prices" : {
      |      "btc_usdt" : "5500.0",
      |      "noah_eth" : "0.000003",
      |      "eth_btc" : "0.03181",
      |      "btc_usd" : "5814.754",
      |      "noah_btc" : "0.00000009",
      |      "noah_usd" : "0.0005",
      |      "eth_usdt" : "182.2036",
      |      "eth_usd" : "183.95"
      |    },
      |    "totals" : [ {
      |      "currency" : "noah",
      |      "has" : "29903428.7325177800000000000000000003333333"
      |    }, {
      |      "currency" : "btc",
      |      "has" : "2.725347285958399016046216228579919288073063796"
      |    }, {
      |      "currency" : "eth",
      |      "has" : "89.7102861406248071894545103738825738819073655"
      |    }, {
      |      "currency" : "usd",
      |      "has" : "18703.248176646342"
      |    }, {
      |      "currency" : "usdt",
      |      "has" : "3426.504963445832"
      |    } ]
      |  }, {
      |    "name" : "livecoin",
      |    "balances" : {
      |      "noah" : {
      |        "currency" : "noah",
      |        "has" : "7913528.83158360"
      |      },
      |      "ecoreal" : {
      |        "currency" : "ecoreal",
      |        "has" : "50.00000000"
      |      },
      |      "btc" : {
      |        "currency" : "btc",
      |        "has" : "0.40842961"
      |      }
      |    },
      |    "prices" : {
      |      "eth_btc" : "0.03163145",
      |      "ecoreal_btc" : "0.0000125",
      |      "ecoreal_eth" : "0.000389",
      |      "btc_usd" : "5939.65",
      |      "noah_btc" : "0.00000009",
      |      "eth_usd" : "188"
      |    },
      |    "totals" : [ {
      |      "currency" : "noah",
      |      "has" : "12451635.609361377777777777777777778"
      |    }, {
      |      "currency" : "ecoreal",
      |      "has" : "32724.36880000"
      |    }, {
      |      "currency" : "btc",
      |      "has" : "1.1212722048425240"
      |    }, {
      |      "currency" : "eth",
      |      "has" : "12.93158681320331505511128955517373"
      |    }, {
      |      "currency" : "usd",
      |      "has" : "2425.9289330365"
      |    } ]
      |  }, {
      |    "name" : "yobit",
      |    "balances" : {
      |      "eth" : {
      |        "currency" : "eth",
      |        "has" : "16.32464555"
      |      },
      |      "rur" : {
      |        "currency" : "rur",
      |        "has" : "0.00000206"
      |      },
      |      "noah" : {
      |        "currency" : "noah",
      |        "has" : "42710031.8818285"
      |      },
      |      "btc" : {
      |        "currency" : "btc",
      |        "has" : "4.80764285"
      |      },
      |      "usd" : {
      |        "currency" : "usd",
      |        "has" : "0.0495238"
      |      }
      |    },
      |    "prices" : {
      |      "noah_eth" : "0.00000276",
      |      "eth_btc" : "0.03158485",
      |      "btc_usd" : "6329.99632001",
      |      "noah_btc" : "0.00000009",
      |      "noah_usd" : "0.00056104",
      |      "eth_usd" : "199.08018613"
      |    },
      |    "totals" : [ {
      |      "currency" : "rur",
      |      "has" : "0.00000206"
      |    }, {
      |      "currency" : "noah",
      |      "has" : "102043100.69069261088861197045376736613218"
      |    }, {
      |      "currency" : "btc",
      |      "has" : "9.167165024034117733178666942363753748687"
      |    }, {
      |      "currency" : "eth",
      |      "has" : "286.4181546366973372115063505185619331938"
      |    }, {
      |      "currency" : "usd",
      |      "has" : "57644.3608338037262900"
      |    } ]
      |  } ],
      |  "totalBalances" : [ {
      |    "currency" : "eth",
      |    "has" : "35.618485945203200000"
      |  }, {
      |    "currency" : "rur",
      |    "has" : "0.00000206"
      |  }, {
      |    "currency" : "noah",
      |    "has" : "83314807.265829880000000000"
      |  }, {
      |    "currency" : "btc",
      |    "has" : "5.216072497"
      |  }, {
      |    "currency" : "ecoreal",
      |    "has" : "50.00000000"
      |  }, {
      |    "currency" : "usdnt",
      |    "has" : "102.963565345682600000"
      |  } ],
      |  "totalValues" : [ {
      |    "currency" : "eth",
      |    "has" : "414.0215296076077792616815215894593285319040655"
      |  }, {
      |    "currency" : "rur",
      |    "has" : "0.00000206"
      |  }, {
      |    "currency" : "noah",
      |    "has" : "153860610.4830089916682857236815949871655133"
      |  }, {
      |    "currency" : "btc",
      |    "has" : "13.047102180288219914864324224119694564776063796"
      |  }, {
      |    "currency" : "ecoreal",
      |    "has" : "32724.36880000"
      |  }, {
      |    "currency" : "usdnt",
      |    "has" : "90905.170594255439610000000000000"
      |  } ]
      |}
    """.stripMargin
  val now = Json.parse(in).as[Out]
  val old =
    """
      |[
      |  {
      |    "currency": "eth",
      |    "has": "100"
      |  },
      |  {
      |    "currency": "btc",
      |    "has": "50"
      |  },
      |  {
      |    "currency": "noah",
      |    "has": "10000000"
      |  },
      |  {
      |    "currency": "usdnt",
      |    "has": "40000"
      |  }
      |]
    """.stripMargin

  val external =
    """
      |[
      |    {
      |      "currency": "eth",
      |      "has": "300"
      |    },
      |    {
      |      "currency": "btc",
      |      "has": "20"
      |    },
      |    {
      |      "currency": "noah",
      |      "has": "350300000"
      |    },
      |    {
      |      "currency": "usd",
      |      "has": "454344"
      |    }
      |  ]
    """.stripMargin


  val oldTotalBalances = Json.parse(old).as[Array[Asset]].toVector
  val externalBalances = Json.parse(external).as[Array[Asset]].toVector

  test("calculate change test") {
      val newTotalBalances = now.totalBalances.toVector
      val changes = CsvReport.changeOf(oldTotalBalances, newTotalBalances)
      assert(changes.find(_.currency == CsvCoin.usdnt.toString).get.now.setScale(5, RoundingMode.DOWN) == BigDecimal(102.96356).setScale(5, RoundingMode.DOWN))
  }




}
