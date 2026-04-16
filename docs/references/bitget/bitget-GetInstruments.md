---
sidebar_position: 5
hide_table_of_contents: true
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

# Get Instruments

### Description

Query the specifications for online trading pair instruments.

### HTTP Request

- GET /api/v3/market/instruments
- Rate limit: 20/sec/IP

<div class="api-aligning">

```bash title=Request Example
curl "https://api.bitget.com/api/v3/market/instruments?category=USDT-FUTURES"  \

```

### Request Parameters

| Parameters | Type   | Required | Description                                                                                                                                                                  |
|------------|--------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| category   | String | Yes      | Product type <br/> `SPOT` Spot trading <br/> `MARGIN` Margin trading <br/> `USDT-FUTURES` USDT futures <br/> `COIN-FUTURES` Coin-M futures <br/> `USDC-FUTURES` USDC futures |
| symbol     | String | No       | Symbol name <br/>e.g.,`BTCUSDT`                                                                                                                                              |

</div>

<div class="api-br-10"></div>





<Tabs>
<TabItem value="Spot" label="Spot" default>

```json title=Response Example
{
  "code": "00000",
  "msg": "success",
  "requestTime": 1770531248742,
  "data": [
    {
      "symbol": "BTCUSDT",
      "category": "SPOT",
      "baseCoin": "BTC",
      "quoteCoin": "USDT",
      "buyLimitPriceRatio": "0.02",
      "sellLimitPriceRatio": "0.02",
      "minOrderQty": "0.000001",
      "maxOrderQty": "0",
      "pricePrecision": "2",
      "quantityPrecision": "6",
      "quotePrecision": "8",
      "minOrderAmount": "1",
      "maxSymbolOrderNum": "",
      "maxProductOrderNum": "400",
      "status": "online",
      "maintainTime": "",
      "maxPositionNum": "200",
      "symbolType": "crypto",
      "launchTime": "1532454360000"
    }
  ]
}
```

</TabItem>



<TabItem value="Futures" label="Futures" default>

```json title=Response Example
{
  "code": "00000",
  "msg": "success",
  "requestTime": 1770531054230,
  "data": [
    {
      "symbol": "BTCUSDT",
      "category": "USDT-FUTURES",
      "baseCoin": "BTC",
      "quoteCoin": "USDT",
      "isRwa": "NO",
      "buyLimitPriceRatio": "0.05",
      "sellLimitPriceRatio": "0.05",
      "feeRateUpRatio": "0.005",
      "makerFeeRate": "0.0002",
      "takerFeeRate": "0.0006",
      "openCostUpRatio": "0.01",
      "minOrderQty": "0.0001",
      "maxOrderQty": "1200",
      "pricePrecision": "1",
      "quantityPrecision": "4",
      "quotePrecision": "",
      "priceMultiplier": "0.1",
      "quantityMultiplier": "0.0001",
      "type": "perpetual",
      "minOrderAmount": "5",
      "maxSymbolOrderNum": "",
      "maxProductOrderNum": "400",
      "maxPositionNum": "200",
      "status": "online",
      "offTime": "-1",
      "limitOpenTime": "-1",
      "deliveryTime": "",
      "deliveryStartTime": "",
      "deliveryPeriod": "",
      "launchTime": "0",
      "fundInterval": "8",
      "minLeverage": "1",
      "maxLeverage": "150",
      "maintainTime": "",
      "symbolType": "crypto",
      "maxMarketOrderQty": "220"
    }
  ]
}
```

</TabItem>


<TabItem value="Margin" label="Margin" default>

```json title=Response Example
{
  "code": "00000",
  "msg": "success",
  "requestTime": 1770531089306,
  "data": [
    {
      "symbol": "ADAUSDT",
      "category": "MARGIN",
      "baseCoin": "ADA",
      "quoteCoin": "USDT",
      "buyLimitPriceRatio": "0.02",
      "sellLimitPriceRatio": "0.02",
      "minOrderQty": "0.001",
      "maxOrderQty": "0",
      "pricePrecision": "4",
      "quantityPrecision": "3",
      "quotePrecision": "7",
      "minOrderAmount": "1",
      "maxSymbolOrderNum": "",
      "maxProductOrderNum": "400",
      "maxPositionNum": "200",
      "status": "online",
      "maintainTime": "",
      "isIsolatedBaseBorrowable": "YES",
      "isIsolatedQuotedBorrowable": "YES",
      "warningRiskRatio": "0.8",
      "liquidationRiskRatio": "1",
      "maxCrossedLeverage": "5",
      "maxIsolatedLeverage": "10",
      "userMinBorrow": "0.00000001",
      "areaSymbol": "no",
      "maxLeverage": "10",
      "symbolType": "crypto",
      "launchTime": null
    }
  ]
}
```

