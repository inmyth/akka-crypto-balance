# Crypto Balance Aggregator

- Get each of altcoin's balance with all base coins represented in altcoin's equivalent
- Get total account balance represented in base coins

```
java -jar fatjar.jar <path to config> <dir to log> <dir to result>
```



needs
config file for API keys and list of exchanges.

command
somthing somthing select-configfile getbalances


Need results for the following.
1. balance from each exchange.
2. sum of each coins balance
3. price info of all coins that have balance
4. total value of each coin against other coins.



Result
1. balance from each exchange.
————
exchange1
NOAH: 10000
BTC: 10
USDT: 1000
ETH: 199
———
exchange2
NOAH: 10000
BTC: 10
USD: 1000
———
exchange3
NOAH: 10000
BTC: 10
———
.
.
.


TotalBALANCE
2. sum of each coins balance
NOAH:300000
BTC: 30
USDTandUSD: 1000
ETH: 199

————

PriceHITBTC(get price info from hitbtc)
3. price info of all coins that have balance
NOAH-BTC: 0.0000001
NOAH-ETH: 0.00002
NOAH-USDT:0.08
BTC-USDT: 6500
ETH-BTC: 0.031
ETH-USDT: 200

————

TOTAL VALUE BY EACH COIN
4. total value of each coin against other coins.
In NOAH = TotalBALANCE.NOAH + TotalBALANCE.BTC / PriceHITBTC.NOAH-BTC + TotalBALANCE.USDTandUSD / PriceHITBTC.NOH-USD + TotalBALANCE.ETH / PriceHITBTC.NOAH-ETH

In BTC = TotalBALANCE.NOAH * PriceHITBTC.NOAH-BTC + TotalBALANCE.BTC + TotalBALANCE.ETH * PriceHITBTC.ETH-BTC + TotalBALANCE.USDT / PriceHITBTC.BTC-USDT

In ETH = TotalBALANCE.NOAH * PriceHITBTC.NOAH-ETH + TotalBALANCE.BTC / PriceHITBTC.ETH-BTC + TotalBALANCE.ETH + TotalBALANCE.USDTandUSD / PriceHITBTC.ETH-USDT

In USD = TotalBALANCE.NOAH / PriceHITBTC.NOAH-USDT + TotalBALANCE.BTC * PriceHITBTC.BTC-USDT + TotalBALANCE.ETH * PriceHITBTC.ETH-USDT + TotalBALANCE.USDTandUSD

ETH-BTC
BTC-USDT
ETH-USDT