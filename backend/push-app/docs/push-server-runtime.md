# Push Server Runtime

## Purpose

`backend/push-app` is the executable push server for Redis Stream-backed SSE delivery.
It exists so market/trading HTTP and write workloads in `app` do not also own long-lived SSE fan-out pressure.

## Ownership

`push-app` owns:

- `PushServerApplication`.
- Redis Stream consumer group initialization and polling.
- SSE connection registry and fan-out for push event envelopes.
- Canonical `/api/futures/stream/**` routes plus legacy `/api/futures/**/stream` aliases.
- Push relay metrics for publish age, ack result, delivery send result, and dropped events.

`push-app` does not own:

- Business writes, DB transactions, Flyway, JPA, QueryDSL, or storage repositories.
- Bitget/external provider connector runtime.
- Product/domain source-of-truth rules.
- Direct references to `backend/app` implementation classes.

## Event Contract

`app` publishes `core` `PushEventEnvelope` records to Redis Streams.
`push-app` consumes those records and converts the envelope payload JSON into SSE data without calling `app`, storage, or external provider code.
Market payloads may reuse `stream` response DTO shape for wire compatibility.

## Route Contract

Canonical routes are:

- `/api/futures/stream/orders`
- `/api/futures/stream/markets`
- `/api/futures/stream/markets/summary`
- `/api/futures/stream/markets/{symbol}`
- `/api/futures/stream/markets/{symbol}/candles`

Existing `/api/futures/orders/stream` and `/api/futures/markets/**/stream` routes remain aliases until clients are fully migrated.
Nginx must route `/api/futures/stream/**` to `push-app`; non-stream `/api/futures/**` remains routed to `app`.

## Verification

```bash
cd backend
./gradlew :push-app:test :push-app:bootJar architectureLint --console=plain
```

Runtime compose changes should also be checked with Docker Compose config preflight for the affected local or production compose file.