</TabItem>

</Tabs>

<div class="api-aligning">

### Response Parameters

| Parameters                 | Type   | Description                                                                                                                                                                                                                                                                                                                                         |
|----------------------------|--------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| category                   | String | Product type <br/> `SPOT` Spot trading <br/> `MARGIN` Margin trading <br/> `USDT-FUTURES`: USDT futures <br/> `COIN-FUTURES`: Coin-M futures <br/> `USDC-FUTURES`: USDC futures                                                                                                                                                                     |
| symbol                     | String | Symbol name                                                                                                                                                                                                                                                                                                                                         |
| isRwa                      | String | Is this an RWA Symbol<br/> `YES` <br/> `NO`                                                                                                                                                                                                                                                                                                         |
| baseCoin                   | String | Base coin <br/>e.g.,`BTC`in`BTCUSDT`                                                                                                                                                                                                                                                                                                                |
| quoteCoin                  | String | Quote coin <br/>e.g.,`USDT`in`BTCUSDT`                                                                                                                                                                                                                                                                                                              |
| buyLimitPriceRatio         | String | Buy price limit ratio <br/> The ratio of the buy limit price to the market price, determining the maximum price at which a buy order will be placed                                                                                                                                                                                                 |
| sellLimitPriceRatio        | String | Sell price limit ratio <br/> The ratio of the sell limit price to the market price, determining the minimum price at which a sell order will be placed                                                                                                                                                                                              |
| feeRateUpRatio             | String | Fee markup ratio <br/>The percentage by which the actual fee is increased relative to the base fee                                                                                                                                                                                                                                                  |
| openCostUpRatio            | String | Opening cost markup ratio <br/> The percentage by which the cost of opening a trading position is increased relative to the base or standard cost                                                                                                                                                                                                   |
| minOrderQty                | String | Minimum order quantity <br/>This refers to the smallest allowable quantity for placing an order in terms of the base coin <br/> Only applicable to futures trading, for spot/margin, please refer to <a href="https://www.bitget.com/zh-CN/trade-info/trade-rule">Trading Rules</a>.                                                                |
| maxOrderQty                | String | Maximum order quantity for a single limit order <br/>This refers to the largest allowable quantity for placing an order in terms of the base coin  <br/> Only applicable to futures trading, for spot/margin, please refer to <a href="https://www.bitget.com/zh-CN/trade-info/trade-rule">Trading Rules</a>. <br/>A value of 0 indicates no limit. |
| minOrderAmount             | String | Minimum order amount <br/> This refers to the smallest allowable amount for placing an order in terms of the quote coin                                                                                                                                                                                                                             |
| pricePrecision             | String | Price precision <br/> The number of decimal places allowed for the price                                                                                                                                                                                                                                                                            |
| quantityPrecision          | String | Quantity precision <br/> The number of decimal places allowed for the quantity                                                                                                                                                                                                                                                                      |
| quotePrecision             | String | Market order precision <br/> The number of decimal places allowed for the price of the quote coin                                                                                                                                                                                                                                                   |
| priceMultiplier            | String | Price multiplier <br/> Used for futures orders, along with `pricePrecision` <br/> Example: `pricePrecision`: 2 & `priceMultiplier`: 0.02<br/>The order price must be a multiple of `priceMultiplier` and have two decimal places (e.g.,0.08, 1.14, 2.36)                                                                                            |
| quantityMultiplier         | String | Quantity multiplier <br/> Used for futures orders, along with `quantityPrecision` <br/> Example: `quantityPrecision`: 2 & `quantityMultiplier`: 0.02<br/>The order quantity must be a multiple of `quantityMultiplier` and have two decimal places  (e.g.,0.08, 1.14, 2.36)                                                                         |
| type                       | String | Futures type <br/> `perpetual` Perpetual <br/> `delivery` Delivery                                                                                                                                                                                                                                                                                  |
| maxSymbolOrderNum          | String | Maximum order number in terms of the trading pair                                                                                                                                                                                                                                                                                                   |
| maxProductOrderNum         | String | Maximum order number in terms of the product line                                                                                                                                                                                                                                                                                                   |
| maxPositionNum             | String | Maximum position number in terms of the trading pair                                                                                                                                                                                                                                                                                                |
| status                     | String | Trading pair status <br/> `listed` Listed (not yet open) <br/> `online` Normal <br/> `limit_open` Restrict opening positions  <br/> `limit_close` Restrict closing positions <br/> `offline` Delisted/under maintenance <br/> `restrictedAPI` API restricted                                                                                        |
| offTime                    | String | Trading halt time. <br/> If not configured, it returns: ""                                                                                                                                                                                                                                                                                          |
| limitOpenTime              | String | Restricted open time <br/> If not configured, it returns: ""; Other values indicate symbol is under/expected maintenance and trading is prohibited after a specified time                                                                                                                                                                           |
| deliveryTime               | String | Delivery time <br/> Available only for deliveries                                                                                                                                                                                                                                                                                                   |
| deliveryStartTime          | String | Delivery start time <br/> Available only for deliveries                                                                                                                                                                                                                                                                                             |
| deliveryPeriod             | String | Delivery period <br/> `this_quarter` This quarter <br/> `next_quarter` Next quarter <br/> Available only for deliveries                                                                                                                                                                                                                             |
| launchTime                 | String | Launch time <br/> Unix millisecond timestamp indicating when the trading pair was launched                                                                                                                                                                                                                                                          |
| fundInterval               | String | Funding Interval <br/> `1` Every 1 hour <br/>`8` Every 8 hours                                                                                                                                                                                                                                                                                      |
| minLeverage                | String | Minimum leverage                                                                                                                                                                                                                                                                                                                                    |
| maxLeverage                | String | Maximum leverage                                                                                                                                                                                                                                                                                                                                    |
| maintainTime               | String | Maintenance time <br/> If not configured, it returns: ""                                                                                                                                                                                                                                                                                            |
| isIsolatedBaseBorrowable   | String | Base coin borrowable status <br/> Available only for margin trading                                                                                                                                                                                                                                                                                 |
| isIsolatedQuotedBorrowable | String | Quote coin borrowable status <br/> Available only for margin trading                                                                                                                                                                                                                                                                                |
| warningRiskRatio           | String | Warning risk ratio                                                                                                                                                                                                                                                                                                                                  |
| liquidationRiskRatio       | String | Liquidation risk ratio                                                                                                                                                                                                                                                                                                                              |
| maxCrossedLeverage         | String | Maximum leverage for cross margin <br/> Available only for margin trading                                                                                                                                                                                                                                                                           |
| maxIsolatedLeverage        | String | Maximum leverage for isolated margin <br/> Available only for margin trading                                                                                                                                                                                                                                                                        |
| userMinBorrow              | String | Minimum borrowable amount <br/> Available only for margin trading                                                                                                                                                                                                                                                                                   |
| areaSymbol                 | String | Area symbol<br/> `YES`/`NO` <br/> Available only for Spot trading <br/> Only return this parameter for pairs where the value is `YES`.                                                                                                                                                                                                              |
| makerFeeRate               | String | Maker fee rate<br/>In decimal form, e.g., 0.0002 represents 0.02%                                                                                                                                                                                                                                                                                   |
| takerFeeRate               | String | Taker fee rate<br/>In decimal form, e.g., 0.0002 represents 0.02%                                                                                                                                                                                                                                                                                   |
| maxMarketOrderQty          | String | Maximum order quantity for a single market order <br/>This refers to the largest allowable quantity for placing an order in terms of the base coin                                                                                                                                                                                                  |
| symbolType                 | String | Symbol Types <br/> `crypto` cryptocurrency <br/>`metal` precious metals <br/>`stock` stocks <br/>`commodity` commodities                                                                                                                                                                                                                            |

</div>
