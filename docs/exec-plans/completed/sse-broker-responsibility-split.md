# SSE Broker Responsibility Split Plan

이 계획서는 `docs/exec-plans/README.md`와 `docs/exec-plans/plan-template.md`를 따른다.
`$oh-my-codex:ralplan` consensus 결과이며, 구현 전 책임 현황과 분리 방향을 고정한다.

## Implementation Reference Documents

- `BACKEND.md` — 백엔드 작업 입구 문서이며 `web/common-web` SSE delivery, 책임 분리, 검증 명령 기준을 고정한다. read by: `ralph` executor, `team` backend executor/verifier.
- `docs/design-docs/backend-design/README.md` — backend 상세 설계 색인과 문서 책임 분리 규칙을 확인한다. read by: executor/verifier.
- `docs/design-docs/backend-design/01-architecture-foundations.md` — `web/job/application/domain/infrastructure` 고정 레이어와 HTTP/SSE delivery 소유권을 확인한다. read by: executor/verifier.
- `docs/design-docs/backend-design/02-package-and-wiring.md` — `common/web` SSE lifecycle, `SseSubscriptionRegistry`, bean wiring, concrete class 우선 규칙을 확인한다. read by: executor.
- `docs/design-docs/backend-design/03-application-and-providers.md` — application/provider 경계, application service dependency 금지, SSE 개념 내향 누수 금지를 확인한다. read by: candle interval collaborator 담당 executor.
- `docs/design-docs/backend-design/07-clean-code-responsibility.md` — cleanup/refactor 계획, 회귀 테스트 우선, 클래스/메서드 책임 분리 기준을 확인한다. read by: executor/verifier.
- `docs/design-docs/backend-design/05-testing-and-lint.md` — backend test layer와 `architectureLint` 검증 계약을 확인한다. read by: verifier.
- `docs/exec-plans/README.md` — active/completed 계획 lifecycle을 확인한다. read by: planner/executor.
- `docs/exec-plans/active/sse-subscription-topology-implementation.md` — 기존 SSE topology 결정과 unified stream 맥락을 대조한다. read by: executor before changing market SSE topology.

## 목적 / 큰 그림

현재 backend SSE broker들은 stream별 동작을 유지하면서도 lifecycle, send failure, telemetry, capacity 처리 코드가 반복되고, 특히 `MarketCandleRealtimeSseBroker`는 SSE fan-out과 repository-backed candle finalization 계산을 함께 소유한다.

목표는 broker를 하나의 generic framework로 합치지 않고, stream-specific orchestration은 `web` broker에 남기되 반복되는 SSE mechanics와 candle finalization 계산 경계를 작고 검증 가능한 단위로 분리하는 것이다.

## 진행 현황

- [x] 관련 backend 설계 문서 확인
- [x] SSE broker 파일 inventory 확인
- [x] 현재 책임과 혼재 지점 정리
- [x] baseline targeted SSE test 실행: `cd backend && ./gradlew test --tests '*Sse*' --console=plain`
- [x] Architect review: `ITERATE`, 추상화/경계 구체화 요구
- [x] Critic review: `APPROVE`, minor recommendation 반영
- [x] 실행 계획 문서 작성
- [x] 사용자 승인 또는 후속 실행 mode handoff (`$oh-my-codex:ralph` 실행 요청)
- [x] 구현
- [x] 테스트
- [x] review 스킬 기반 검토 확인 (`Ralph architect verification: APPROVE`)
- [x] 작업 종료 처리(완료 판단 및 completed 이동)

## RALPLAN-DR Summary

### Principles

1. Broker는 stream-specific `web` orchestrator로 남긴다. 단일 generic SSE framework를 만들지 않는다.
2. 공통화는 네 broker에 실제로 반복되는 lifecycle, capacity, send failure, telemetry policy에 한정한다.
3. `application/domain`에는 `SseEmitter`, SSE event name, envelope DTO, subscription 개념을 들이지 않는다.
4. raw endpoint와 unified endpoint 동작은 refactor 전에 characterization test로 고정한다.
5. 새 abstraction보다 기존 `common/web` utility 재사용과 작은 concrete collaborator를 우선한다.

### Decision Drivers

