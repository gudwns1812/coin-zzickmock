# OBSERVABILITY.md

## 목적

이 문서는 `coin-zzickmock`의 관측성 기준을 정의하는 입구 문서다.
운영자는 Grafana에서 시스템 상태를 보고, 서비스 관리자는 관리자 페이지에서 사용자/도메인 상태를 본다.
두 화면은 같은 데이터를 중복해서 보여 주는 것이 아니라 서로 다른 질문에 답해야 한다.

## 이 문서의 역할

이 문서는 아래 역할만 맡는다.

- Micrometer, Prometheus, Grafana, Loki를 기준 관측성 스택으로 고정한다.
- Grafana 모니터링과 관리자 페이지 모니터링의 책임 경계를 나눈다.
- 백엔드, 프론트, 배치, 외부 연동, 캐시, 로그에 붙일 최소 신호를 정의한다.
- 요청/응답을 관측할 때 기록할 정보와 기록하지 말아야 할 정보를 구분한다.
- 엔드포인트 성격별 지연 기준과 p95/p99 악화 알림 기준을 정의한다.
- 메트릭/로그/관리자 화면을 추가할 때 지켜야 할 태그, 개인정보, 카디널리티 기준을 고정한다.

즉, "DevOps 관측성 작업용 체크인 문서"라고 보면 된다.

## 기준 스택

- 메트릭: Spring Boot + Micrometer로 애플리케이션 meter를 수집하고 Prometheus가 scrape한다.
- 로그: 애플리케이션은 구조화 로그를 남기고 Loki가 수집한다.
- 대시보드와 알림: Grafana가 Prometheus 메트릭과 Loki 로그를 함께 보여 준다.
- 관리자 모니터링: 관리자 페이지는 운영 인프라 지표가 아니라 서비스 운영자가 조치할 수 있는 도메인 상태를 보여 준다.

새 관측성 구현은 이 스택을 먼저 사용한다.
다른 SaaS나 에이전트를 추가해야 하면 왜 Micrometer/Prometheus/Loki/Grafana로 충분하지 않은지 먼저 설명해야 한다.
API 응답속도, use-case duration, 외부 연동 latency 같은 횡단 관심사는 `Providers`의 `TelemetryProvider`
또는 목적별 협력 객체 뒤에서 직접 구현한다.
기능 코드와 도메인 모델은 계측 구현 세부사항을 알지 않는다.

## Grafana와 관리자 페이지의 경계

### Grafana

Grafana는 시스템 운영자와 개발자가 장애를 감지하고 원인을 좁히는 곳이다.
Grafana에는 낮은 카디널리티의 집계 지표, 로그 검색, 알림 기준을 둔다.

Grafana가 답해야 하는 질문:

- 서비스가 살아 있는가?
- 어떤 엔드포인트, 스케줄러, 외부 커넥터가 느리거나 실패하는가?
- 5xx, timeout, rejected SSE connection, 큐 backlog가 증가했는가?
- Bitget, Redis, MySQL, SMTP 같은 외부 의존성이 정상인가?
- 캐시 hit/miss, eviction, read/write failure가 평소와 다르게 변했는가?
- 최근 배포 이후 에러율, p95, p99, 캐시 miss율이 나빠졌는가?
- 장애가 특정 symbol, 엔드포인트, job, environment에 한정되는가?

Grafana에 두지 않는 것:

- 개별 회원의 상세 개인정보
- 관리자 업무 처리 화면
- 주문 승인, 교환권 처리, 회원 제재 같은 write action
- 비즈니스 운영자가 직접 처리해야 하는 업무 큐의 원장 화면

### 관리자 페이지

관리자 페이지는 서비스 운영자가 사용자를 지원하고 도메인 업무를 처리하는 곳이다.
관리자 페이지에는 집계 운영 상태와 조치 가능한 업무 항목을 둔다.

관리자 페이지가 답해야 하는 질문:

- 지금 운영자가 처리해야 할 요청은 무엇인가?
- 특정 회원의 주문, 포지션, 포인트, 교환권 상태가 정상인가?
- 실패했거나 보류된 업무 항목을 사람이 재시도, 승인, 반려, 취소해야 하는가?
- 공지, 상품, 회원 상태 같은 운영 데이터를 안전하게 수정할 수 있는가?
- 시스템이 degraded 상태라면 운영자에게 어떤 업무 제한이나 안내가 필요한가?

