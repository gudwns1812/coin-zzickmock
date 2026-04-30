# Bitget WebSocket Latest Price Source

## 목적 / 큰 그림

현재 최신가를 REST polling으로 읽는 경로를 단계적으로 WebSocket 기반 경로로 바꾼다.
이 PR은 첫 번째 PR로, 런타임 동작을 바꾸지 않고 이후 PR들이 공유할 source/health/freshness 계약과
감사 기준을 고정한다.

## 진행 현황

- [x] PR 1: realtime source contract와 latest-price audit 기준 추가
- [ ] PR 2: Bitget WebSocket provider parser/connection lifecycle
- [ ] PR 3: realtime market data store와 REST bootstrap/recovery labeling
- [ ] PR 4: market summary read/SSE의 WS source 전환
- [ ] PR 5: command/read service의 realtime source 전환
- [ ] PR 6: trade/ticker semantics 기준 execution processor 분리
- [ ] PR 7: live candle aggregation/persistence/SSE
- [ ] PR 8: frontend consumer와 제품/provider 문서 업데이트

## 의사결정 기록

- 정상 최신가는 WebSocket source여야 한다.
- `TRADE`, `TICKER`, `CANDLE` source는 서로 다른 의미를 가진다.
- `REST_BOOTSTRAP`과 `REST_RECOVERY`는 명시적 fallback label이다.
- trading command는 fresh WebSocket 값을 기본으로 요구한다.
- `REST_RECOVERY`는 테스트와 source label이 있을 때만 제한적으로 허용한다.
- `REST_BOOTSTRAP`은 command 실행 source로 쓰지 않는다.

## Realtime Source Contract

PR 1에서 추가한 계약:

- `MarketRealtimeSourceType`
  - `TRADE`: public trade/execution stream
  - `TICKER`: ticker/mark/index/funding stream
  - `CANDLE`: direct or aggregated candle stream
  - `REST_BOOTSTRAP`: startup seed only
  - `REST_RECOVERY`: reconnect/gap repair only
- `MarketRealtimeHealth`
  - `HEALTHY`, `BOOTSTRAPPING`, `RECOVERING`, `STALE`, `UNAVAILABLE`
- `MarketRealtimeReconnectState`
  - `NOT_STARTED`, `CONNECTING`, `CONNECTED`, `RECONNECTING`, `DISCONNECTED`
- `MarketRealtimeSourceSnapshot`
  - symbol, source type, health, source event time, received time, reconnect state, fallback reason, trade id, candle open time
  - `ageMs(now)` and `isFresh(now, maxAge)` freshness API
- `MarketRealtimeFreshnessPolicy`
  - command/read code can explicitly decide whether `REST_RECOVERY` is allowed
  - `REST_BOOTSTRAP` is never accepted by this policy for command use

## Latest Price Audit

The following call sites currently depend on REST polling or REST-shaped latest-price snapshots and must be migrated or
explicitly classified in later PRs.

### Provider boundary

- `providers/connector/MarketDataGateway.java`
  - `loadSupportedMarkets()`
  - `loadMarket(String symbol)`
- `providers/infrastructure/BitgetMarketDataGateway.java`
  - `loadSupportedMarkets()` delegates to `loadMarket("BTCUSDT")` and `loadMarket("ETHUSDT")`
  - `loadMarket(String symbol)` reads Bitget REST ticker data

### Scheduled market refresh and market SSE

- `feature/market/application/realtime/MarketRealtimeFeed.java`
  - `@Scheduled(fixedDelayString = "${coin.market.refresh-delay-ms:1000}")`
  - calls `MarketSupportedMarketRefresher.refreshSupportedMarkets()`
- `feature/market/application/realtime/MarketSupportedMarketRefresher.java`
  - calls `providers.connector().marketDataGateway().loadSupportedMarkets()`
  - publishes `MarketSummaryUpdatedEvent`
- `feature/market/api/MarketRealtimeSseBroker.java`
  - consumes `MarketSummaryUpdatedEvent` and emits market summary SSE
- `feature/market/api/MarketSummaryResponse.java`
  - exposes `lastPrice` and `markPrice` from `MarketSummaryResult`

### Command/read services

- `feature/order/application/service/CreateOrderService.java`
  - order preview and create paths call `loadMarket(command.symbol())`
  - uses `lastPrice()` for placement/preview and `markPrice()` for position open/increase
- `feature/position/application/service/ClosePositionService.java`
  - close paths call `loadMarket(symbol)`
  - uses `lastPrice()` for market close/reference/reconciliation and `markPrice()` for finalization
- `feature/account/application/service/GetAccountSummaryService.java`
  - calls `loadMarket(snapshot.symbol())`
  - uses `markPrice()` for mark-to-market account summary
- `feature/position/application/service/GetOpenPositionsService.java`
  - calls `loadMarket(snapshot.symbol())`
  - uses `markPrice()` for open-position mark-to-market
- `feature/position/application/service/UpdatePositionTpslService.java`
  - calls `loadMarket(current.symbol())`
  - uses `markPrice()` for TP/SL validation and `lastPrice()` for pending close order cap reconciliation

### Execution processors

- `feature/order/application/realtime/PendingOrderFillProcessor.java`
  - consumes `MarketSummaryUpdatedEvent`
  - uses previous/current `lastPrice()` movement for pending limit fills
  - uses `markPrice()` when applying filled open/close orders
- `feature/order/application/realtime/PositionLiquidationProcessor.java`
  - uses `markPrice()` for liquidation trigger and `lastPrice()` for execution
- `feature/order/application/realtime/PositionTakeProfitStopLossProcessor.java`
  - uses `markPrice()` for TP/SL trigger and `lastPrice()` for execution
- `feature/order/application/service/MarketOrderExecutionService.java`
  - currently fans `MarketSummaryUpdatedEvent` into pending fills, liquidation, and TP/SL processing

### Not latest-price source migrations

These references use already persisted position state or domain calculations and are not direct REST latest-price reads:

- `feature/position/domain/LiquidationPolicy.java`
- `feature/position/domain/PositionSnapshot.java`
- `feature/position/infrastructure/persistence/*`
- `feature/position/application/service/UpdatePositionLeverageService.java`

## Command Fallback Policy

- Order preview requires fresh `TRADE` execution price and fresh `TICKER` mark price.
- Market order execution requires fresh `TRADE` execution price and fresh `TICKER` mark price.
- Manual market close follows the same policy as market order execution.
- Limit close placement validates/reference-checks against fresh realtime data.
- TP/SL update validation uses fresh `TICKER` mark price.
- Pending limit fills use accepted `TRADE` movement only and must not use REST fallback to generate fills.
- Liquidation and TP/SL triggers use `TICKER` mark price; execution uses fresh `TRADE` price.
- `REST_RECOVERY` can be accepted only when the caller opts in, the snapshot is fresh, and the response/action remains source-labeled.
- `REST_BOOTSTRAP` is startup seed/read-only state and must not satisfy command execution freshness.

## 검증 절차

- `cd backend && GRADLE_USER_HOME=/tmp/gradle-home ./gradlew test --tests '*MarketRealtimeSourceSnapshotTest' --console=plain`
- `cd backend && GRADLE_USER_HOME=/tmp/gradle-home ./gradlew architectureLint --console=plain`

## 후속 PR 메모

- PR 2 must confirm Bitget official field semantics before parser code is finalized.
- PR 4 must include a spy/fake test proving healthy market summary paths do not call `loadMarket()` or `loadSupportedMarkets()`.
- PR 5 and PR 6 must convert every command/processor listed in the audit or explicitly document why a site is recovery/history-only.