1. Stream 의미를 숨기지 않으면서 lifecycle/fan-out 실패 처리 중복을 줄인다.
2. `MarketCandleRealtimeSseBroker`의 repository-backed interval 계산 책임을 broker에서 분리하거나 명시적으로 예외 처리한다.
3. raw summary, raw candle, unified market, trading execution stream 호환성 위험을 낮춘다.

### Viable Options

#### Option A — 점진적 책임 분리(선택)

계약 고정 테스트를 먼저 추가/확인하고, 증명된 반복 mechanics만 작게 추출한다. Broker는 유지한다. Candle affected-interval 계산은 SSE와 무관한 이름 있는 collaborator 뒤로 이동한다. Raw summary registry migration은 동작 보존 테스트가 가능할 때만 진행한다.

- Pros: 낮은 regression 위험, 설계 문서와 정합, 작은 PR slice 가능.
- Cons: 일부 중복이 일시적으로 남는다.

#### Option B — 전체 broker 통합

시장/거래 모든 SSE stream을 하나의 generic broker/registry로 통합한다.

- Pros: 중복 제거 폭이 가장 크다.
- Cons: raw endpoint contract, stream별 key/reason/initial snapshot semantics가 숨겨지고 over-abstraction 위험이 크다.
- Rejected: 현재 요청 범위와 호환성 기준에 비해 위험이 크다.

#### Option C — 문서화만 하고 refactor 없음

현 상태 책임을 문서화하고 코드는 유지한다.

- Pros: 코드 변경 위험이 없다.
- Cons: candle broker의 혼재 책임과 lifecycle 중복이 계속 커진다.
- Rejected: 사용자가 책임 분리 계획을 요청했고 현재 혼재가 이미 확인되었다.

## 현재 책임 Inventory

### 공통 SSE mechanics

- `backend/src/main/java/coin/coinzzickmock/common/web/SseSubscriptionRegistry.java`
  - per-key + `clientKey` reservation/register/unregister/release, fair semaphore capacity, subscriber snapshot을 담당한다.
  - 금지 책임: send, lifecycle callback binding, telemetry stream label, payload semantics.
- `backend/src/main/java/coin/coinzzickmock/common/web/SseEmitterLifecycle.java`
  - `SseEmitter` completion/timeout/error callback binding과 silent completion만 담당한다.
- `backend/src/main/java/coin/coinzzickmock/common/web/SseDeliveryExecutor.java`
  - SSE async delivery executor boundary만 담당한다.
- `backend/src/main/java/coin/coinzzickmock/common/web/SseClientKey.java`
  - optional `clientKey` 정규화와 fallback 생성만 담당한다.

### Market raw summary stream

- `backend/src/main/java/coin/coinzzickmock/feature/market/web/MarketRealtimeSseBroker.java`
  - raw market summary stream orchestration을 담당한다.
  - 책임: multi-symbol reserve/register, lifecycle binding, replaced emitter completion, live `MarketSummaryUpdatedEvent` fan-out, stream telemetry, rejection reason mapping.
- `backend/src/main/java/coin/coinzzickmock/feature/market/web/MarketSummarySubscriptionRegistry.java`
  - raw summary stream의 multi-symbol client subscription state와 per-symbol/total capacity를 담당한다.
  - 특이점: `SseSubscriptionRegistry`와 달리 한 client가 여러 symbol에 걸쳐 있고, replacement 시 acquired-symbol capacity release semantics가 있다.

### Market raw candle stream

- `backend/src/main/java/coin/coinzzickmock/feature/market/web/MarketCandleRealtimeSseBroker.java`
  - raw `(symbol, interval)` candle stream orchestration을 담당한다.
  - 현재 과다 책임: `MarketHistoryRepository`를 직접 사용해 `historyFinalized` affected intervals를 계산한다.
  - 유지 책임: subscription key lifecycle, live candle fan-out, response 생성, send failure cleanup, telemetry.

### Unified market stream

- `backend/src/main/java/coin/coinzzickmock/feature/market/web/MarketStreamRegistry.java`
  - unified market session source-of-truth와 member/summary/candle derived index를 담당한다.
  - 금지 책임: send, lifecycle callback binding, SSE event payload 생성.
- `backend/src/main/java/coin/coinzzickmock/feature/market/web/MarketStreamSession.java`
  - 한 unified session의 active symbol, open-position summary reasons, candle subscription state를 담당한다.
