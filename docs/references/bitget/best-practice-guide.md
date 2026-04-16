---
sidebar_position: 6
hide_table_of_contents: false
---

# Best Practices Guide

## Product Configuration

Users can obtain the exchange's product configuration through `GET /api/v3/public/instruments`.

## Market Data

Users can receive real-time market data updates from WebSocket channels.

**Depth Data Channels**:<br/>
`books`, `books5`, `books15` default push frequency for spot: `200ms`, for futures: `150ms`<br/>
`books1` default push frequency: `1ms`<br/>

`books` corresponds to full depth data, first push is full data: `snapshot`, subsequent pushes are incremental changes:
`update`.<br/>
`books1` corresponds to 1-level depth data, each push: `snapshot`.<br/>
`books5` corresponds to 5-level depth data, each push: `snapshot`.<br/>
`books50` corresponds to 50-level depth data, each push: `snapshot`.<br/>

When there are no changes to the order book, the system will not send new snapshots.

Order book data is triggered and pushed by order events. In most cases, users receive the same order book data from all
WebSocket connections and channels. When there are no order book changes for an extended period, the system triggers
compensatory pushes through scheduled tasks, which may result in slightly different push sequence numbers due to clock
differences between servers.

The system pushes the latest state of the order book. When depth changes occur (including multiple changes in a short
time, such as A→B→A), the system sends updates for the final state.

## Account and Sub-account Configuration

After creating API keys, users can configure their accounts through the API or web interface before trading.

### Account Configuration

Users can view current account configuration through the following REST API:

`GET /api/v3/account/settings`

The API returns account mode, position mode, asset mode, and many other account-related information.

### Account Mode

The Unified Trading Account system provides three account modes: Spot Mode (coming soon), Basic Mode, and Advanced Mode.

| Account Mode  | Tradable Products                                                               | Margin Support                                                                                                                            | Eligibility Requirements                                                                                                            |
|:--------------|:--------------------------------------------------------------------------------|:------------------------------------------------------------------------------------------------------------------------------------------|:------------------------------------------------------------------------------------------------------------------------------------|
| Spot Mode     | Only supports spot trading, does not support margin trading and futures trading | Does not use margin                                                                                                                       | Users under regulated entities: This account mode applies by default                                                                |
| Basic Mode    | Spot, USDT Futures, USDC Futures                                                | In Basic Mode, USDT and USDC denominated pairs share the same margin, and profits and losses between these products can offset each other | Users registered under global entities: This account mode applies by default. Need to complete questionnaire to activate this mode. |
| Advanced Mode | Spot, Margin, USDT Futures, USDC Futures, Coin-margined                         | All assets support mutual margin                                                                                                          | Account equity ≥ 1,000 USD: Complete questionnaire to activate this mode                                                            |

In **Advanced Mode**, assets of all product types can be used as shared margin, and profits and losses can offset each
other.

Account mode can be changed via API or on the web.

### Position Mode

The exchange currently supports two position modes.

| Position Mode | Description                                                                                                                        |
|:--------------|:-----------------------------------------------------------------------------------------------------------------------------------|
| One-way Mode  | Can only hold long or short positions. The exchange automatically opens/closes positions based on your specified position quantity |
| Hedge Mode    | Can hold both long and short positions simultaneously                                                                              |

Users can set the position mode through the following REST API (all positions must be closed and no pending orders
before setting):

`POST /api/v3/account/set-hold-mode`

### Cross/Isolated Margin Mode

The Unified Trading Account system currently only supports cross margin mode.

### Get Leverage

Users can get leverage through the following REST API:

`GET /api/v3/account/settings`

Currently, there is no global leverage setting; it needs to be set separately by product trading pair.

**Product Types**:

