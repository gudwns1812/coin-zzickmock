---
sidebar_position: 40
hide_table_of_contents: true
---

# Get Current Funding Rate

### Description

Get current funding rate.

### HTTP Request

- GET /api/v3/market/current-fund-rate
- Rate limit: 20次/sec/IP

<div class="api-aligning">

```bash title=Request Example
curl "https://api.bitget.com/api/v3/market/current-fund-rate?symbol=BTCUSDT" \

```

### Request Parameters

| Parameter | Type   | Required | Description                                         |
|:----------|:-------|:---------|:----------------------------------------------------|
| symbol    | String | Yes      | Trading pair, based on the symbolName, i.e. BTCUSDT |

</div>

<div class="api-br-10"></div>


<div class="api-aligning">

```json title=Response Example
{
  "code": "00000",
  "msg": "success",
  "requestTime": 1743059269376,
  "data": [
    {
      "symbol": "BTCUSDT",
      "fundingRate": "0.000071",
      "fundingRateInterval": "8",
      "nextUpdate": "1743062400000",
      "minFundingRate": "-0.003",
      "maxFundingRate": "0.003"
    }
  ]
}
```

### Response Parameters

| Parameter               | Type   | Description                                                                                                                                 |
|:------------------------|:-------|:--------------------------------------------------------------------------------------------------------------------------------------------|
| &gt;symbol              | String | Trading pair name                                                                                                                           |
| &gt;fundingRate         | String | Current funding rates                                                                                                                       |
| &gt;fundingRateInterval | String | Funding rate settlement period<br/>Unit: hour. Enumeration values include 1, 2, 4, 8. 1 represents 1 hour, 2 represents 2 hours, and so on. |
| &gt;nextUpdate          | String | Next update time<br/>Unix timestamp in milliseconds                                                                                         |
| &gt;minFundingRate      | String | Lower limit of funding rate <br/>Returned in decimal form. 0.025 represents 2.5%.                                                           |
| &gt;maxFundingRate      | String | Upper limit of funding rate <br/>Returned in decimal form. 0.025 represents 2.5%.                                                           |

</div>