- `backend/src/main/java/coin/coinzzickmock/feature/market/web/MarketStreamBroker.java`
  - unified market stream orchestration을 담당한다.
  - 책임: session open/release, initial summary/candle snapshot send, summary/candle/history-finalized envelope fan-out, position reason add/remove, send failure cleanup, telemetry.
- `backend/src/main/java/coin/coinzzickmock/feature/market/web/UnifiedMarketStreamStrategy.java`
  - authenticated actor와 open position symbols를 조회해 unified stream open input을 만든다.
- `backend/src/main/java/coin/coinzzickmock/feature/market/web/MarketStreamPositionEventListener.java`
  - `PositionOpenedEvent`/`PositionFullyClosedEvent`를 unified stream summary reason update로 연결한다.

### Trading execution stream

- `backend/src/main/java/coin/coinzzickmock/feature/order/web/TradingExecutionSseBroker.java`
  - member-keyed trading execution SSE stream을 담당한다.
  - 책임: member subscription lifecycle, `TradingExecutionEvent` AFTER_COMMIT fan-out, send failure cleanup, telemetry.

## 문서 원문 대조표

| 작업 영역 | 반드시 읽은 원문 문서 | 적용해야 하는 규칙 | 구현 선택 | 금지한 shortcut | 검증 방법 |
| --- | --- | --- | --- | --- | --- |
| backend web/SSE | `BACKEND.md`, `01-architecture-foundations.md`, `02-package-and-wiring.md` | HTTP/SSE delivery는 `web`, shared lifecycle은 `common/web`, `SseEmitter`는 `web/common-web`에만 위치 | Broker는 stream별로 유지하고 공통 mechanics만 `common/web`로 제한 | generic broker framework, feature 밖 업무 패키지, `SseEmitter` 내향 누수 | `*Sse*` tests, static grep, `architectureLint` |
| application boundary | `03-application-and-providers.md`, `07-clean-code-responsibility.md` | application은 SSE delivery를 모르고 business/query semantics만 소유 | candle interval visibility logic은 non-SSE query collaborator 후보로 분리 | application에 envelope/event name/subscription 용어 추가 | application/domain grep, collaborator unit test |
| cleanup/refactor | `07-clean-code-responsibility.md` | 회귀 테스트 우선, 작고 이름 있는 책임으로 분리 | characterization → mechanics extraction → candle split 순서 | 추상화 먼저 추가, pass-through interface, `Manager/Helper/Util` | targeted/full backend tests |
| execution plan | `docs/exec-plans/README.md`, `plan-template.md`, 기존 SSE topology plan | active plan은 진행 현황과 검증 증거를 갱신 | 이 문서를 active plan으로 사용하고 기존 SSE plan과 충돌 확인 | 구현 후 계획 문서 방치 | 완료 전 plan 갱신 |

대조표 점검:

- [x] 작업 영역별 원문 문서를 실제로 읽었다.
- [x] 제품/DB 문서와 설계 문서의 책임 차이를 구분했다.
- [x] 주변 코드 패턴이 원문 문서와 충돌할 때 설계 문서를 우선한다.
- [x] 금지한 shortcut을 구현 단계와 리뷰 단계에서 다시 확인할 수 있다.

## 목표 책임 경계

1. `common/web`는 mechanics-only다.
   - 유지: `SseSubscriptionRegistry`, `SseEmitterLifecycle`, `SseDeliveryExecutor`, `SseClientKey`.
   - 새 class는 네 broker에서 반복되는 완결된 policy가 확인될 때만 추가한다.
   - 예: `SseSendOperation` 같은 concrete collaborator는 send duration/result telemetry와 failure cleanup callback을 하나의 테스트 가능한 policy로 소유할 때만 허용한다.
   - 금지: 단순 위임 interface, `Manager`, `Helper`, `Util`, stream별 business key/payload 판단.
2. Feature broker는 stream-specific decision owner다.
   - subscriber key/session 선택, event source 선택, initial snapshot 순서, payload DTO/envelope, stream label, rejection reason, cleanup reason을 broker가 결정한다.
   - public method는 stream 이야기를 보여야 한다: reserve/open/register → event listener → fan-out.