관리자 페이지에 두지 않는 것:

- JVM memory, GC, thread pool, HTTP p95 같은 인프라 지표
- PromQL, LogQL, stack trace 원문 검색
- 카디널리티가 높은 실시간 메트릭 전체
- 비개발 운영자가 조치할 수 없는 내부 예외 세부사항

### 공통 규칙

같은 현상을 양쪽에 모두 보여 줄 수는 있지만 표현 목적은 달라야 한다.
예를 들어 리워드 교환 알림 실패는 Grafana에서는
`reward.notification.sent.total{result="failure"}`와 로그로 보고, 관리자 페이지에서는 "알림 발송 실패 요청" 업무 큐로 보여 준다.

캐시 miss율 상승은 Grafana에서는 성능/비용 리스크로 보고, 관리자 페이지에서는 사용자가 체감할 수 있는
지연이나 데이터 갱신 지연이 있을 때만 degraded 안내로 보여 준다.

## 절대 규칙

- Micrometer meter 이름은 낮은 카디널리티를 유지한다.
- Prometheus label에는 `memberId`, `orderId`, `requestId`, 휴대폰 번호, 토큰 원문, email, URL query 원문을 넣지 않는다.
- 요청/응답 관측은 metadata 중심으로 기록하고, request body와 response body 원문은 기본적으로 기록하지 않는다.
- 로그에는 원인 추적용 correlation id와 낮은 카디널리티 context를 남기되, 민감 정보 원문은 남기지 않는다.
- 관리자 페이지는 민감 데이터를 보여 줄 수 있지만 역할 기반 접근 제어와 감사 로그 기준을 먼저 충족해야 한다.
- 도메인 모델 안에서 Micrometer, logger, Sentry, Prometheus API를 직접 호출하지 않는다.
- 애플리케이션 계층의 공통 계측은 `TelemetryProvider` 또는 목적별 협력 객체 뒤에 둔다.
- API 응답속도 같은 공통 요청/응답 계측은 filter/interceptor 같은 경계에서 수집하되, 기록은 `TelemetryProvider`를 통해 수행한다.
- 외부 HTTP 연동 계측은 connector/infrastructure 경계에서 처리하고 기능 코드에 흩뿌리지 않는다.
- 로그 메시지는 사람이 검색할 수 있는 안정적인 event name과 key-value context를 가져야 한다.
- 알림은 "운영자가 바로 판단할 수 있는 증상"에 걸고, 원인 후보는 dashboard와 runbook으로 연결한다.

## 메트릭 이름과 label 기준

기본 이름 규칙:

- Counter: `<domain>.<event>.total`
- Timer: `<domain>.<operation>.duration`
- Gauge: `<domain>.<state>.current`
- Distribution summary: `<domain>.<payload>.size`

권장 공통 labels:

- `application`: `coin-zzickmock`
- `service`: `backend`, `frontend`
- `environment`: `local`, `ci`, `preview`, `staging`, `production`
- `endpoint`: 정규화된 route pattern
- `endpoint_group`: `health`, `auth`, `market_read`, `history_read`, `trading_write`, `admin_write`, `sse_open`, `external_proxy`
- `method`: HTTP method
- `status`: HTTP status family 또는 카디널리티가 낮은 exact status
- `result`: `success`, `failure`, `empty`, `timeout`, `rejected`, `fallback`
- `symbol`: 지원 market symbol처럼 집합이 제한된 값에만 사용
- `job`: scheduler/job name
- `provider`: `bitget`, `redis`, `mysql`, `smtp`
- `cache`: `market_snapshot`, `market_supported_symbols`, `market_history`, `leaderboard`
- `operation`: `read`, `write`, `evict`, `refresh`, `fill`, `invalidate`

금지 labels:

- 사용자 식별자 원문
- query string이 포함된 request path 원문
- order id, request id, session id, JWT subject
- exception message 원문
- cache key 원문
- user-agent 원문
- 임의의 frontend route state

## 요청/응답 관측 기준

요청/응답 관측은 "무엇이 느리고 실패하는지"를 알기 위한 것이지, 사용자 데이터를 수집하기 위한 것이 아니다.
기본값은 metadata 기록이고, body 원문 기록은 금지한다.

### 요청에서 기록할 정보

