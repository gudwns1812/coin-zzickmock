# Implementation Plan: SSE Subscription Topology

## 목적 / 큰 그림

트레이딩 상세 화면의 market realtime 경로를 `MarketStreamSessionKey(memberId, clientKey)` 기반 unified market SSE로 구현한다. 완료 후 사용자는 한 화면에서 active symbol과 open-position symbols의 market summary, active interval candle을 하나의 market SSE로 받고, non-selected symbol position도 실제 live mark price로 PnL/ROE가 갱신된다.

## 원칙

- 새 dependency 없이 기존 Spring/Next.js 구조를 사용한다.
- `SseEmitter`는 `web` 또는 `common/web`에만 둔다.
- application/domain은 SSE, stream, subscription 같은 delivery 개념이 아니라 `PositionOpenedEvent`, `PositionFullyClosedEvent` 같은 business-semantic event만 발행한다.
- 기존 raw market/candle stream endpoint contract는 first pass에서 유지한다.
- 구현은 test-first에 가깝게 작은 slice로 진행하고, 각 slice 후 targeted test를 실행한다.


## 진행 현황

- [x] ralplan consensus 완료
- [x] implementation plan 초안 작성
- [ ] 사용자 승인 또는 실행 모드 handoff
- [ ] 구현
- [ ] 테스트
- [ ] Docker backend rebuild 검증
- [ ] Browser actual-data runtime 검증
- [ ] review 스킬 기반 검토 확인
- [ ] 작업 종료 처리(완료 판단 및 completed 이동)

## 문서 원문 대조표

| 작업 영역 | 반드시 읽은 원문 문서 | 적용해야 하는 규칙 | 구현 선택 | 금지한 shortcut | 검증 방법 |
| --- | --- | --- | --- | --- | --- |
| backend web/SSE | `BACKEND.md`, `docs/design-docs/backend-design/README.md`, `01-architecture-foundations.md`, `02-package-and-wiring.md`, `03-application-and-providers.md` | HTTP/SSE delivery는 `web`, shared lifecycle은 `common/web` 우선, `SseEmitter`는 web/common-web에만 위치 | unified market stream은 `MarketStreamSessionKey(memberId, clientKey)` session registry + broker로 구현 | broker별 ad-hoc map/semaphore 난립, raw member/client telemetry label | broker/controller tests, `architectureLint`, `SseEmitter` import check |
| backend application events | `03-application-and-providers.md` | application/domain은 HTTP/SSE를 모르고 business-semantic event만 발행 | `FilledOpenOrderApplier`, `PositionCloseFinalizer`에서 after-commit event 발행 | SSE subscription event를 application event로 명명, rollback path에서 reason mutation | after-commit event tests, mutation failure tests |
| frontend route/state | `FRONTEND.md`, `frontend/README.md`, `docs/design-docs/ui-design/README.md` | 새 server state는 React Query 우선, `response.ok` 없는 json 금지, 핵심 흐름 runtime 검증 | detail page는 unified market EventSource 하나 + order SSE 별도, chart candle은 parent unified stream에서 수신 | `FuturesPriceChart`가 detail path에서 별도 candle EventSource 유지, Zustand에 market server state 복제 | frontend tests, browser Network/EventSource/console 검증 |
| execution plan | `docs/exec-plans/README.md`, `docs/exec-plans/plan-template.md` | active 계획은 진행 현황과 검증 증거를 계속 갱신 | `docs/exec-plans/active/sse-subscription-topology-implementation.md`를 실행 기준으로 사용 | 구현 후 계획 문서 방치 | 종료 전 진행 현황/증거 업데이트 |

대조표 점검:

- [x] 작업 영역별 원문 문서를 실제로 읽었다.
- [x] 제품/DB 문서와 설계 문서의 책임 차이를 구분했다.
- [x] 주변 코드 패턴이 원문 문서와 충돌할 때 설계 문서를 우선한다.
- [x] 금지한 shortcut을 구현 단계와 리뷰 단계에서 다시 확인할 수 있다.

## 구현 순서

### 0. Baseline 고정