3. Candle history-finalized interval derivation은 broker에서 분리한다.
   - 재사용 가능한 market-history visibility semantics이면 `feature/market/application/query/FinalizedCandleIntervalsReader` 같은 collaborator로 이동한다.
   - 이 collaborator는 `List<MarketCandleInterval>` 또는 값 객체만 반환하고 SSE/response DTO를 모른다.
   - 순수 response shaping으로 판단되면 `web` collaborator에 둘 수 있지만 repository 직접 주입은 피하고 application query collaborator를 통해 필요한 데이터를 받는다.
   - 어느 선택이든 `MarketCandleRealtimeSseBroker`는 response 생성과 fan-out만 담당한다.
4. Raw summary registry는 조건부다.
   - 먼저 multi-symbol replacement, acquired-symbol capacity release, pending capacity cleanup, per-symbol fan-out 테스트를 확인/보강한다.
   - `SseSubscriptionRegistry`로 동일 의미를 단순하게 보존할 수 있으면 migration한다.
   - 그렇지 않으면 `MarketSummarySubscriptionRegistry`를 stream-specific registry로 유지하고 이유를 이 문서/테스트에 남긴다.

## 구체적인 단계

### 0. 기존 계획 정리

- 기존 `docs/exec-plans/active/sse-subscription-topology-implementation.md`가 이미 완료 상태를 많이 포함하므로, 이번 책임 분리와 충돌하지 않는지 확인한다.
- 이번 작업은 `docs/exec-plans/active/sse-broker-responsibility-split.md`를 기준 active plan으로 사용한다.

### 1. Baseline과 characterization test 고정

- 실행:
  - `cd backend && ./gradlew test --tests '*Sse*' --console=plain`
- 보강 후보:
  - raw summary: multi-symbol replacement, capacity release, pending cleanup, per-symbol fan-out.
  - raw candle: `historyFinalized` affected intervals 계산과 fan-out 대상.
  - unified market: envelope kind/type/source/serverTime, summary/candle/history finalized targeting.
  - trading execution: AFTER_COMMIT fan-out와 rollback/no-event 경로.
- URL/payload contract assertions를 endpoint/controller 또는 broker test에 명시한다.

### 2. 반복 mechanics만 안전하게 추출

- 네 broker의 send/lifecycle/telemetry/rejection block을 비교한다.
- 기존 `SseEmitterLifecycle`, `SseDeliveryExecutor`, `SseSubscriptionRegistry`로 해결 가능한 중복을 먼저 줄인다.
- 새 `common/web` collaborator가 필요하면 아래 조건을 모두 만족해야 한다.
  - concrete class다.
  - 한 문장 책임이 가능하다.
  - 자체 test가 있다.
  - stream label, rejection reason, business key, payload type 의미는 caller가 제공한다.
  - 단순 pass-through가 아니다.

### 3. Candle interval derivation 분리

- `MarketCandleRealtimeSseBroker#affectedIntervals`와 그 하위 repository-backed logic을 분리한다.
- 선호안:
  - `feature/market/application/query/FinalizedCandleIntervalsReader` 또는 더 나은 역할명.
  - 입력: `symbol`, `openTime`, `closeTime`.
  - 출력: `List<MarketCandleInterval>`.
  - repository 조회와 bucket completion 판단을 담당하되 SSE response를 모른다.
- Broker는 반환된 interval 값을 `MarketCandleHistoryFinalizedResponse`로 변환하고 fan-out한다.

### 4. Raw summary registry 결정

- 테스트 결과를 기준으로 둘 중 하나를 선택한다.
  - Keep: custom multi-symbol registry가 현재 stream-specific semantics를 가장 단순하게 표현한다면 유지한다.
  - Migrate: `SseSubscriptionRegistry`로 동일 semantics를 명확히 보존할 수 있고 common registry를 복잡하게 만들지 않을 때만 이동한다.
- 선택 이유를 `의사결정 기록`에 남긴다.

### 5. 문서와 코드 정리

- 동작 변경이 없으면 product spec은 갱신하지 않는다.
- backend 설계 규칙 자체를 바꾸는 경우에만 관련 번호 문서를 갱신한다.
- 이 active plan의 진행 현황, 놀라움과 발견, 의사결정 기록, 검증 증거를 갱신한다.