- `correlationId`
- `requestId`
- `method`
- `pathPattern`
- `endpoint_group`
- `contentLength` 또는 request size bucket
- 인증 여부: `authenticated=true|false`
- actor role: `USER`, `ADMIN`, `ANONYMOUS` 같은 낮은 카디널리티 값
- client type: `browser`, `server`, `internal`, `unknown`
- 시작 시각과 종료 시각

### 응답에서 기록할 정보

- `status`
- `status_family`: `2xx`, `3xx`, `4xx`, `5xx`
- `durationMs`
- response size bucket
- error code: `ErrorCode`처럼 제한된 enum
- `result`: `success`, `client_error`, `server_error`, `timeout`, `cancelled`

### 기록하지 않는 정보

- password, JWT, cookie, access token, refresh token
- phone number, email, 주소, account identifier 원문
- request/response body 원문
- query string 원문
- 파일 업로드 원문
- 외부 API credential

### 제한적 payload 관측

payload 자체가 필요한 경우에도 원문 대신 아래 중 하나를 사용한다.

- size bucket: `0-1kb`, `1-10kb`, `10-100kb`, `100kb+`
- schema version
- payload type
- validation error code
- 비가역 hash. 단, hash도 개인 식별자를 재식별하는 용도로 쓰지 않는다.

운영 장애 분석에 body 원문이 꼭 필요하면 별도 feature flag, 샘플링, 마스킹, 보관 기간, 접근 권한을 먼저 정의해야 한다.

## 엔드포인트 성격별 기준

모든 엔드포인트에 같은 지연 기준을 적용하지 않는다.
Grafana dashboard와 alert는 `endpoint_group`별로 기준을 다르게 잡는다.
아래 값은 시작점이며, 실제 운영 baseline이 쌓이면 조정한다.

| 엔드포인트 그룹 | 예시 | p95 경고 | p99 긴급 | 비고 |
| --- | --- | ---: | ---: | --- |
| `health` | `/actuator/health` | 100ms | 250ms | 장애 감지용이라 매우 낮게 유지한다. |
| `auth` | login, refresh, logout | 300ms | 800ms | 인증 실패율과 함께 본다. |
| `market_read` | market summary, current price | 300ms | 800ms | 실시간 화면 체감과 직접 연결된다. |
| `history_read` | candle/history 조회 | 800ms | 2s | DB/Redis/Bitget 보충 여부를 함께 본다. |
| `trading_write` | order, close, TP/SL | 500ms | 1.5s | 실패율과 중복/경합 로그를 함께 본다. |
| `admin_read` | admin list/detail | 1s | 3s | 운영자 업무 화면 기준이다. |
| `admin_write` | approve/reject/update | 700ms | 2s | 감사 로그와 상태 전이 conflict를 함께 본다. |
| `sse_open` | market/trading stream open | 500ms | 1.5s | open latency와 rejected count를 본다. |
| `external_proxy` | frontend proxy route | 800ms | 2s | upstream status와 502를 함께 본다. |

SSE 연결 유지 중 성능은 일반 HTTP duration보다 아래 신호를 우선한다.

- active connection 수
- rejected connection 수
- send failure 수
- event delivery gap
- market snapshot staleness

## p95/p99 악화 알림 기준

p95/p99는 단일 spike로 알림을 보내지 않는다.
지속적으로 나빠지는지를 보려면 최소 두 개의 창을 함께 본다.

기본 규칙:

- `경고`: 엔드포인트 그룹별 p95가 기준을 10분 이상 넘거나, 최근 30분 p95가 이전 24시간 같은 시간대 baseline보다 50% 이상 나빠질 때.
- `긴급`: 엔드포인트 그룹별 p99가 기준을 5분 이상 넘고 동시에 요청 수가 최소 표본 수를 넘을 때.
- `회귀`: 배포 이후 30분 동안 p95 또는 p99가 배포 전 6시간 baseline보다 30% 이상 나빠질 때.
- `노이즈 방지`: 5분 요청 수가 너무 적은 엔드포인트는 p95/p99 알림 대신 error count와 slow request log를 본다.

권장 표본 수:

- 일반 API: 5분에 최소 50 요청
- admin API: 15분에 최소 20 요청
- trading write API: 15분에 최소 10 요청이라도 p99와 error log를 함께 본다.
- health check: 표본 수와 무관하게 실패율 중심으로 본다.