| Position Mode | Product Type   | Margin Mode | Level        |
|:--------------|:---------------|:------------|:-------------|
| One-way       | USDT Futures   | Cross       | Trading Pair |
| One-way       | USDC Futures   | Cross       | Trading Pair |
| One-way       | Coin Perpetual | Cross       | Trading Pair |
| Hedge         | USDT Futures   | Cross       | Trading Pair |
| Hedge         | USDT Perpetual | Cross       | Trading Pair |
| Hedge         | USDC Futures   | Cross       | Trading Pair |

### Set Leverage

After obtaining the leverage, users can set it as needed:

`POST /api/v3/account/set-leverage`

Users can use the above two API interfaces to preset the leverage for each trading pair before trading.

#### Example

Suppose we have the following settings and requirements:

- Account Mode: Advanced Mode
- Position Mode: One-way Mode
- Products requiring 3x leverage:
    - BTCUSDT
- The above products only use cross margin mode

The setting level for spot margin is by currency, users can extract the currency to set individually, i.e., BTC, USDT.

Request body example for setting BTC currency leverage to 3x (applicable for selling BTCUSDT):

```json
{
  "leverage": "3.0",
  "coin": "BTC",
  "productType": "MARGIN"
}
```

The request body for setting USDT is similar.

The next step is to set the leverage for BTCUSDT perpetual futures.

Since it's USDT futures, users need to set separately:

```json
{
  "leverage": "3",
  "symbol": "BTCUSDT",
  "productType": "usdt-futures"
}
```

After sending the above API REST requests, the leverage settings for these products are complete.

## Order Management

### Subscribe to Order Channel

Before placing orders, users should first subscribe to the order channel using WebSocket to monitor order status (such
as pending, filled) and take appropriate actions (such as placing new orders after complete fill).

The order channel provides multiple subscription dimensions. To subscribe to the above BTCUSDT order data, users can
send any of the following requests after connecting to and logging into the private WebSocket:

| Subscription Dimension | Product Type                                                           |
|:-----------------------|:-----------------------------------------------------------------------|
| **Request**            | `{"op": "subscribe", "args": [{"instType": "UTA", "topic": "order"}]}` |
| **Success Response**   | `{"event": "subscribe", "arg": {"topic": "order", "instType": "UTA"}}` |

**Note**: The order channel does not push full data on initial subscription, only pushes order updates when order status
changes (such as from pending to canceled).

In other words, users cannot know the current order data when subscribing to the order channel. To get data for unfilled
orders before subscribing to the order channel, use the following REST API:

`GET /api/v3/trade/unfilled-orders`

### Place Order

To make it easier for the system to identify orders, we recommend users fill in the client order ID (`clientOid` field)
when placing orders. The client order ID must match `^[0-9A-Za-z_:#\\-+\\s]{1,32}$`.

`clientOid` uniqueness check only applies to all pending orders, but we still recommend users always use unique
`clientOid` for troubleshooting purposes.

In this example, we will fill in `testBTC0123` in the `clientOid` field.

After subscribing to the order channel, users can prepare to place the BTCUSDT order.

Users can place orders through REST and WebSocket.

#### REST API

Users can place orders through the following REST API. After the server receives the request, it will return the order
ID (`orderId`).

<div className="table-wrap-text">

| REST API             | `POST /api/v3/trade/place-order`                                                                                                                            |
|:---------------------|:------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Request Body**     | `{"category":"SPOT","symbol":"BGBUSDT","orderType":"limit","qty":"123","price":"1.11","side":"buy","posSide":"long","timeInForce":"gtc","reduceOnly":"no"}` |
| **Success Response** | `{"code": "00000", "msg": "success", "requestTime": 1695806875837, "data": {"clientOid": "testBTC0123", "orderId": "1234567890"}}`                          |

</div>

**Note**: This only means the exchange has successfully received the request and assigned an order ID to the order. At
this point, the order may not have reached the matching system yet, and users need to further check the order status for
confirmation.

#### WebSocket

Users can also place orders through WebSocket, which is theoretically more efficient and resource-saving than REST.