1. 현재 SSE 관련 테스트를 먼저 실행해 baseline을 잡는다.
   - `cd backend && ./gradlew test --tests '*Sse*' --console=plain`
   - `npm test --workspace frontend -- --test-name-pattern=sse` 또는 현재 test runner가 지원하지 않으면 `npm test --workspace frontend` 내 관련 실패를 확인한다.
2. 현재 raw endpoint contract를 source/test로 고정한다.
   - `MarketControllerTest`
   - `MarketRealtimeSseBrokerTest`
   - `MarketCandleRealtimeSseBrokerTest`
   - `frontend/lib/sse-tab-return-reconnect-source.test.ts`

### 1. Backend market stream session registry 기반 추가

예상 파일:

- `backend/src/main/java/coin/coinzzickmock/common/web/...`
- 또는 `backend/src/main/java/coin/coinzzickmock/feature/market/web/...`
- `backend/src/test/java/coin/coinzzickmock/common/web/...`
- `backend/src/test/java/coin/coinzzickmock/feature/market/web/...`

작업:

1. `MarketStreamSessionKey(memberId, clientKey)`, `MarketStreamSession`, `CandleSubscription(symbol, interval)`, `SummarySubscriptionReason` 값을 정의한다.
2. `MarketStreamSession`을 source of truth로 둔다. `summaryIndex`와 `candleIndex`는 fan-out용 derived index이며 외부에서 직접 수정할 수 없게 private으로 둔다.
3. summary reason은 `ACTIVE_SYMBOL`, `OPEN_POSITION`만 둔다. `CANDLE_INTERVAL` reason은 만들지 않는다.
4. candle은 session의 `CandleSubscription(symbol, interval)` 단일 상태로 관리하고 `replaceCandleSubscription`으로만 교체한다.
5. registry public API를 명확히 둔다: `registerSession`, `releaseSession`, `addSummaryReason`, `removeSummaryReason`, `replaceCandleSubscription`, `sessionsForSummary`, `sessionsForCandle`.
6. registry 책임은 등록, subscription 변경, fan-out 조회, 해제에 한정한다.
7. 모든 subscription mutation은 registry API를 통해서만 수행한다. broker/controller/listener가 index를 직접 수정하지 않는다.
8. `memberId`, `symbol`, `CandleSubscription`, summary reason, derived index cleanup은 `releaseSession`/replacement 해제 경로에서 보장한다.
9. registry는 emitter lifecycle callback을 연결하지 않고, send를 수행하지 않으며, send failure를 직접 해석하지 않는다. lifecycle/send failure cleanup orchestration은 broker 책임이다.
10. active-session capacity와 subscription-index bound rejection은 broker/admission policy 경계에서 처리하고 telemetry를 남긴다.
11. raw `memberId`, `clientKey`, `sessionKey`를 telemetry label/log identifier로 남기지 않는다. member는 fingerprint만 허용한다.

테스트:

- 같은 `(memberId, clientKey)`만 replace된다.
- 다른 member의 같은 `clientKey`는 replace되지 않는다.
- `releaseSession`과 replacement가 source session과 모든 derived index를 제거한다.
- 외부 class가 `summaryIndex`/`candleIndex`를 직접 변경할 수 없고 registry API를 통해서만 변경된다.
- `replaceCandleSubscription`은 기존 candle index를 제거하고 새 candle index만 등록한다.
- registry test는 등록/구독 변경/조회/해제 책임을 넘어 lifecycle callback 또는 send failure cleanup을 검증하지 않는다.

### 2. Unified market stream broker + envelope

예상 파일:

- `backend/src/main/java/coin/coinzzickmock/feature/market/web/MarketStreamBroker.java`
- `backend/src/main/java/coin/coinzzickmock/feature/market/web/MarketStreamRegistry.java`
- `backend/src/main/java/coin/coinzzickmock/feature/market/web/MarketStreamSession.java`
- `backend/src/main/java/coin/coinzzickmock/feature/market/web/MarketStreamSessionKey.java`
- `backend/src/main/java/coin/coinzzickmock/feature/market/web/CandleSubscription.java`
- `backend/src/main/java/coin/coinzzickmock/feature/market/web/MarketStreamEventResponse.java`
- `backend/src/main/java/coin/coinzzickmock/feature/market/web/MarketStreamEventType.java`
- 관련 test files