알림에는 반드시 아래 링크를 붙인다.

- 엔드포인트 그룹 dashboard
- slow request Loki query
- 최근 배포 marker
- 관련 owner
- rollback 또는 mitigation note

## 백엔드 공통 메트릭

### HTTP와 런타임

Grafana에 추가할 지표:

- `http.server.requests`: route/method/status별 count, p50, p95, p99
- `http.server.request.size`: 요청 size bucket
- `http.server.response.size`: 응답 size bucket
- `http.server.slow_requests.total`: 엔드포인트 그룹별 slow request 수
- `jvm.memory.used`, `jvm.gc.pause`, `jvm.threads.live`
- `process.cpu.usage`, `system.cpu.usage`
- DB connection pool active/idle/pending
- Redis command latency와 failure
- application startup success와 uptime

최소 알림:

- backend health check 실패
- 5xx rate 5분 이상 상승
- 엔드포인트 그룹별 p95/p99 지속 악화
- DB connection pool 포화
- Redis unavailable로 cache-backed flow가 fallback되는 상황

### 요청/응답 로그

중요 로그:

- 요청 완료: `event=http.request.completed`
- slow request: `event=http.request.slow`
- server error: `event=http.request.server_error`
- client error spike 분석용: `event=http.request.client_error`
- upstream proxy failure: `event=http.proxy.upstream_failure`

`http.request.completed`는 모든 요청에 남길 수 있지만, 운영 비용이 부담되면 성공 요청은 sampling하고 4xx/5xx/slow request는 항상 남긴다.

## 캐시 관측 기준

캐시는 성능 최적화 수단이면서 장애 원인이 될 수 있다.
캐시 관측은 "hit/miss가 적절한가", "Redis 장애 시 fallback이 안전한가",
"캐시 key 설계가 카디널리티를 폭발시키지 않는가"를 확인할 수 있어야 한다.

### 공통 캐시 메트릭

Grafana에 추가할 지표:

- `cache.operation.total` with `cache`, `operation`, `result`
- `cache.operation.duration` with `cache`, `operation`
- `cache.hit.total` with `cache`
- `cache.miss.total` with `cache`, `reason`
- `cache.write.total` with `cache`, `result`
- `cache.evict.total` with `cache`, `reason`
- `cache.value.size` with `cache`, size bucket
- `cache.keyspace.current` with `cache`, 가능할 때만 사용
- `cache.fallback.total` with `cache`, `fallback=db|bitget|local|empty`

캐시별로 필요한 추가 신호:

- `market_snapshot`: snapshot staleness, supported symbol count
- `market_supported_symbols`: refresh success/failure, empty refresh count
- `market_history`: interval/range bucket별 hit/miss, Redis unavailable, Bitget fallback
- `leaderboard`: snapshot age, Redis write failure, refresh duration

### 캐시 로그

중요 로그:

- 캐시 hit/miss 샘플링: `event=cache.lookup`
- 캐시 읽기 실패: `event=cache.read.failure`
- 캐시 쓰기 실패: `event=cache.write.failure`
- 캐시 evict: `event=cache.evict`
- fallback 사용: `event=cache.fallback.used`

로그에 cache key 원문을 남기지 않는다.
대신 `cache`, `operation`, `result`, `reason`, `symbol`, `interval`, `rangeBucket`처럼 제한된 context를 남긴다.

### 캐시 알림 기준

초기 알림 기준:

- `market_history` cache miss율이 30분 동안 baseline 대비 2배 이상 증가.
- Redis read/write 실패가 5분 동안 1% 이상.
- Bitget fallback이 10분 동안 지속적으로 증가.
- leaderboard snapshot age가 refresh interval의 2배를 초과.
- market snapshot staleness가 10초 이상 지속.

캐시 miss 자체는 장애가 아닐 수 있다.
알림은 miss 증가가 latency, Bitget 호출량, Redis failure, 사용자 체감 지연과 함께 나타날 때 우선한다.

## 시장 데이터 메트릭

시장 데이터는 backend가 짧은 주기로 갱신하고 SSE로 전파하는 핵심 경로다.

Grafana에 추가할 지표:

