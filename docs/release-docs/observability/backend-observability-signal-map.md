# Backend Observability Signal Map

## Purpose

이 문서는 backend 관측성 릴리즈에서 우선 확인해야 할 metric과 log 신호를 정리한다.
루트 [OBSERVABILITY.md](/Users/hj.park/projects/coin-zzickmock/OBSERVABILITY.md)가 전체 원칙을 담당하고, 이 문서는 release 체크와 rollout 확인에 필요한 backend 신호 지도를 담당한다.

## Provider Instrumentation Rule

- Micrometer는 Spring Boot meter, Prometheus scrape, dashboard alert용 낮은 카디널리티 집계에 둔다.
- API 응답속도, use-case duration, 외부 연동 latency 같은 횡단 관심사는 `Providers`의 `TelemetryProvider` 또는 목적별 협력 객체 뒤에서 직접 구현한다.
- 요청/응답 공통 계측은 filter/interceptor 경계에서 수집하고, 기능 코드와 도메인 모델에는 계측 구현을 흩뿌리지 않는다.
- 외부 HTTP 연동 계측은 connector/infrastructure 경계에서 처리한다.
- SSE connection lifecycle은 feature API broker가 직접 소유하므로 `providers.telemetry.SseTelemetry`라는 좁은 목적형 interface를 주입한다.
  Micrometer 구현은 `providers.infrastructure.MicrometerSseTelemetry`에만 둔다.
- 처리된 backend domain/application 실패 로그는 `GlobalExceptionHandler`의 `ErrorCode.logLevel()` 정책을 기준으로 본다.
  feature/provider/application boundary 로그는 raw identifier나 provider detail 대신 `provider`, `cache`, `operation`, `reason`, `symbol`, `interval`, `range_bucket` 같은 낮은 카디널리티 context만 보완한다.

## Priority Signal Map

| Priority | Area | Signals | Release Question |
| ---: | --- | --- | --- |
| 1 | HTTP common path | `http.request.total`, `http.request.duration`, `http.request.slow.total`, `http.payload.size.bucket.total` by `endpoint_group` | 배포 후 어떤 endpoint group이 느리거나 실패하는가? |
| 2 | Market realtime and Bitget | market refresh, Bitget REST/WS latency, failure, fallback, REST requests per second, snapshot staleness | 실시간 시장 데이터가 stale이거나 Bitget 호출량이 튀는가? |
| 3 | SSE streams | active/opened/closed/rejected connections, send success/failure/duration, executor queue/rejection | stream 연결이 밀리거나 거절되는가? |
| 4 | Trading execution and risk | order preview/create/fill, fill claim miss, TP/SL trigger, liquidation, wallet balance changed | 가격 이벤트 이후 주문/포지션 상태 전이가 정상인가? |
| 5 | Activity, reward, admin, leaderboard | DAU recording result, reward notification sent/skipped/failure, admin transition result, leaderboard refresh/snapshot age/Redis failure | 서비스 활동 수집이나 운영자 조치 실패가 쌓이는가? |

## Backend Metric Catalog

This catalog separates metrics implemented by the current observability rollout from planned release signals. Planned
signals stay here as dashboard/runbook targets, but they must not be treated as shipped Prometheus meters until code and
tests add them.

### Implemented: HTTP And Runtime

- `http.request.total` with `method`, `route_pattern`, `endpoint_group`, `status`, `status_family`
- `http.request.duration` with `method`, `route_pattern`, `endpoint_group`, `status_family`
- `http.request.slow.total` with `method`, `route_pattern`, `endpoint_group`, `status_family`
- `http.payload.size.bucket.total` with `method`, `route_pattern`, `endpoint_group`, `status_family`, `direction`, `size_bucket`
- JVM, DB connection pool, Redis command latency, process CPU, uptime

### Implemented: Bitget REST

- `market.bitget.request.total` with `operation`, `result`
- `market.bitget.request.duration` with `operation`
- `market.bitget.request.rate.per_second` with `operation`, `result`
  - dashboard-derived: `rate(market_bitget_request_total[1m])`
- `market.bitget.fallback.total` with `operation`, `symbol`

### Implemented: SSE

- `sse.connections.current` with `stream`
- `sse.connections.opened.total` with `stream`
- `sse.connections.closed.total` with `stream`, `reason`
- `sse.connections.rejected.total` with `stream`, `reason`
- `sse.send.total` with `stream`, `result`
- `sse.send.duration` with `stream`, `result`
- `sse.executor.rejected.total` with `stream`

`sse.connections.current` exposes only implemented stream labels. Unknown or malformed stream names may still be
sanitized to `stream="unknown"` on diagnostic counters/timers, but they must not create an active-connection gauge
series because `sum by (stream) (sse_connections_current)` is an operator-facing current-state panel.

Implemented `stream` values:

- `market`
- `market_candle`
- `trading_execution`

Implemented `reason` values:

- `total_limit`
- `symbol_limit`
- `member_limit`
- `client_complete`
- `timeout`
- `error`
- `send_failure`
- `executor_rejected`
- `client_replaced`
- `unknown`

Executor queue depth is not implemented yet; add a separate bounded queue gauge only if the executor exposes stable queue depth.

### Implemented: Daily Active Users

- DAU source of truth: `member_daily_activity` table, one row per KST `activity_date` and `member_id`.
- DAU ingestion is async best-effort. Login/authenticated API request paths enqueue activity events; DB writes use the `(activity_date, member_id)` unique key with idempotent upsert.
- Long-term identifier-free trend: `daily_active_user_summary`.
- Default summary schedule: every day 00:05 KST snapshots yesterday's DAU. Configure with `DAU_SUMMARY_ENABLED` and `DAU_SUMMARY_CRON`.
- `dau.activity.record.total` with `source`, `result`

Implemented `source` values:

- `login`
- `authenticated_api`

Implemented `result` values:

- `queued`
- `success`
- `failure`
- `skipped`
- `rejected`

### Implemented: Market Trade Movement Queue

- `market.trade.movement.queue.size.current`
- `market.trade.movement.queue.drop.total` with `reason`
- `market.trade.movement.worker.failure.total` with `reason`

Implemented `reason` values:

- `full`
- `runtime_exception`

Pending limit fill은 단일 백엔드 인스턴스의 live worker가 수신한 accepted WebSocket trade movement만 기준으로 한다.
restart-gap 징후는 WebSocket reconnect, snapshot staleness, trade movement queue depth/drop, worker failure를 함께 본다.

### Planned: Market Realtime, WebSocket, History, And Cache

- `market.refresh.total` with `result`
- `market.refresh.duration`
- `market.snapshot.staleness.seconds` with bounded `symbol`
- `market.bitget.websocket.state.current`
- `market.bitget.websocket.reconnect.total`
- `market.bitget.websocket.message.total` with `channel`, `result`
- `market.history.db.lookup.total` with `interval`, `range_bucket`, `result`
- `market.history.redis.lookup.total` with `interval`, `range_bucket`, `result`
- `market.history.bitget.lookup.total` with `interval`, `range_bucket`, `result`
- `market.history.persist.total` with `result`, `symbol`
- `market.history.retry.pending.current`
- `market.history.retry.total` with `result`

### Planned: Trading

- `trading.order.created.total` with `order_type`, `side`, `result`
- `trading.order.preview.total` with `result`
- `trading.order.fill.total` with `symbol`, `reason`, `result`
- `trading.order.claim.failure.total` with `reason`
- `trading.position.close.total` with `reason`
- `trading.position.liquidation.total` with `margin_mode`, `symbol`
- `trading.wallet.balance_changed.total` with `source`

### Planned: Reward, Admin, Leaderboard

- `reward.redemption.created.total`
- `reward.redemption.transition.total` with `from`, `to`, `result`
- `reward.notification.sent.total` with `provider`, `result`
- `admin.action.total` with `action`, `resource`, `result`
- `leaderboard.refresh.total` with `result`
- `leaderboard.refresh.duration`
- `leaderboard.snapshot.age.seconds`

## Release Checks

- 배포 후 30분 동안 endpoint group별 p95/p99, 5xx, slow request를 배포 전 baseline과 비교한다.
- Bitget REST requests per second가 baseline보다 급증하면 cache miss, history backfill, SSE consumer 상태를 함께 확인한다.
- `market.snapshot.staleness.seconds`가 구현된 뒤 10초 이상 지속되면 Bitget WebSocket state, reconnect count, REST fallback을 함께 확인한다.
- SSE rejection이 증가하면 active connection, symbol/member limit, executor queue를 함께 확인한다.
- trading write 실패가 증가하면 order fill claim miss, liquidation, TP/SL trigger metric과 error log를 함께 확인한다.
- `dau.activity.record.total{result=~"failure|rejected"}`가 증가하면 activity executor saturation, DB write latency, `member_daily_activity` upsert failure log를 함께 확인한다.

## Label And Privacy Guardrails

- Prometheus label에 `memberId`, `orderId`, `requestId`, raw query string, raw cache key, email, phone number, JWT subject를 넣지 않는다.
- `symbol`은 지원 market symbol처럼 집합이 제한된 값에만 사용한다.
- Provider 기반 계측 context도 같은 민감정보 금지 기준을 따른다.
- member-specific 조사가 필요하면 metric label이 아니라 접근 제어가 있는 관리자 화면 또는 correlation id 기반 로그 조회를 사용한다.
- CoreException detail, upstream provider message, raw route/URI, raw cache key는 release log check에서 결함으로 본다.