Since WebSocket operations are asynchronous communication, users need to provide a message ID (`id`) for identification
of its return.

After logging into the private WebSocket, send the following WebSocket message:

```json
{
  "op": "trade",
  "id": "testBTC0123",
  "category": "spot",
  "topic": "place-order",
  "args": [
    {
      "orderType": "limit",
      "price": "100",
      "qty": "0.1",
      "side": "buy",
      "symbol": "BTCUSDT",
      "timeInForce": "gtc",
      "clientOid": "testBTC0123"
    }
  ]
}
```

After the server receives the request, it will return the result along with the message ID (i.e., `testBTC0123`) and the
order ID (`orderId`) assigned by the exchange:

```json
{
  "event": "trade",
  "id": "testBTC0123",
  "category": "spot",
  "topic": "place-order",
  "args": [
    {
      "symbol": "BTCUSDT",
      "orderId": "1234567890",
      "clientOid": "testBTC0123",
      "cTime": "1750034397008"
    }
  ],
  "code": "0",
  "msg": "success",
  "ts": "1750034397076"
}
```

**Note**: This only means the exchange has successfully received the request and assigned an order ID to the order. At
this point, the order may not have reached the matching system yet, and users need to further check the order status for
confirmation.

### Check Order Status

After placing an order, if the order returns no errors (`"code": "0"`), after the order enters matching, users will
receive a message in the WebSocket order channel indicating the order status is `new`.

After the order is completely filled, users will receive a push message indicating the order status has changed to
`filled`, along with other fields related to the fill.

If the order is partially or fully filled, WebSocket will return `state = partially_filled` and `filled` respectively.

For Immediate-Or-Cancel (IOC), Fill-Or-Kill (FOK), and post-only orders, these orders may be rejected by the matching
engine, and users will receive `live` followed by `canceled` status.

User orders may be canceled by the system for various reasons, such as liquidation or self-trade. Users can refer to
`cancelSource` to determine the reason for order cancellation.

The terminal status of an order is `canceled` or `filled`.

Each fill of an order is assigned a trade ID (`tradeId`) by the system.

**Possible Order States**:

| Scenario                                                                             | Status Change                                                                |
|:-------------------------------------------------------------------------------------|:-----------------------------------------------------------------------------|
| Rejected at entry                                                                    | `code` is not zero, no update push in WebSocket order channel                |
| Order placed and immediately fully filled                                            | `live` → `new` → `filled`                                                    |
| Order placed and immediately filled through multiple trades                          | `live` → `new` → `partially_filled` → ... → `filled`                         |
| Order placed but immediately canceled by matching engine (e.g., IOC, FOK, post-only) | `live` → `canceled` (cancellation reason can be queried from `cancelSource`) |
| IOC order partially filled then canceled by system due to insufficient price depth   | `live` → `partially_filled` → `canceled`                                     |

### Modify Order

The modify order interface supports order modification for all product types, allowing users to modify the order's
price (`price` field) and/or quantity (`qty` field). Additionally, the API provides an `autoCancel` parameter to set
automatic order cancellation when modification fails.

**REST**:

`POST /api/v3/trade/modify-order`

**WebSocket Business Operation Request Parameters**:

`{
    "args": [
        {
            "autoCancel": "yes",
            "clientOid": "135423791666666666",
            "orderId": "1354237910666666666",
            "price": "5",
            "qty": "2",
            "symbol":"BTCUSDT"
        }
    ],
    "id": "ae5ea6df-215f-4750-a700-d487d03ac020",
    "op": "trade",
    "category": "usdt-futures",
    "topic": "modify-order"
}`

Similar to placing orders, users should receive a successful REST/WebSocket response from the server, then receive order
information push updates in the WebSocket order channel.

**Note**: Orders cannot be modified when completely filled or successfully canceled.

A successful response only indicates the exchange has received the request. Users should refer to the WebSocket order
channel for confirmation.

### Cancel Order

Users can cancel orders in a similar way through REST or WebSocket.

