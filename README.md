# Crypto Balance Aggregator

- Get each of altcoin's balance with all base coins represented in altcoin's equivalent
- Get total account balance represented in base coins
- config's isProduction == false will set the scheduler at 10s



## Creating Telegram API Key

- Create a Telegram public channel (only works from app not browser)
- Get Channel's id. Id starts with -100... https://stackoverflow.com/a/39943226/1014413
- Create a bot
    - add this guy https://telegram.me/botfather
    - type `/start` and enter name, username
    - you will get APIKey of the bot
- Add the bot as Admin of the channel
- Enter channel's id and API key to config

```
java -jar fatjar.jar <path to config> <dir to log> <dir to result>
```

## Notes

Beware or usdnt and usd, usdt conversion. `usdnt` is mostly used in csv reporting. Some in External, RootActor and config starting and external balances.

Refer to json report for clean result. From here result is converted to csv, including usdnt conversion.

## VERSIONS

1.6
- fixed divided by zero

1.5
- fixed and changed interval

1.3
- added external balanced
- formatted csv
    - price comes from external/hitbtc

1.2
- automated Telegram csv send

1.1
- automatic csv reporter done




