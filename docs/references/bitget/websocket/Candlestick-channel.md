# Candlestick Channel

## Description

The channel will push a snapshot after successful subscribed, later on the updates will be pushed

If intended to query history data in a customized time range, please refer to Get Candle Data

When there are transactions in the K-line channel, data is pushed once per second.

When there are no transactions, data is pushed once at the specified time granularity.

Request Example

```json
{
  "op": "subscribe",
  "args": [
    {
      "instType": "USDT-FUTURES",
      "channel": "candle1m",
      "instId": "BTCUSDT"
    }
  ]
}
```

Request Parameters

| Parameter  | Type         | Required | Description                                                                                                                                                                                                                                                                                                                                                                                                                                              |
|------------|--------------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| op         | String       | Yes      | Operation, subscribe unsubscribe                                                                                                                                                                                                                                                                                                                                                                                                                         |
| args       | List<Object> | Yes      | List of channels to request subscription                                                                                                                                                                                                                                                                                                                                                                                                                 |
| > instType | String       | Yes      | Product type                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| > channel  | String       | Yes      | Channel name, candle1m (1 minute) candle5m (5 minutes) candle15m (15 minutes) candle30m (30minutes) candle1H (1 hour) candle4H (4 hours) candle12H (12 hours) candle1D (1 day) candle1W (1 week) candle6H (6 hours) candle3D (3 days) candle1M (1-month line) candle6Hutc (6-hour line, UTC) candle12Hutc (12-hour line, UTC) candle1Dutc (1-day line, UTC) candle3Dutc (3-day line, UTC) candle1Wutc (weekly line, UTC) candle1Mutc (monthly line. UTC) |
| >instId    | String       | Yes      | Product ID E.g. ETHUSDT                                                                                                                                                                                                                                                                                                                                                                                                                                  | 

Response Example

```json
 {
  "event": "subscribe",
  "arg": {
    "instType": "USDT-FUTURES",
    "channel": "candle1m",
    "instId": "BTCUSDT"
  }
}
```

Response Parameters

| Parameter  | Type   | Description                                                                                                                                                                                                                                                                                                                                                                                                                                               |
|------------|--------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| event      | String | Yes  Event                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| arg        | Object | Subscribed channels                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| > channel  | String | Channel name, candle1m (1 minute) candle5m (5 minutes) candle15m (15 minutes) candle30m (30 minutes) candle1H (1 hour) candle4H (4 hours) candle12H (12 hours) candle1D (1 day) candle1W (1 week) candle6H (6 hours) candle3D (3 days) candle1M (1-month line) candle6Hutc (6-hour line, UTC) candle12Hutc (12-hour line, UTC) candle1Dutc (1-day line, UTC) candle3Dutc (3-day line, UTC) candle1Wutc (weekly line, UTC) candle1Mutc (monthly line. UTC) |
| > instType | String | Product type                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| > instId   | String | Product E.g. ETHUSDT                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| code       | String | Error code, returned only on error                                                                                                                                                                                                                                                                                                                                                                                                                        |                                                                                                                                                                                                                                                                                                                                                                                                                        
| msg        | String | Error message                                                                                                                                                                                                                                                                                                                                                                                                                                             |

Push Data

```json
{
  "action": "snapshot",
  "arg": {
    "instType": "USDT-FUTURES",
    "channel": "candle1m",
    "instId": "BTCUSDT"
  },
  "data": [
    [
      "1695685500000",
      "27000",
      "27000.5",
      "27000",
      "27000.5",
      "0.057",
      "1539.0155",
      "1539.0155"
    ]
  ],
  "ts": 1695715462250
}
```

Push Parameters

| Parameter  | Type         | Description                                                          |
|------------|--------------|----------------------------------------------------------------------|
| arg        | Object       | Channels with successful subscription                                |
| > channel  | String       | Channel name                                                         |
| > instId   | String       | Product ID                                                           |
| > instType | String       | Product type                                                         |
| data       | List<String> | Subscription data                                                    |
| > index[0] | String       | Start time, milliseconds format of Unix timestamp, e.g.1597026383085 |
| > index[1] | String       | Opening price                                                        |
| > index[2] | String       | Highest price                                                        |
| > index[3] | String       | Lowest price                                                         |
| > index[4] | String       | Closing price                                                        |
| > index[5] | String       | The value is the trading volume of left coin                         |
| > index[6] | String       | Trading volume of quote currency                                     |
| > index[7] | String       | Trading volume of USDT                                               |
| > ts       | String       | Data streaming time                                                  |