- `market.refresh.total` with `result`
- `market.refresh.duration`
- `market.snapshot.staleness.seconds` by bounded `symbol`
- `market.bitget.request.total` with `operation`, `result`
- `market.bitget.request.duration` with `operation`
- `market.bitget.request.rate.per_second` with `operation`, `result`
- `market.bitget.fallback.total` with `operation`, `symbol`
- `market.history.persist.total` with `result`, `symbol`
- `market.history.retry.pending.current`
- `market.history.retry.total` with `result`
- `market.history.db.lookup.total` with `interval`, `range_bucket`, `result`
- `market.history.redis.lookup.total` with `interval`, `range_bucket`, `result`
- `market.history.bitget.lookup.total` with `interval`, `range_bucket`, `result`

중요 로그:

- Bitget 응답 사용 불가: `event=market.bitget.unusable_response`
- Bitget timeout/failure: `event=market.bitget.failure`
- market refresh 결과가 비어 있음: `event=market.refresh.empty`
- 닫힌 1분 캔들 저장 실패: `event=market.history.persist.failure`
- retry 등록/해결: `event=market.history.retry.queued`, `event=market.history.retry.resolved`

## SSE 스트림 메트릭

Grafana에 추가할 지표:

- `sse.connections.current` with `stream=market|trading`
- `sse.connections.opened.total` with `stream`
- `sse.connections.closed.total` with `stream`, `reason`
- `sse.connections.rejected.total` with `stream`, `reason=total_limit|symbol_limit|member_limit`
- `sse.send.total` with `stream`, `result`
- `sse.send.duration` with `stream`
- `sse.executor.queue.current`
- `sse.executor.rejected.total`

중요 로그:

- 연결 거절: `event=sse.connection.rejected`
- 전송 실패: `event=sse.send.failure`
- route/symbol별 반복 disconnect: `event=sse.disconnect.rate_limited_context`

Grafana label에는 `memberId`를 넣지 않는다.
회원 단위 지원 조사가 필요하면 log correlation id 또는 접근 제어가 있는 관리자 페이지 조회를 사용한다.

## 거래와 리스크 이벤트 메트릭

Grafana에 추가할 지표:

- `trading.order.created.total` with `order_type`, `side`, `result`
- `trading.order.preview.total` with `result`
- `trading.order.fill.total` with `result`, `symbol`, `reason=limit|tp|sl|liquidation`
- `trading.order.claim.failure.total` with `reason`
- `trading.position.close.total` with `reason`
- `trading.position.liquidation.total` with `margin_mode`, `symbol`
- `trading.wallet.balance_changed.total` with `source`

중요 로그:

- 주문 체결 claim 성공: `event=trading.order.fill.claimed`
- 주문 체결 claim 경합 실패: `event=trading.order.fill.claim_missed`
- 조건부 종료 발동: `event=trading.position.conditional_close.triggered`
- 강제청산: `event=trading.position.liquidated`
- 지갑 변경 실패: `event=trading.wallet.mutation.failure`

로그에는 감사/지원에 필요한 경우에만 `orderId` 또는 `memberId`를 포함할 수 있다.
이 값들은 Loki 로그 context에는 들어갈 수 있지만 Prometheus label에는 넣지 않는다.

## 리워드와 관리자 운영 메트릭

Grafana에 추가할 지표:

- `reward.redemption.created.total`
- `reward.redemption.transition.total` with `from`, `to`, `result`
- `reward.notification.sent.total` with `provider=smtp`, `result`
- `admin.action.total` with `action`, `resource`, `result`

중요 로그:

- 리워드 알림 건너뜀: `event=reward.notification.skipped`
- 리워드 알림 실패: `event=reward.notification.failure`
- 관리자 상태 전이 conflict: `event=admin.state_transition.conflict`

관리자 페이지는 pending/failed redemption 업무 큐를 보여 준다.
Grafana는 rate, failure, latency, log를 보여 준다.

## 리더보드 메트릭

Grafana에 추가할 지표:

- `leaderboard.refresh.total` with `result`
- `leaderboard.refresh.duration`
- `leaderboard.snapshot.update.total` with `result`
- `leaderboard.snapshot.age.seconds`

중요 로그:

- 리더보드 전체 refresh 실패: `event=leaderboard.refresh.failure`
- 리더보드 회원 refresh 실패: `event=leaderboard.member_refresh.failure`
- 리더보드 snapshot stale: `event=leaderboard.snapshot.stale`