**REST**:

`POST /api/v3/trade/cancel-order`

**WebSocket Business Operation Request Parameters**:

`{
    "args": [
        {
            "orderId": "xxxxxxxxxxxxxxxxxx",
            "clientOid": "xxxxxxxxxxxxxxxxxx"
        }
    ],
    "id": "c8a1999c-1f82-409d-870e-f40ff49c4072",
    "op": "trade",
    "topic": "cancel-order"
}"`

Similarly, users should receive a successful REST/WebSocket response from the server. When users receive a push update
from the WebSocket order channel indicating the order status is `canceled`, it means the order cancellation was
successful.

**Note**: Orders cannot be canceled when completely filled or successfully canceled.

A successful response only indicates the exchange has received the request. Users should refer to the WebSocket order
channel for confirmation.

### Batch Operations

Placing, modifying, and canceling orders all support batch operations, with a maximum of 20 orders per batch.

**REST**:

| Operation    | API                                     |
|:-------------|:----------------------------------------|
| Place Order  | `POST /api/v3/trade/place-batch`        |
| Modify Order | `POST /api/v3/trade/batch-modify-order` |
| Cancel Order | `POST /api/v3/trade/cancel-batch`       |

**WebSocket Business Operation Request Parameters**:

| Operation    | Parameter                 |
|:-------------|:--------------------------|
| Place Order  | `"topic": "batch-place"`  |
| Modify Order | `"topic": "batch-modify"` |
| Cancel Order | `"topic": "batch-cancel"` |

Batch operations allow partial order operations to succeed. After receiving the response, users should check the `code`
and `msg` fields for each order in the return result to determine the execution result.

### Order Timestamps

Order data contains multiple timestamps for users to track order status and latency.

| Field         | Description                                                                                                            |
|:--------------|:-----------------------------------------------------------------------------------------------------------------------|
| `createdTime` | Order creation time in the order management system after risk check                                                    |
| `updatedTime` | Last update time of the order in the order management system. Updated after order modification, fill, and cancellation |
| `ts`          | WebSocket gateway push time                                                                                            |

## Pagination

Bitget provides pagination functionality to help users easily obtain the data they want from massive amounts of data.
Related request parameters are as follows:

| Parameter   | Type   | Required | Description                                                                                                                                                                                |
|:------------|:-------|:---------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `cursor`    | String | No       | Used for pagination, not passed for first query, when querying second page and subsequent data, take the minimum Id from the previous query, results will return data less than this value |
| `startTime` | String | No       | Start time, Unix timestamp (milliseconds)                                                                                                                                                  |
| `endTime`   | String | No       | End time, Unix timestamp (milliseconds)                                                                                                                                                    |
| `limit`     | String | No       | Number of results returned, maximum 100, default 100                                                                                                                                       |

Trading interfaces with pagination functionality are listed below:

- `GET /api/v3/trade/unfilled-orders` - Get current orders
- `GET /api/v3/trade/history-orders` - Get historical orders
- `GET /api/v3/trade/fills` - Get fill details
- `GET /api/v3/account/financial-records` - Get financial records

## Self-Trade Prevention

The trading system implements mandatory self-trade prevention at the parent account level. All accounts under the same
parent account, including the parent account itself and all sub-accounts, cannot self-trade. The default STP mode for
orders is Cancel Maker, and users can also specify the order's STP mode through the `stpMode` parameter in the order
placement interface.

Bitget supports 4 STP modes (`stpMode`): `none`, `cancel_maker`, `cancel_taker`, and `cancel_both`.

**Note**: The mandatory self-trade prevention feature applies to all users, all order types, and all order book trading
products.

### Self-Trade Prevention Modes

Bitget provides users with four modes that define how to prevent self-trading. The execution result of STP depends on
the STP mode of the Taker order; the STP mode of existing orders in the order book is not considered.

#### none Mode

