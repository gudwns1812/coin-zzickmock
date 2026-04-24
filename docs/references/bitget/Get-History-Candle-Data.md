# Get Historical Candlestick Data

Retrieves historical K-line data for Bitget Contract (Futures).

## Endpoint Information

*   **HTTP Request:** `GET /api/v2/mix/market/history-candles`
*   **Rate Limit:** 20 times per 1 second (per IP)
*   **Max Data Points:** 200 pieces per request.

## Request Parameters

| Parameter | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `symbol` | String | Yes | Trading pair (e.g., `BTCUSDT`) |
| `productType` | String | Yes | `usdt-futures`, `coin-futures`, or `usdc-futures` |
| `granularity` | String | Yes | Time interval (see list below) |
| `startTime` | String | No | Start time in milliseconds |
| `endTime` | String | No | End time in milliseconds |
| `limit` | String | No | Default 100, maximum 200 |

### Granularity Values

*   **Minutes:** `1m`, `3m`, `5m`, `15m`, `30m`
*   **Hours:** `1H`, `4H`, `6H`, `12H`
*   **Days/Weeks:** `1D`, `3D`, `1W`, `1M`
*   **UTC Variants:** `6Hutc`, `12Hutc`, `1Dutc`, `3Dutc`, `1Wutc`, `1Mutc`

## Response Parameters

The response returns an array of arrays, where each inner array represents a candle:

| Index | Field | Description |
| :--- | :--- | :--- |
| 0 | Timestamp | Milliseconds (ms) |
| 1 | Open | Open price |
| 2 | High | High price |
| 3 | Low | Low price |
| 4 | Close | Close price |
| 5 | Volume | Base currency volume |
| 6 | Quote Volume | Quote currency volume |

## Example Response (JSON)

```json
[
  [
    "1627008780000",
    "24016.00",
    "24020.00",
    "24015.00",
    "24016.00",
    "12.345",
    "296477.52"
  ]
]
```

## Example Request (cURL)

```bash
curl "https://api.bitget.com/api/v2/mix/market/history-candles?symbol=BTCUSDT&granularity=1H&limit=200&productType=usdt-futures"
```

## Important Notes

1.  **Rounding:** Timestamps for `startTime` and `endTime` should be rounded down to the nearest unit of the selected granularity.
2.  **Historical Limits:** This endpoint allows querying data older than 90 days, unlike the standard `candles` endpoint.
3.  **V2 Standard:** Use the `/api/v2/mix/...` endpoint as it is the current recommended standard by Bitget.