작업:

1. `MARKET_SUMMARY`, `MARKET_CANDLE`, `MARKET_HISTORY_FINALIZED` envelope DTO를 추가한다.
   - 모든 envelope는 `serverTime`과 `source`(`INITIAL_SNAPSHOT` 또는 `LIVE`)를 포함한다.
2. `MarketSummaryUpdatedEvent` fan-out은 registry의 `sessionsForSummary(symbol)` snapshot으로만 수행한다.
3. `MarketCandleUpdatedEvent` / `MarketHistoryFinalizedEvent` fan-out은 registry의 `sessionsForCandle(new CandleSubscription(symbol, interval))` snapshot으로만 수행한다.
4. summary reason state(`ACTIVE_SYMBOL`, `OPEN_POSITION`) add/remove를 idempotent하게 만든다.
5. candle state는 reason set이 아니라 `CandleSubscription` 단일 값으로 유지한다.
6. first pass에서는 unified endpoint와 raw endpoint registry를 logical separation한다. 기존 `MarketRealtimeSseBroker`, `MarketCandleRealtimeSseBroker`는 raw compatibility route용으로 유지하고 unified `MarketStreamBroker`와 registry를 공유하지 않는다.
7. broker가 emitter lifecycle callback을 연결하고 completion/timeout/error에서 `releaseSession`을 호출한다.
8. broker가 initial/live send failure를 감지해 `releaseSession`, emitter complete, send-failure telemetry를 수행한다.
9. broker/admission policy가 active-session capacity와 subscription-index bound rejection을 처리한다.

테스트:

- active BTC + open ETH summary가 한 emitter로 수신된다.
- BTC 1m candle은 session의 `CandleSubscription(BTCUSDT, 1m)`에만 도착한다.
- `MARKET_HISTORY_FINALIZED`는 interval matching 시에만 도착한다.
- envelope metadata(`kind`/`type`, `symbol`, applicable `interval`, `serverTime`, `source`)가 항상 포함된다.
- send failure와 lifecycle callback이 broker 경계에서 cleanup과 telemetry로 이어진다.

### 3. Backend unified endpoint + initial snapshot

예상 파일:

- `backend/src/main/java/coin/coinzzickmock/feature/market/web/MarketController.java`
- `backend/src/main/java/coin/coinzzickmock/feature/position/application/.../OpenPositionSymbolsReader.java` 또는 동등한 얇은 query/port
- `backend/src/test/java/coin/coinzzickmock/feature/market/web/MarketControllerTest.java`

작업:

1. `GET /api/futures/markets/stream?symbol=...&interval=...&clientKey=...`를 추가한다.
2. `Providers.auth().currentActor()`로 `memberId`를 resolve한다.
3. active symbol + authenticated open-position symbols를 derive한다. 이때 `MarketController`는 position application service/repository를 직접 알지 않고 `OpenPositionSymbolsReader` 같은 collaborator에 `memberId`만 넘겨 open symbol 목록을 요청한다.
4. initial `MARKET_SUMMARY` envelope를 active/open-position symbols 각각에 보낸다.
5. active `symbol + interval` latest candle이 있으면 initial `MARKET_CANDLE` envelope를 보낸다.
6. 초기 연결 순서는 broker가 `registerSession`으로 source session/index 등록 → broker가 lifecycle callback 연결 → `source=INITIAL_SNAPSHOT` initial snapshot send → 실패 시 broker가 `releaseSession`이다. Initial send 전에 등록을 미루지 않는다.
7. initial send 실패 시 broker가 `releaseSession`으로 source session과 derived index를 release한다.
8. 등록 후 initial snapshot send 중 live event가 동시에 도착할 수 있다. MVP에서는 중복/역전을 허용하되 envelope의 `serverTime`, `source`, natural key(`symbol` 또는 `symbol+interval+openTime`)로 frontend가 idempotent upsert한다.

금지:

- `web`에서 repository 직접 호출.
- application service가 다른 application service를 주입.
- market web/controller가 position 내부 query 모델을 직접 아는 것.
- old endpoint response를 envelope로 바꾸기.

### 4. Position mutation business events

예상 파일:

- `backend/src/main/java/coin/coinzzickmock/feature/position/application/event/...`
- `backend/src/main/java/coin/coinzzickmock/feature/order/application/service/FilledOpenOrderApplier.java`
- `backend/src/main/java/coin/coinzzickmock/feature/position/application/close/PositionCloseFinalizer.java`
- 관련 tests

작업:

1. event payload에는 최소 `memberId`, `symbol`, mutation kind/reason을 포함한다.
2. business event 이름은 `PositionOpenedEvent`, `PositionFullyClosedEvent`처럼 도메인 의미만 담는다. `Sse`, `Stream`, `Subscription`, `Registry` 같은 delivery/기술 용어를 넣지 않는다.
3. `FilledOpenOrderApplier`는 successful new-position creation 후에만 after-commit `PositionOpenedEvent`를 발행한다.
4. existing position increase는 full open과 구분한다. 별도 event는 발행하지 않거나, add path가 호출되더라도 `OPEN_POSITION` reason add가 idempotent라 duplicate reason을 만들지 않아야 한다.
5. `PositionCloseFinalizer`는 successful full-close delete 후에만 after-commit `PositionFullyClosedEvent`를 발행한다.
6. partial close는 `PositionFullyClosedEvent`를 발행하지 않고 `OPEN_POSITION` reason removal을 금지한다. full close일 때만 remove한다.
7. rollback/failure path에서는 summary reason이 바뀌면 안 된다.
8. market `web` listener가 business event를 받아 active `memberId -> sessionKeys`에 summary reason update를 적용한다.

### 5. Frontend proxy + envelope parser

예상 파일:

- `frontend/app/api/futures/markets/stream/route.ts`
- `frontend/lib` 또는 `frontend/components/futures` 아래 parser module
- 관련 frontend tests

작업:

1. `/api/futures/markets/stream` route handler를 추가한다.
2. `symbol`, `interval`, `clientKey`를 검증하고 backend unified endpoint로 forward한다.
3. `response.ok` 없이 `res.json()`을 호출하지 않는 기존 규칙을 유지한다.
4. envelope parser/type guard를 추가해 malformed event를 무시하되 stream은 유지한다.
5. `serverTime`, `source`, `kind/type`, natural key를 parsing해 initial/live 중복 또는 순서 역전 시 최신 값만 남기도록 한다.

### 6. Frontend trading detail 통합

예상 파일:

- `frontend/components/futures/MarketDetailRealtimeView.tsx`
- `frontend/components/futures/FuturesPriceChart.tsx`
- `frontend/components/futures/livePositionDisplay.ts`
- related tests

작업:

1. `MarketDetailRealtimeView`가 exactly one unified market EventSource를 열도록 바꾼다.
2. selected market display는 selected-symbol `MARKET_SUMMARY`로 갱신한다.
3. `marketSnapshotsBySymbol`을 유지하고 positions는 matching symbol snapshot으로 mark-to-market한다.
4. `FuturesPriceChart`는 detail page에서 별도 candle EventSource를 열지 않고 parent가 받은 `MARKET_CANDLE` / `MARKET_HISTORY_FINALIZED`를 props 또는 shared state로 받는다.
5. order/trading EventSource는 별도로 유지한다.
6. 사용자가 `symbol` 또는 `interval`을 변경하면 first pass에서는 기존 unified EventSource를 닫고 새 unified EventSource를 연다. 별도 HTTP replace API를 만들거나 호출하지 않는다.

### 7. Compatibility와 landing page 정리

1. old raw summary/candle endpoints가 기존 tests를 계속 통과하는지 확인한다.
2. first pass에서 raw endpoint는 기존 raw brokers/registries를 사용하고 unified endpoint는 `MarketStreamRegistry`를 사용한다. 두 경로는 logical separation을 유지한다.
2. `MarketsLandingRealtimeView`는 이번 acceptance blocker가 아니므로, 필요하면 기존 per-symbol stream을 유지한다.
3. 별도 후속 작업으로 landing page stream consolidation 여부를 기록한다.