## 프론트엔드 메트릭과 이벤트

프론트 운영 telemetry는 주로 Sentry와 Grafana의 로그/edge 지표에 둔다.
관리자 페이지를 브라우저 성능 dashboard로 만들지 않는다.

추적할 프론트 신호:

- route transition error count
- hydration/runtime error count
- 정규화된 backend family별 API call failure count
- stream별 EventSource open/error/close count
- EventSource message parse failure count
- futures stream route의 proxy route 502 count
- Sentry release와 environment tags
- frontend route handler의 request/response metadata

중요 로그/events:

- `event=frontend.sse.open`
- `event=frontend.sse.error`
- `event=frontend.sse.parse_failure`
- `event=frontend.proxy.upstream_failure`
- `event=frontend.route_handler.completed`
- `event=frontend.route_handler.slow`

브라우저 사용자 식별자를 메트릭에 넣지 않는다.
지원 상관관계가 필요하면 backend log와 join할 수 있는 짧은 수명의 correlation id를 사용한다.

## Loki 로그 기준

운영 로그는 구조화된 key-value context로 읽을 수 있어야 한다.

가능하면 포함할 공통 필드:

- `event`
- `level`
- `service`
- `environment`
- `correlationId`
- `requestId`
- `method`
- `pathPattern`
- `endpointGroup`
- `status`
- `durationMs`
- `requestSizeBucket`
- `responseSizeBucket`

도메인 필드:

- `symbol`
- `interval`
- `rangeBucket`
- `job`
- `provider`
- `cache`
- `operation`
- `result`
- `reason`

민감 정보 기준:

- password, JWT, cookie, 휴대폰 번호, email, access token, refresh token, SMTP credential 원문을 남기지 않는다.
- request/response body는 기본적으로 남기지 않는다.
- 지원 업무에 member-specific audit data가 필요하면 최소 식별자만 남기고 메트릭 label에는 넣지 않는다.

로그 레벨:

- `DEBUG`: 예상 가능한 disconnect, 아직 pending인 retry, optional auth miss, sampled cache hit/miss.
- `INFO`: 운영상 의미 있는 상태 전이 성공, retry resolved, release marker.
- `WARN`: 외부 의존성 fallback, retry queued, notification skipped/failed, 복구 가능한 persistence miss, 지속적인 slow endpoint.
- `ERROR`: 처리되지 않은 server exception, 데이터 무결성 리스크, 개입이 필요한 반복 실패.

## Grafana dashboard 구성

최소 dashboard:

- 백엔드 개요: health, traffic, errors, latency, JVM, DB, Redis.
- 엔드포인트 지연: endpoint group별 p50/p95/p99, slow request, 4xx/5xx.
- 요청/응답: request size, response size, status family, slow request logs.
- 캐시 개요: cache hit/miss, read/write failure, fallback, keyspace, value size.
- 시장 데이터: refresh success, Bitget latency/failure, snapshot staleness, history persistence, retry backlog.
- SSE 스트림: active connections, rejected connections, send failures, executor queue.
- 거래 런타임: order creation/fill, TP/SL, liquidation, wallet balance events.
- 리워드/관리자 운영: redemption queue rates, notification failures, admin action failures.
- 프론트 런타임: Sentry errors, route failures, proxy failures, SSE browser errors.
- 릴리즈 관찰: deployment marker, error/latency/cache miss deltas, rollback triggers.

각 dashboard는 맨 위에 "지금 깨졌는가?"를 판단할 수 있는 요약 row를 둔다.
로그 원문을 먼저 읽어야만 장애 여부를 알 수 있는 dashboard는 만들지 않는다.

## 알림 정책

알림은 조치가 필요할 때만 보낸다.

긴급 알림 후보:

- backend health check down
- 5xx가 지속적으로 증가
- 엔드포인트 그룹별 p95/p99 지속 악화
- core API latency 기준 초과
- market snapshot staleness 기준 초과
- Bitget failure로 fallback/empty market data가 지속됨
- SSE rejection spike
- DB connection pool saturation
- Redis unavailable로 cache-backed path가 지속 실패
- reward notification failure가 일회성이 아니라 지속됨

티켓성 알림 후보:

- history cache miss율 상승
- leaderboard refresh intermittent failure
- frontend parse failure가 baseline보다 증가
- retry backlog가 0보다 크지만 증가하지는 않음
- request/response size가 baseline보다 크게 증가