### 6. 검증

- Targeted:
  - `cd backend && ./gradlew test --tests '*Sse*' --console=plain`
- Structure:
  - `cd backend && ./gradlew architectureLint --console=plain`
- Full backend after production code changes:
  - `cd backend && ./gradlew check --console=plain`
- Static boundary checks:
  - `rg -n "SseEmitter" backend/src/main/java/coin/coinzzickmock | grep -v '/web/'`는 의도치 않은 내향 누수를 보여야 한다.
  - `rg -n "Sse|SseEmitter|Subscription|MarketStreamEventResponse|MarketStreamEventType" backend/src/main/java/coin/coinzzickmock/feature/*/{application,domain}`로 application/domain의 SSE/envelope 개념 누수를 확인한다. 단, 비-SSE business `Subscription` 용어가 있으면 문맥으로 판정한다.

## 수용 기준(테스트 가능한 형태)

- raw endpoint URL/payload behavior가 명시적 plan amendment 없이 바뀌지 않는다.
- `MarketCandleRealtimeSseBroker`가 repository-backed interval completion logic을 직접 소유하지 않는다. 예외로 유지한다면 테스트와 의사결정 기록에 이유가 있다.
- 새 `common/web` code가 있다면 concrete repeated policy를 소유하고 test가 있으며 pass-through interface/default method가 아니다.
- 각 broker는 stream-specific 결정을 유지하고 한 문장 책임으로 설명 가능하다.
- `application/domain`에는 SSE/subscription/envelope delivery 개념이 없다.
- `cd backend && ./gradlew test --tests '*Sse*' --console=plain`가 통과한다.
- `cd backend && ./gradlew architectureLint --console=plain`가 통과한다.
- production code 변경 후 `cd backend && ./gradlew check --console=plain`가 통과한다.

## 위험과 완화

- 위험: 공통화가 generic SSE framework로 커져 stream semantics를 숨긴다.
  - 예방: broker별 contract test를 먼저 고정하고, 새 common class는 concrete policy 하나만 소유하게 한다.
  - 완화: abstraction이 pass-through로 보이면 되돌리고 broker-local 중복을 허용한다.
  - 복구: 해당 extraction commit/slice를 revert하고 characterization tests를 유지한다.
- 위험: candle interval logic을 application으로 옮기며 SSE envelope 개념이 내향 누수된다.
  - 예방: collaborator API를 `MarketCandleInterval` 중심으로 설계하고 SSE/response DTO import를 금지한다.
  - 완화: web response assembly는 broker/web collaborator에 남긴다.
  - 복구: application collaborator를 query-only로 축소하거나 web collaborator + application data reader로 재배치한다.
- 위험: raw summary registry migration이 multi-symbol capacity/replacement 의미를 깨뜨린다.
  - 예방: migration 전 compatibility tests를 추가한다.
  - 완화: migration 조건을 만족하지 않으면 custom registry를 유지한다.
  - 복구: custom registry로 되돌리고 유지 이유를 기록한다.

## 검증 증거

- 2026-05-12 continuation verification:
  - Code review focus pass: blocking findings 없음. 변경은 `MarketCandleRealtimeSseBroker`의 repository-backed interval 계산을 `FinalizedCandleIntervalsReader` application query collaborator로 분리하며, broker에는 SSE response assembly/fan-out만 남겼다.
  - `cd backend && ./gradlew test --tests '*Sse*' --tests '*FinalizedCandleIntervalsReaderTest' --console=plain`
  - Result: `BUILD SUCCESSFUL in 2s`, `5 actionable tasks: 1 executed, 4 up-to-date`.
  - `cd backend && ./gradlew architectureLint --console=plain`
  - Result: `BUILD SUCCESSFUL in 392ms`, `1 actionable task: 1 up-to-date`.
  - `cd backend && ./gradlew check --console=plain`
  - Result: `BUILD SUCCESSFUL in 34s`, `6 actionable tasks: 1 executed, 5 up-to-date`.
  - Static boundary grep: `SseEmitter` outside `web/common-web` 없음; `application/domain`의 `Sse`, `SseEmitter`, `MarketStreamEventResponse`, `MarketStreamEventType` 누수 없음; `Subscription` mention 없음.