Orders are not restricted by the STP mechanism, the system does not compare UIDs, and trading proceeds normally.

#### cancel_taker Mode

Cancel the Taker order, keep the Maker order.

#### cancel_maker Mode

Cancel the Maker order, keep the Taker order.

#### cancel_both Mode

Cancel both Taker and Maker orders.

## Unified Account and Position

### Account

#### WebSocket Subscription

We recommend using WebSocket to subscribe to the account channel to receive account updates.

This endpoint returns the user's asset value in USD and other parameters that are continuously updated due to price
changes. Bitget sends update data to users when valuations change.

Request and response examples after connecting to private WebSocket and logging in:

| Subscription Dimension | Account                                                                     |
|:-----------------------|:----------------------------------------------------------------------------|
| **Request**            | `{"op": "subscribe","args": [ { "instType": "UTA","topic": "account" } ] }` |
| **Success Response**   | `{"event": "subscribe","arg": {"instType": "UTA","topic": "account"} }`     |

#### Initial Subscription Full Data

Unlike the order channel, the account channel pushes full data on initial subscription.

#### Subsequent Pushes

Subsequently, users will receive account data pushes based on the following situations:

| Push Type            | Description                                                                                                              |
|:---------------------|:-------------------------------------------------------------------------------------------------------------------------|
| Event-triggered Push | Unified account spot/margin/futures order fill, fund settlement, when balance changes (transfer, airdrop, lending, etc.) |

#### REST API

Users can also view account balance through REST API:

`GET /api/v3/account/assets`
`GET /api/v3/account/funding-assets`

### Maximum Available Quantity

In Advanced Mode, enabling auto-borrow allows users to buy/sell products with quantities exceeding the currency balance.

In this case, users would want to know the maximum buy/sell quantity for that product. Users can poll the following REST
API to get the maximum available quantity (including available balance and the exchange's maximum borrowable):

`POST /api/v3/account/max-open-available`

Request and response example:

| Request              | `{"category":"SPOT","symbol":"BTCUSDT","orderType":"market","side":"sell"}`                                                                                                                        |
|:---------------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Success Response** | `{"code": "00000", "requestTime": 1741851607871, "data": {"available": "52.008255", "maxOpen": "", "buyOpenCost": "", "sellOpenCost": "", "maxBuyOpen": "", "maxSellOpen": ""}, "msg": "success"}` |

For spot/margin `available`, when side=buy it represents quote currency quantity, when side=sell it represents base
currency quantity.

### Maximum Transferable

To get the maximum transferable amount of a unified account or a sub-account, users can get the maximum transferable
through `GET /api/v3/account/max-transferable`.

This endpoint supports getting maximum transferable with borrowing.

### Position

Users should use WebSocket to get position information updates.

#### WebSocket Subscription

Similar to the order channel, the position channel provides multiple subscription dimensions.

This endpoint returns mark price and other continuously changing parameters. Bitget periodically pushes data updates to
users.

To subscribe to BTCUSDT position data, users can send any of the following requests after connecting to and logging into
the private WebSocket:

| Subscription Dimension | Product Type                                                             |
|:-----------------------|:-------------------------------------------------------------------------|
| **Request**            | `{"op": "subscribe","args": [{"instType": "UTA","topic": "position"}]}`  |
| **Success Response**   | `{"event": "subscribe","arg": {"instType": "UTA","topic": "position"} }` |

#### Initial Subscription Full Data

Like the account channel, the position channel pushes full data on initial subscription, pushing all position
information where positions are not 0.

#### Subsequent Pushes

Subsequently, users will receive position data pushes based on the following situations:

| Push Type            | Description                                                                                                                                                                                                                                                                       |
|:---------------------|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Event-triggered Push | Unified account futures close position order placement, unified account futures open position order fill, unified account futures close position order fill, unified account futures close position order modification, unified account futures close position order cancellation |

#### REST API

Users can also view position information through REST API:

`GET /api/v3/position/current-position`