각 알림은 아래를 연결해야 한다.

- dashboard panel
- Loki query
- 관련 owner
- 최근 release marker
- rollback 또는 mitigation note

## 관리자 모니터링 화면

권장 관리자 모니터링 화면:

- 리워드 교환 요청: pending, failed notification, cancelled, approved/rejected history.
- 거래 지원 조회: member positions, open orders, recent fills, liquidation history.
- 시장 상태 요약: supported symbols, last backend snapshot time, degraded indicator.
- 캐시/degraded 안내 요약: 운영자에게 보여 줄 수준의 cache/market degraded 상태.
- 시스템 공지: current release id, known degraded dependencies, operator notes.
- 관리자 감사: shop item, redemption, member, operational state 변경자와 변경 시각.

관리자 페이지 데이터는 product table 또는 명시적인 support projection에서 가져온다.
공개 관리자 프론트에서 Prometheus나 Loki를 직접 질의하지 않는다.
필요하면 인증된 backend endpoint가 query detail을 숨기고 role check를 수행해야 한다.

## 구현 경계

백엔드:

- Micrometer는 infrastructure 또는 application orchestration 경계에서 사용한다.
- domain model은 framework-free로 유지한다.
- use-case와 business event 기록은 `TelemetryProvider`를 우선 사용한다.
- Bitget, SMTP 같은 외부 연동은 connector-level instrumentation으로 계측한다.
- scheduled job은 scheduler-level instrumentation으로 계측한다.
- meter 등록은 이름과 label이 일관되도록 충분히 중앙화한다.
- HTTP request/response 관측은 filter/interceptor 계층에서 공통 처리한다.
- body 원문 logging이 필요한 경우 별도 보안/운영 결정을 먼저 남긴다.

프론트:

- 기존 Sentry 설정을 browser/runtime error 기준으로 사용한다.
- EventSource와 proxy failure event는 운영 불확실성을 줄이는 곳에만 명시적으로 추가한다.
- route handler의 upstream request/response metadata는 구조화 로그로 남긴다.
- 명시적 결정 없이 새 전역 client telemetry library를 추가하지 않는다.

문서:

- 새 metric family는 구현 전 또는 구현과 같은 작업에서 이 문서에 추가한다.
- 새 dashboard나 alert 이름이 rollout의 일부라면 release note에 반영한다.
- 관측성 변경으로 환경 변수가 추가되면
  [docs/release-docs/01-environments-and-artifacts.md](/Users/hj.park/projects/coin-zzickmock/docs/release-docs/01-environments-and-artifacts.md)를 갱신한다.

## 릴리즈와 장애 대응에서의 사용

릴리즈 전:

- 변경된 subsystem의 dashboard가 있는지 확인한다.
- dashboard가 없다면 release note에 monitoring gap을 적는다.
- 알림 기준이 배포 직후 바로 울릴 것으로 예상되는지 확인한다.
- release id 또는 commit SHA를 Grafana에서 배포 시점과 metric 변화를 연결할 수 있게 기록한다.

릴리즈 후:

- 5xx, endpoint group별 p95/p99, market staleness, SSE rejection, cache miss/failure, external dependency error를 본다.
- 배포 전후 30분 p95/p99와 캐시 miss율 변화를 확인한다.
- release note에 관측한 리스크를 기록한다.
- rollback trigger가 울리면
  [docs/release-docs/03-rollout-and-rollback.md](/Users/hj.park/projects/coin-zzickmock/docs/release-docs/03-rollout-and-rollback.md)를 장애 대응 입구로 사용한다.

## 현재 저장소 메모

- backend에는 이미 `TelemetryProvider` 추상화와 no-op 구현이 있다.
- market history cache와 Bitget lookup 경로는 이미 `symbol`, `interval`, `range_bucket`, `source`, `result` 같은 낮은 카디널리티 개념을 사용한다.
- frontend에는 이미 Sentry 설정과 `/monitoring` tunnel route가 있다.
- release docs는 smoke-test 결과와 post-release risk 기록을 요구하지만, 구체 dashboard 이름과 알림 기준은 아직 구현으로 고정되어 있지 않다.

이 메모는 현재 저장소 상태를 설명한다.
관측성 구현이 실제로 추가되면 함께 갱신한다.