## 구현 완료 전 검증 순서

1. Backend targeted tests.
2. Frontend targeted tests.
3. Full static verification.
4. Docker backend rebuild + health check.
5. Browser runtime verification with actual backend data.
6. 종료조건 체크리스트 충족 확인.

## 종료조건 / Done Definition

작업은 아래가 모두 사실일 때만 완료로 보고한다.

- [ ] Backend targeted tests가 통과했다.
- [ ] Frontend targeted tests가 통과했다.
- [ ] `cd backend && ./gradlew architectureLint --console=plain` 통과.
- [ ] `cd backend && ./gradlew check --console=plain` 통과.
- [ ] `npm test --workspace frontend` 통과.
- [ ] `npm run lint` 통과.
- [ ] `npm run build` 통과.
- [ ] `NGINX_PORT=18080 docker compose up --build -d mysql redis backend nginx`로 backend image를 다시 빌드하고 container health가 `UP`이다.
- [ ] `curl -fsS http://127.0.0.1:18080/actuator/health`가 `UP`을 반환한다.
- [ ] `FUTURES_API_BASE_URL=http://127.0.0.1:18080 npm run dev --workspace frontend -- --port 3100`로 현재 코드 기준 frontend를 띄웠다.
- [ ] Browser에서 `http://127.0.0.1:3100/markets/BTCUSDT`를 열고 실제 backend market data가 화면에 표시된다.
- [ ] Browser Network/EventSource 관찰에서 detail page market stream은 하나이고 order stream은 별도임을 확인했다.
- [ ] BTC active + ETH open position fixture 또는 실제 계정 상태에서 ETH position card의 mark price/PnL/ROE가 ETH live summary 수신 후 갱신됨을 확인했다.
- [ ] Console error와 failed network request가 없다. 있다면 원인을 기록하고 수정했거나, product acceptance와 무관한 known external issue로 명시했다.
- [ ] 구현 결과와 검증 증거를 계획 문서에 업데이트했다.

위 항목 중 하나라도 미충족이면 종료하지 않고 수정/재검증한다. Docker 또는 브라우저 검증이 환경 문제로 불가능하면, 실패 이유·대체 검증·남은 위험을 명시하고 완료가 아니라 blocked/partial로 보고한다.

## 놀라움과 발견

- frontend `.env.development`는 `FUTURES_API_BASE_URL=http://127.0.0.1:18080`을 기본으로 사용한다. Docker runtime 검증은 `NGINX_PORT=18080`으로 compose nginx를 띄우는 방식이 현재 local frontend 설정과 맞다.

## 의사결정 기록

- Docker backend 검증은 `docker compose up --build -d mysql redis backend nginx`처럼 backend image rebuild를 포함해야 한다. 단순 `bootRun`만으로는 종료조건을 만족하지 않는다.
- Browser 검증은 mock이 아니라 Docker backend actual market data를 대상으로 한다.

## 결과 및 회고

- 구현 전 계획 단계이므로 아직 없음. 구현 후 검증 로그와 남은 위험을 이 섹션에 갱신한다.

## 네이밍 점검

`docs/design-docs/backend-design/10-technical-naming-rules.md` 기준으로 신규 클래스 이름에는 불필요한 기술명을 넣지 않는다. 계획 초안의 `MarketStreamSseBroker`는 기술명을 드러내므로 `MarketStreamBroker`로 보정한다. `SseEmitter`처럼 framework/protocol type 자체를 언급해야 하는 문맥은 경계 설명에만 남기고, 신규 역할 클래스명은 `MarketStreamRegistry`, `MarketStreamSession`, `CandleSubscription`, `PositionOpenedEvent`, `PositionFullyClosedEvent`처럼 역할/도메인 의미를 우선한다. 기존 코드에 이미 있는 `SseSubscriptionRegistry`, `MarketRealtimeSseBroker`, `MarketCandleRealtimeSseBroker`는 compatibility 유지 대상의 현재 이름으로만 참조한다.
