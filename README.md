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

## VERSIONS

1.2
- automated Telegram csv send

1.1
- automatic csv reporter done