- 2026-05-12 local baseline:
  - `cd backend && ./gradlew test --tests '*Sse*' --console=plain`
  - Result: `BUILD SUCCESSFUL in 2s`, `5 actionable tasks: 1 executed, 4 up-to-date`.
- 2026-05-12 Ralph focused regression:
  - `cd backend && ./gradlew test --tests 'coin.coinzzickmock.feature.market.application.query.FinalizedCandleIntervalsReaderTest' --tests 'coin.coinzzickmock.feature.market.web.MarketCandleRealtimeSseBrokerTest' --console=plain`
  - Result: `BUILD SUCCESSFUL in 2s`, `5 actionable tasks: 3 executed, 2 up-to-date`.
- 2026-05-12 Ralph targeted SSE regression:
  - `cd backend && ./gradlew test --tests '*Sse*' --console=plain`
  - Result: `BUILD SUCCESSFUL in 2s`, `5 actionable tasks: 1 executed, 4 up-to-date`.
- 2026-05-12 Ralph static boundary checks:
  - `rg -n "SseEmitter" backend/src/main/java/coin/coinzzickmock | grep -v '/web/'`
  - Result: no matches.
  - `rg -n "Sse|SseEmitter|Subscription|MarketStreamEventResponse|MarketStreamEventType" backend/src/main/java/coin/coinzzickmock/feature/*/{application,domain}`
  - Result: no matches.
  - `rg -n "MarketHistoryRepository|findCompletedHourlyCandles|BucketRange|MarketTime" backend/src/main/java/coin/coinzzickmock/feature/market/web/MarketCandleRealtimeSseBroker.java`
  - Result: no matches.
- 2026-05-12 Ralph architecture lint:
  - `cd backend && ./gradlew architectureLint --console=plain`
  - Result: `BUILD SUCCESSFUL`, `violations=0`, `advisories=0`.
- 2026-05-12 Ralph full backend check:
  - `cd backend && ./gradlew check --console=plain`
  - Result: `BUILD SUCCESSFUL in 29s`, `6 actionable tasks: 1 executed, 5 up-to-date`.
- 2026-05-12 Ralph deslop pass:
  - Scope: changed files only.
  - Behavior lock: focused reader/broker tests and `*Sse*` tests were already green.
  - Finding: removed the public test-convenience `withoutHistoricalVisibility` path from `FinalizedCandleIntervalsReader`; tests now wire an explicit repository-backed reader.
  - Fallback findings: no fallback-like code remained after cleanup.
- 2026-05-12 Ralph post-deslop focused regression:
  - `cd backend && ./gradlew test --tests 'coin.coinzzickmock.feature.market.application.query.FinalizedCandleIntervalsReaderTest' --tests 'coin.coinzzickmock.feature.market.web.MarketCandleRealtimeSseBrokerTest' --console=plain --rerun-tasks`
  - Result: `BUILD SUCCESSFUL in 6s`, `5 actionable tasks: 5 executed`.
- 2026-05-12 Ralph post-deslop targeted SSE regression:
  - `cd backend && ./gradlew test --tests '*Sse*' --console=plain --rerun-tasks`
  - Result: `BUILD SUCCESSFUL in 5s`, `5 actionable tasks: 5 executed`.
- 2026-05-12 Ralph post-deslop static boundary checks:
  - `rg -n "SseEmitter" backend/src/main/java/coin/coinzzickmock | grep -v '/web/'`
  - Result: no matches.
  - `rg -n "Sse|SseEmitter|Subscription|MarketStreamEventResponse|MarketStreamEventType" backend/src/main/java/coin/coinzzickmock/feature/*/{application,domain}`
  - Result: no matches.
  - `rg -n "MarketHistoryRepository|findCompletedHourlyCandles|BucketRange|MarketTime" backend/src/main/java/coin/coinzzickmock/feature/market/web/MarketCandleRealtimeSseBroker.java`
  - Result: no matches.
- 2026-05-12 Ralph post-deslop architecture lint:
  - `cd backend && ./gradlew architectureLint --console=plain --rerun-tasks`
  - Result: `BUILD SUCCESSFUL in 3s`, `violations=0`, `advisories=0`.
