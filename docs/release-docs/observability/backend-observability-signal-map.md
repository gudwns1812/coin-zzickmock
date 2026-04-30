# Backend Observability Signal Map

## Purpose

이 문서는 backend 관측성 릴리즈에서 우선 확인해야 할 metric과 log 신호를 정리한다.
루트 [OBSERVABILITY.md](/Users/hj.park/projects/coin-zzickmock/OBSERVABILITY.md)가 전체 원칙을 담당하고, 이 문서는 release 체크와 rollout 확인에 필요한 backend 신호 지도를 담당한다.

## Provider Instrumentation Rule

- Micrometer는 Spring Boot meter, Prometheus scrape, dashboard alert용 낮은 카디널리티 집계에 둔다.
- API 응답속도, use-case duration, 외부 연동 latency 같은 횡단 관심사는 `Providers`의 `TelemetryProvider` 또는 목적별 협력 객체 뒤에서 직접 구현한다.
- 요청/응답 공통 계측은 filter/interceptor 경계에서 수집하고, 기능 코드와 도메인 모델에는 계측 구현을 흩뿌리지 않는다.
- 외부 HTTP 연동 계측은 connector/infrastructure 경계에서 처리한다.

## Priority Signal Map

| Priority | Area | Signals | Release Question |
| ---: | --- | --- | --- |
| 1 | HTTP common path | `http.server.requests`, request/response size, slow request, 4xx/5xx by `endpoint_group` | 배포 후 어떤 endpoint group이 느리거나 실패하는가? |
| 2 | Market realtime and Bitget | market refresh, Bitget REST/WS latency, failure, fallback, REST requests per second, snapshot staleness | 실시간 시장 데이터가 stale이거나 Bitget 호출량이 튀는가? |
| 3 | SSE streams | active/opened/closed/rejected connections, send success/failure/duration, executor queue/rejection | stream 연결이 밀리거나 거절되는가? |
| 4 | Trading execution and risk | order preview/create/fill, fill claim miss, TP/SL trigger, liquidation, wallet balance changed | 가격 이벤트 이후 주문/포지션 상태 전이가 정상인가? |
| 5 | Reward, admin, leaderboard | reward notification sent/skipped/failure, admin transition result, leaderboard refresh/snapshot age/Redis failure | 운영자가 조치해야 할 업무 실패가 쌓이는가? |

## Backend Metrics

### HTTP And Runtime

- `http.server.requests` with `endpoint_group`, `method`, `status`
- `http.server.request.size` with `endpoint_group`
- `http.server.response.size` with `endpoint_group`
- `http.server.slow_requests.total` with `endpoint_group`
- JVM, DB connection pool, Redis command latency, process CPU, uptime

### Market And Bitget

- `market.refresh.total` with `result`
- `market.refresh.duration`
- `market.snapshot.staleness.seconds` with bounded `symbol`
- `market.bitget.request.total` with `operation`, `result`
- `market.bitget.request.duration` with `operation`
- `market.bitget.request.rate.per_second` with `operation`, `result`
- `market.bitget.fallback.total` with `operation`, `symbol`
- `market.bitget.websocket.state.current`
- `market.bitget.websocket.reconnect.total`
- `market.bitget.websocket.message.total` with `channel`, `result`

### History And Cache

- `market.history.db.lookup.total` with `interval`, `range_bucket`, `result`
- `market.history.redis.lookup.total` with `interval`, `range_bucket`, `result`
- `market.history.bitget.lookup.total` with `interval`, `range_bucket`, `result`
- `market.history.persist.total` with `result`, `symbol`
- `market.history.retry.pending.current`
- `market.history.retry.total` with `result`

### SSE

- `sse.connections.current` with `stream`
- `sse.connections.opened.total` with `stream`
- `sse.connections.closed.total` with `stream`, `reason`
- `sse.connections.rejected.total` with `stream`, `reason`
- `sse.send.total` with `stream`, `result`
- `sse.send.duration` with `stream`
- `sse.executor.queue.current`
- `sse.executor.rejected.total`

### Trading

- `trading.order.created.total` with `order_type`, `side`, `result`
- `trading.order.preview.total` with `result`
- `trading.order.fill.total` with `symbol`, `reason`, `result`
- `trading.order.claim.failure.total` with `reason`
- `trading.position.close.total` with `reason`
- `trading.position.liquidation.total` with `margin_mode`, `symbol`
- `trading.wallet.balance_changed.total` with `source`

### Reward, Admin, Leaderboard

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
- `market.snapshot.staleness.seconds`가 10초 이상 지속되면 Bitget WebSocket state, reconnect count, REST fallback을 함께 확인한다.
- SSE rejection이 증가하면 active connection, symbol/member limit, executor queue를 함께 확인한다.
- trading write 실패가 증가하면 order fill claim miss, liquidation, TP/SL trigger metric과 error log를 함께 확인한다.

## Label And Privacy Guardrails

- Prometheus label에 `memberId`, `orderId`, `requestId`, raw query string, raw cache key, email, phone number, JWT subject를 넣지 않는다.
- `symbol`은 지원 market symbol처럼 집합이 제한된 값에만 사용한다.
- Provider 기반 계측 context도 같은 민감정보 금지 기준을 따른다.
- member-specific 조사가 필요하면 metric label이 아니라 접근 제어가 있는 관리자 화면 또는 correlation id 기반 로그 조회를 사용한다.