- 2026-05-12 Ralph post-deslop full backend check:
  - `cd backend && ./gradlew check --console=plain --rerun-tasks`
  - Result: `BUILD SUCCESSFUL in 37s`, `6 actionable tasks: 6 executed`.
- 2026-05-12 Ralph architect verification:
  - Verdict: `APPROVE`.
  - Result: no concrete file/line issues against raw SSE contract preservation, layer boundaries, abstraction scope, or test adequacy.

## Review 결과

`code-review-focus` 기준으로 변경 파일을 점검했다.

- Blocking findings: 없음.
- Consistency: `FinalizedCandleIntervalsReader`는 `feature/market/application/query`에 위치하고 SSE/web DTO를 의존하지 않는다.
- Maintainability: `MarketCandleRealtimeSseBroker`에서 bucket completion/repository 조회 책임이 제거되어 broker가 response assembly와 fan-out에 집중한다.
- Residual risk: raw summary registry migration과 common-web send mechanics extraction은 이번 continuation에서 수행하지 않았고, 계획의 conditional follow-up으로 남아 있다.

## Consensus Review 기록

### Architect review

- Verdict: `ITERATE`.
- 주요 지적:
  - broker duplication이 의도적 locality일 수 있으므로 추상화가 stream semantics를 숨기지 않아야 한다.
  - candle `historyFinalized` interval logic의 application/web 경계를 명확히 해야 한다.
  - raw summary registry migration은 multi-symbol semantics를 테스트로 보존해야 한다.
- 반영:
  - contract freeze 우선, mechanics-only common extraction, candle boundary decision, no broker collapse, conditional registry migration으로 계획을 수정했다.

### Critic review

- Verdict: `APPROVE`.
- non-blocking recommendation 반영:
  - production code 변경 후 `./gradlew check --console=plain`를 mandatory로 승격했다.
  - raw/unified/trading contract assertions를 명시했다.
  - static grep 범위를 application/domain SSE/envelope 개념 누수까지 확장했다.

## ADR

- Decision: broker 통합이 아니라 점진적 책임 분리를 선택한다.
- Drivers: 호환성, layer boundary, 실제 중복 제거, stream semantics 보존.
- Alternatives considered:
  - Full generic SSE framework: raw contract와 stream별 의미를 숨길 위험 때문에 거절.
  - No refactor/document only: 현재 mixed responsibility를 방치하므로 거절.
- Why chosen: 가장 명확한 혼재 책임을 작은 단위로 줄이면서 review와 rollback이 쉽다.
- Consequences:
  - 일부 중복은 안전성이 증명될 때까지 남는다.
  - raw summary registry convergence는 조건부 후속 결정으로 남는다.
- Follow-ups:
  - 책임 분리 후 raw market summary endpoint를 unified market stream으로 흡수할지 별도 제품/호환성 계획에서 평가한다.

### 2026-05-12 Ralph implementation ADR

- Decision: `MarketCandleRealtimeSseBroker`에서 repository-backed `historyFinalized` interval derivation을 제거하고 `feature/market/application/query/FinalizedCandleIntervalsReader`로 이동한다.
- Drivers: raw candle broker는 SSE subscription lifecycle, raw response shaping, fan-out, telemetry에 집중해야 하고, completed hourly bucket visibility는 SSE delivery와 무관한 market-history query semantics다.
- Alternatives considered:
  - Common SSE send/lifecycle collaborator 추가: 이번 diff에서 반복 policy를 새로 증명하지 못했고 generic framework로 커질 위험이 있어 보류.
  - Raw summary registry migration: multi-symbol capacity/replacement semantics가 stream-specific이므로 이번 slice에서는 유지.
- Consequences:
  - Raw SSE `historyFinalized` response type and `affectedIntervals` string contract remains in `web`.
  - Application collaborator returns only `List<MarketCandleInterval>` and imports no SSE/envelope/subscription types.

## 사용 가능한 agent types roster

- `explore`: repo-local 파일, symbol, 관계 lookup.
- `planner`: slice 순서와 plan/doc 업데이트 정리.
- `architect`: layer boundary와 abstraction 검토.
- `executor`: 구현/refactor 담당.
- `test-engineer`: characterization/contract test 설계와 보강.
- `verifier`: test/lint/static grep 증거 확인.
- `code-reviewer` 또는 `critic`: 최종 diff 품질/위험 검토.

## Follow-up staffing guidance

### `$ralph` 권장 경로

단일 executor가 순차 진행한다.

1. Reference Reading Handoff의 모든 문서를 읽는다.
2. `*Sse*` baseline과 missing contract tests를 보강한다.
3. candle interval collaborator split을 먼저 수행한다.
4. 반복 mechanics extraction은 작게 진행하고 pass-through abstraction이면 중단한다.
5. raw summary registry는 테스트가 동일 semantics를 보장할 때만 migration한다.
6. 검증 명령과 static grep 결과를 이 문서에 갱신한다.

Suggested reasoning: executor `medium`, verifier/reviewer `high`.

Launch hint:

```bash
$ralph "Implement docs/exec-plans/active/sse-broker-responsibility-split.md. Read the Implementation Reference Documents first. Preserve raw SSE contracts, split candle interval derivation safely, and verify with targeted SSE tests, architectureLint, backend check, and static boundary greps."
```

### `$team` 권장 경로

공유 파일 충돌이 있으므로 leader가 write scope를 엄격히 나눈다.

- Lane 1 — test-engineer: broker/endpoint characterization tests 보강. Write scope: `backend/src/test/java/**/web/*Sse*`, `MarketStream*Test`.
- Lane 2 — executor(common-web): 반복 mechanics extraction 후보를 구현. Write scope: `backend/src/main/java/coin/coinzzickmock/common/web`, 관련 common tests.
- Lane 3 — executor(market-candle): candle interval derivation collaborator split. Write scope: `feature/market/application/query` 또는 선택된 web collaborator + `MarketCandleRealtimeSseBroker`.
- Lane 4 — verifier: `architectureLint`, `check`, static grep, plan evidence update.

Team verification path:

1. Lane별 targeted tests.
2. 통합 `cd backend && ./gradlew test --tests '*Sse*' --console=plain`.
3. `cd backend && ./gradlew architectureLint --console=plain`.
4. `cd backend && ./gradlew check --console=plain`.
5. static grep boundary checks.
6. reviewer/critic pass.

Launch hint:

```bash
$team "Execute docs/exec-plans/active/sse-broker-responsibility-split.md with lanes for tests, common-web mechanics, market-candle interval split, and verification. Each implementation worker must read its assigned Implementation Reference Documents before editing and must not revert others' changes."
```

## Goal-Mode Follow-up Suggestions

- `$ultragoal` — 이 계획을 durable goal로 추적하며 순차 완료 상태를 남길 때 기본 추천.
- `$performance-goal` — SSE fan-out latency, executor queue, memory/connection count 같은 성능 목표가 추가될 때만 사용.
- `$autoresearch-goal` — 외부 SSE library나 Spring SSE 동작 조사 중심으로 바뀔 때만 사용.

## Reference Reading Handoff

후속 실행 lane은 구현 전에 아래를 반드시 읽는다.

- `ralph`: `Implementation Reference Documents`의 모든 문서를 읽고, 이 계획의 `목표 책임 경계`, `수용 기준`, `검증 절차`를 실행 기준으로 삼는다.
- `team`: leader는 각 worker prompt/inbox에 관련 subset을 넣는다.
  - test lane: `BACKEND.md`, `05-testing-and-lint.md`, 이 계획서.
  - common-web lane: `BACKEND.md`, `01`, `02`, `07`, 이 계획서.
  - market-candle lane: `BACKEND.md`, `01`, `03`, `07`, 기존 SSE topology plan, 이 계획서.
  - verifier lane: `BACKEND.md`, `05`, `docs/exec-plans/README.md`, 이 계획서.

## 산출물과 메모

- Context snapshot: `.omx/context/sse-broker-responsibility-split-20260512T023910Z.md`.
- Code artifact: `backend/src/main/java/coin/coinzzickmock/feature/market/application/query/FinalizedCandleIntervalsReader.java`.
- Test artifact: `backend/src/test/java/coin/coinzzickmock/feature/market/application/query/FinalizedCandleIntervalsReaderTest.java`.
- Broker artifact: `backend/src/main/java/coin/coinzzickmock/feature/market/web/MarketCandleRealtimeSseBroker.java` now depends on `FinalizedCandleIntervalsReader` instead of `MarketHistoryRepository`.
