# Core Domain Refactor Wave RALPLAN

## Implementation Reference Documents - Read Before Coding

실행 에이전트는 코드를 바꾸기 전에 아래 문서를 이 순서로 읽는다.

1. `AGENTS.md`
2. `BACKEND.md`
3. `backend/AGENTS.md`
4. `backend/docs/README.md`
5. `backend/docs/architecture/foundations.md`
6. `backend/docs/architecture/package-and-wiring.md`
7. `backend/docs/architecture/testing-and-architecture-lint.md`
8. `backend/docs/code-quality/clean-code-responsibility.md`
9. `backend/core/AGENTS.md`
10. `backend/core/docs/README.md`
11. `backend/core/docs/application-and-providers.md`
12. `backend/app/AGENTS.md` and `backend/app/docs/web-and-job-adapters.md` when a PR touches `app/feature/*/job` or `app/feature/*/web`.
13. `backend/stream/AGENTS.md` and `backend/stream/docs/realtime-delivery-rules.md` when a PR touches market stream imports or behavior.
14. `backend/external/AGENTS.md` and `backend/external/docs/external-integration-rules.md` when a PR touches provider bridge or external market gateway imports.
15. `docs/process/branch-and-pr-rules.md` before creating a branch or PR.

## 요구사항 요약

- 목표는 `order`에서 이미 적용한 application DTO/implement/service 경계를 다른 core domain으로 확산하기 위한 마스터 migration plan을 만드는 것이다.
- 산출물은 구현이 아니라 실행 가능한 PR queue, 재사용 checklist, per-domain mini spec, stop/rollback gate, downstream handoff contract다.
- 실행은 한 번에 모든 domain을 바꾸는 mega PR이 아니라 domain 또는 sub-domain 단위의 작은 PR로 진행한다.
- 제품 동작, API, DB/schema, 거래 공식, persistence 의미는 바꾸지 않는다.
- downstream `$ralph`, `$team`, `$autopilot`, `$ultragoal`이 각 mini spec을 받아 새 deep interview 없이 실행할 수 있어야 한다.

근거:
- 입력 spec은 master migration plan, domain PR queue, reusable checklist, mini spec, stop/rollback gate, handoff contract를 완료 조건으로 둔다: `.omx/specs/deep-interview-core-domain-refactor-wave.md`.
- backend module boundary는 `core`가 business core, `app`이 executable assembly root, `stream`/`storage`/`external`이 leaf adapter임을 고정한다: `BACKEND.md:21`, `BACKEND.md:89`.
- backend feature layer는 `web`, `job`, `application`, `domain`, `infrastructure`로 고정되고 `application/implement`는 layer가 아니라 application 내부 실행 세부 협력 객체다: `backend/docs/architecture/foundations.md:41`, `backend/docs/architecture/foundations.md:51`.
- 새 application input/output/projection DTO의 기본 home은 `application/dto`이고 `application/command`, `application/result`는 migration residue다: `backend/docs/architecture/package-and-wiring.md:53`, `backend/docs/architecture/package-and-wiring.md:65`.
- `application/realtime` 같은 timing/technical package는 migration 때 service/dto/implement/domain으로 재분류한다: `backend/docs/architecture/package-and-wiring.md:67`, `backend/core/docs/application-and-providers.md:242`.
- 구조 검증은 `./gradlew architectureLint`와 `./gradlew check`가 기준이다: `backend/docs/architecture/testing-and-architecture-lint.md:32`, `BACKEND.md:103`.

## RALPLAN-DR 요약

### 원칙

1. 동작 보존 우선: package와 import 이동만으로 끝낼 수 있는 변경을 먼저 실행하고, business rule 변경 신호가 나오면 해당 PR을 중단한다.
2. order convention 우선: `order/application/dto`, `order/application/service`, `order/application/implement` 구조를 기준 slice로 삼는다.
3. 작은 PR 우선: 파일 수, test surface, cross-feature coupling이 낮은 domain부터 migration한다.
4. 기술 맥락 이름 제거: `command`, `result`, `realtime` package residue는 `dto`, `service`, `implement`, 기존 목적 package, 또는 domain으로 책임을 설명하게 한다.
5. 검증 ratchet: 각 PR은 자신이 소유한 residue count를 줄이고, `architectureLint`/targeted test 실패 시 다음 wave로 넘어가지 않는다.

### Decision Drivers

1. Reviewability: domain별 diff가 작아야 import 이동과 behavior change가 섞이지 않는다.
2. Coupling risk: `market`과 `position`은 order/stream/provider/job과 엮여 있어 늦게, 더 작게 나눈다.
3. Execution autonomy: mini spec이 exact package move, affected tests, stop condition을 포함해야 downstream agent가 다시 질문하지 않는다.

### Viable Options

#### Option A: Low-risk domain queue 후 market split

- 접근: `member` -> `leaderboard` -> `account` -> `reward` -> `positionpeek` -> `position` -> `market` split 순서로 이동한다.
- 장점: 작고 테스트가 상대적으로 잘 있는 domain부터 convention을 반복 검증한다. `market` 전까지 import 이동 기계 작업과 stop gate를 안정화할 수 있다.
- 단점: `market` residue가 오래 남아서 중간 기간 동안 old/new convention이 공존한다.

#### Option B: Residue count가 큰 domain부터 처리

- 접근: `market` -> `reward` -> `position` -> `account` 순서로 큰 residue를 먼저 줄인다.
- 장점: 총 residue 수는 빠르게 줄어든다.
- 단점: `market`은 `realtime` 35개와 테스트 48개가 걸린 고위험 domain이라 첫 PR 실패 가능성이 높고, 계획의 반복 가능한 checklist를 검증하기 전에 가장 복잡한 domain을 건드린다.

#### Option C: DTO-only 전체 sweep 후 realtime 별도 wave

- 접근: 모든 domain의 `command`/`result`만 한 PR 또는 몇 개 PR로 `dto` 이동하고, `realtime`은 나중에 처리한다.
- 장점: 단순 import 변경을 빠르게 끝낸다.
- 단점: 여러 domain을 한 번에 건드려 PR boundary가 흐려지고, `application/realtime` 해체라는 핵심 목표가 별도 대형 리스크로 남는다.

#### Option D: Classification/documentation lock first

- 접근: 코드 migration 전에 `market`/`position` realtime 전체 파일의 target package와 문서 충돌을 먼저 확정한 뒤 작은 domain queue로 들어간다.
- 장점: 가장 위험한 lifecycle ownership 리스크를 초반에 태우고, downstream agent가 애매한 후보 표현을 구현으로 오해하지 않는다.
- 단점: 첫 번째 실행 PR에서 residue count가 줄지 않고, 계획 보강 PR이 하나 더 생긴다.

### 선택

Option A + Option D synthesis를 선택한다. 작은 domain부터 migration하되, Architect/Critic 검토에서 지적된 대로 `market`/`position`의 lifecycle ownership 리스크를 마지막까지 미루지 않는다. PR-0 바로 뒤에 코드 변경 없는 `PR-0A: Realtime classification lock`을 추가해 `market/application/realtime` 35개와 `position/application/realtime` 3개의 목표 package를 먼저 잠근다. Option C의 DTO-only sweep은 각 domain PR 안의 첫 단계로 흡수한다.

## Brownfield Evidence

### Canonical order 기준

- 현재 `order/application`은 `dto` 9개, `implement` 17개, `repository` 1개, `service` 7개로 정리되어 있다.
- `CreateOrderCommand`는 `coin.coinzzickmock.feature.order.application.dto`에 있다: `backend/core/src/main/java/coin/coinzzickmock/feature/order/application/dto/CreateOrderCommand.java:1`.
- `CreateOrderService`는 service entrypoint이며 DTO와 implement collaborator를 주입한다: `backend/core/src/main/java/coin/coinzzickmock/feature/order/application/service/CreateOrderService.java:35`, `backend/core/src/main/java/coin/coinzzickmock/feature/order/application/service/CreateOrderService.java:56`.
- `OrderPendingFillProcessor`는 `application/implement`에 있는 실행 세부 협력 객체이고, market event와 position close 협력 객체를 조합한다: `backend/core/src/main/java/coin/coinzzickmock/feature/order/application/implement/OrderPendingFillProcessor.java:1`, `backend/core/src/main/java/coin/coinzzickmock/feature/order/application/implement/OrderPendingFillProcessor.java:33`.

### 남은 residue inventory

탐색 기준: `find backend/core/src/main/java/coin/coinzzickmock/feature -type f | grep -E '/application/(command|result|realtime)/'`.

| Domain | Residue | Count | Risk |
| --- | --- | ---: | --- |
| `member` | `application/result` | 1 | 낮음 |
| `leaderboard` | `application/result` | 3 | 낮음 |
| `account` | `application/result` | 6 | 낮음-중간 |
| `reward` | `application/command`, `application/result` | 10 | 중간 |
| `positionpeek` | `application/result` | 6 | 중간 |
| `position` | `application/realtime`, `application/result` | 8 | 중간-높음 |
| `market` | `application/realtime`, `application/result` | 37 | 높음 |

추가 유사 구조:
- `market`은 `application/history`, `application/repair`, `application/query`, `application/realtime`가 공존한다.
- `position`은 `application/close`, `application/query`, `application/realtime`, `application/result`가 공존한다.
- `leaderboard`는 `application/store`, `application/event`, `application/result`가 있다.
- `reward`는 `application/grant`, `application/refund`, `application/notification`, `application/event`, `application/command`, `application/result`가 있다.

대표 파일:
- `reward/application/command/GrantProfitPointCommand`는 DTO 성격이 명확하다: `backend/core/src/main/java/coin/coinzzickmock/feature/reward/application/command/GrantProfitPointCommand.java:1`.
- `member/application/result/MemberProfileResult`는 domain model에서 result를 조립하는 application output DTO다: `backend/core/src/main/java/coin/coinzzickmock/feature/member/application/result/MemberProfileResult.java:1`, `backend/core/src/main/java/coin/coinzzickmock/feature/member/application/result/MemberProfileResult.java:19`.
- `leaderboard/application/result/LeaderboardResult`는 snapshot을 response-facing application result로 변환한다: `backend/core/src/main/java/coin/coinzzickmock/feature/leaderboard/application/result/LeaderboardResult.java:1`, `backend/core/src/main/java/coin/coinzzickmock/feature/leaderboard/application/result/LeaderboardResult.java:16`.
- `market/application/realtime/MarketRealtimeFeed`는 cache/store/projector/refresher orchestration을 한 package 아래 묶고 있다: `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketRealtimeFeed.java:1`, `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketRealtimeFeed.java:12`.
- `position/application/realtime/OpenPositionBook`는 in-memory open position book이다: `backend/core/src/main/java/coin/coinzzickmock/feature/position/application/realtime/OpenPositionBook.java:1`, `backend/core/src/main/java/coin/coinzzickmock/feature/position/application/realtime/OpenPositionBook.java:14`.
- `DelayedClosedMinuteCandlePersistenceScheduler`는 `TaskScheduler`와 설정 값을 직접 잡고 delayed background trigger를 소유한다: `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/DelayedClosedMinuteCandlePersistenceScheduler.java:15`, `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/DelayedClosedMinuteCandlePersistenceScheduler.java:47`.
- `MarketHistoryStartupBackfill`은 startup backfill이라는 lifecycle 이름을 갖지만 실제 내용은 repository/gateway/recorder orchestration이다: `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketHistoryStartupBackfill.java:14`, `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketHistoryStartupBackfill.java:22`.
- `OpenPositionBookHydrator`는 `SmartLifecycle`을 직접 구현해 startup hydration을 수행한다: `backend/core/src/main/java/coin/coinzzickmock/feature/position/application/realtime/OpenPositionBookHydrator.java:8`, `backend/core/src/main/java/coin/coinzzickmock/feature/position/application/realtime/OpenPositionBookHydrator.java:17`.

### Realtime Classification Lock

PR-0A에서 아래 표를 codebase fact로 재확인하고, 실행 전 계획 파일에 확정본을 남긴다. 표와 다른 판단이 필요하면 해당 domain PR에서 바로 구현하지 말고 계획을 갱신한다.

#### Position realtime 3개

| Source | Target | Reason | Lifecycle note |
| --- | --- | --- | --- |
| `OpenPositionBook` | `position/application/implement` | in-memory execution-detail book | lifecycle 없음 |
| `OpenPositionBookWriter` | `position/application/implement` | order/position mutation 이후 book update collaborator | lifecycle 없음 |
| `OpenPositionBookHydrator` | split: core hydrator coordinator in `position/application/implement`, trigger in `app/feature/position/job` if lifecycle remains | `SmartLifecycle` startup trigger와 repository hydration orchestration이 섞임 | 단순 package move 금지. `SmartLifecycle` 유지가 필요하면 job/runtime trigger 경계로 분리 |

#### Market realtime 35개

| Source | Preliminary target | Reason |
| --- | --- | --- |
| `CompletedHourlyCandleBuilder` | `market/application/implement` | candle aggregation execution collaborator |
| `CurrentMarketCandleBootstrapper` | split if lifecycle trigger exists: trigger in `app/feature/market/job`, core coordinator in `market/application/history` or `market/application/implement` | startup/current candle bootstrap orchestration; trigger와 분리 필요 |
| `DelayedClosedMinuteCandlePersistenceScheduler` | split: scheduling trigger in `app/feature/market/job`, core delayed persistence coordinator in `market/application/implement` | `TaskScheduler`/`@Value` runtime trigger ownership |
| `MarketCandleUpdatedEvent` | `market/application/dto` | application event payload |
| `MarketFundingScheduleLookup` | `market/application/implement` | funding schedule lookup collaborator |
| `MarketHistoryFinalizedEvent` | `market/application/dto` | application event payload |
| `MarketHistoryPersistenceCoordinator` | `market/application/history` unless PR-0A proves public service entrypoint | persisted candle use-case coordinator |
| `MarketHistoryPersistenceResult` | `market/application/dto` | application result payload |
| `MarketHistoryPersistenceStatus` | `market/application/dto` | application result status |
| `MarketHistoryRecorder` | `market/application/history` unless PR-0A proves public service entrypoint | history recording use case/coordinator |
| `MarketHistoryStartupBackfill` | `market/application/repair` or `market/application/history`; startup trigger stays in `job` | startup lifecycle name이지만 repository/gateway/recorder application orchestration |
| `MarketMinuteCandleHistoryListener` | `market/application/history` or `market/application/service` if event entrypoint is public use case | application event entrypoint |
| `MarketMinuteClosedEvent` | `market/application/dto` | application event payload |
| `MarketPriceMovementDirection` | `market/application/dto` unless domain rule emerges | event/value payload enum |
| `MarketRealtimeFeed` | split read facade: `market/application/implement` reader/projector facade; refresh trigger/service separate | `GetMarketSummaryService`가 주입하므로 service-to-service 위반 방지 필요 |
| `MarketRealtimeFreshnessPolicy` | `market/application/implement` or domain if product freshness rule | freshness collaborator |
| `MarketRealtimeHealth` | `market/application/dto` | health snapshot payload |
| `MarketRealtimeReconnectState` | `market/application/implement` | reconnect state mechanism |
| `MarketRealtimeSourceSnapshot` | `market/application/dto` | source snapshot payload |
| `MarketRealtimeSourceType` | `market/application/dto` | payload/source enum |
| `MarketSnapshotStore` | `market/application/implement` | cache/store collaborator |
| `MarketSummaryUpdatedEvent` | `market/application/dto` | application event payload |
| `MarketSupportedMarketRefresher` | `market/application/implement` unless PR-0A proves a public refresh use case | supported-market refresh coordinator |
| `MarketTradePriceMovedEvent` | `market/application/dto` | application event payload |
| `MarketTradePriceMovementPublisher` | `market/application/implement` | movement event publisher collaborator |
| `ProviderMarketRealtimeEventBridge` | `market/application/service` unless provider lifecycle criteria require provider runtime; PR-0A must decide | provider event to feature application bridge |
| `RealtimeMarketCandleProjector` | `market/application/implement` | projection collaborator |
| `RealtimeMarketCandleState` | `market/application/implement` if mutable state, otherwise `market/application/dto`; PR-0A must decide before PR-9 | candle state payload/mechanism |
| `RealtimeMarketCandleUpdate` | `market/application/dto` | update payload |
| `RealtimeMarketCandleUpdateService` | `market/application/service` | public use-case entrypoint |
| `RealtimeMarketDataStore` | `market/application/implement` | in-memory data store collaborator |
| `RealtimeMarketPriceReader` | `market/application/query` or `market/application/implement`; must not become `service` | read facade consumed by order/market |
| `RealtimeMarketSummaryProjector` | `market/application/implement` | projection collaborator |
| `RealtimeMarketTickerUpdate` | `market/application/dto` | update payload |
| `RealtimeMarketTradeTick` | `market/application/dto` | update payload |


### PR-0A Confirmed Classification Lock (recorded 2026-05-17)

Code fact check confirmed the strict-removal path: `application/realtime` is a migration residue, not a reusable purpose package for new work. `backend/core/docs/application-and-providers.md` now uses `grant`/`history`/`repair` examples for legitimate `application/<purpose>` packages and explicitly sends `realtime` residue through the Technical Package Split Recipe.

#### Locked position decisions

| Source | Confirmed target | Evidence | Execution constraint |
| --- | --- | --- | --- |
| `OpenPositionBook` | `position/application/implement` | Spring-managed in-memory book with candidate maps, dirty generation tracking, and no lifecycle interface | Package move only is acceptable after PR-6 DTO move. Rename is optional and must not widen the diff. |
| `OpenPositionBookWriter` | `position/application/implement` | Transaction-synchronization writer around `OpenPositionBook` updates | Preserve after-commit timing; do not change order/position mutation behavior. |
| `OpenPositionBookHydrator` | split: core hydration coordinator in `position/application/implement`, startup trigger in new `app/feature/position/job` | Implements `SmartLifecycle` while also orchestrating repository hydration and symbol rehydration | Do not move `SmartLifecycle` into core `implement`. PR-7 must extract the runtime trigger to `app` if startup hydration remains automatic. |

#### Locked market decisions

| Source | Confirmed target | Evidence | Execution constraint |
| --- | --- | --- | --- |
| `CompletedHourlyCandleBuilder` | `market/application/implement` | Candle aggregation collaborator with no public use-case/lifecycle role | Preserve rollup completeness semantics. |
| `CurrentMarketCandleBootstrapper` | core coordinator in `market/application/implement`; any startup/stream trigger remains outside core in `app`/`web` boundary | Bootstraps current candle data through gateway/store/projector and is exposed to stream bridge | Do not introduce lifecycle annotation in core; `MarketStreamBridgeConfiguration` remains the trigger wiring boundary. |
| `DelayedClosedMinuteCandlePersistenceScheduler` | split: scheduling trigger/runtime config in `app/feature/market/job`, delayed persistence coordinator in `market/application/implement` | Owns `TaskScheduler`, `@Value`, delay, and in-flight closed-minute guard | PR-10A must preserve delay and in-flight release behavior. |
| `MarketCandleUpdatedEvent`, `MarketHistoryFinalizedEvent`, `MarketMinuteClosedEvent`, `MarketSummaryUpdatedEvent`, `MarketTradePriceMovedEvent` | `market/application/dto` | Application event payload records | Preserve field names, publication order, and listener transaction behavior. |
| `RealtimeMarketCandleUpdate`, `RealtimeMarketTickerUpdate`, `RealtimeMarketTradeTick`, `MarketRealtimeSourceSnapshot`, `MarketRealtimeSourceType`, `MarketRealtimeHealth` | `market/application/dto` | Inbound/update/source snapshot payloads and source health enums | Keep validation constructors unchanged. |
| `MarketHistoryPersistenceResult`, `MarketHistoryPersistenceStatus` | `market/application/dto` | Persistence result/status payload from coordinator/repair flow | Preserve `shouldRetry`, `saved`, and `persisted` semantics. |
| `MarketPriceMovementDirection` | `market/application/dto` | Event/value payload enum for movement notifications; no durable domain invariant found | PR-9 may move it with event DTOs. |
| `RealtimeMarketCandleState` | `market/application/implement` | Internal store state consumed by `RealtimeMarketDataStore` and projector; not an external event/update DTO | PR-9 must not move it to `dto`; move with store/projector collaborators in PR-10B. |
| `MarketRealtimeFreshnessPolicy` | `market/application/implement` | Freshness mechanism instantiated by realtime reader; no product-owned long-lived domain rule found | Do not promote to domain without a new product/spec decision. |
| `MarketRealtimeReconnectState` | `market/application/dto` | Source snapshot payload enum used by `MarketRealtimeSourceSnapshot` tests | Move with source snapshot DTOs in PR-9. |
| `MarketSnapshotStore`, `RealtimeMarketDataStore`, `RealtimeMarketCandleProjector`, `RealtimeMarketSummaryProjector`, `MarketFundingScheduleLookup`, `MarketTradePriceMovementPublisher` | `market/application/implement` | Store/projector/lookup/publisher execution collaborators | Keep as concrete collaborators; do not add new port/usecase interfaces. |
| `MarketHistoryPersistenceCoordinator`, `MarketHistoryRecorder`, `MarketMinuteCandleHistoryListener` | `market/application/history` | Closed-minute persistence/history orchestration and application event listener | Listener may remain an application event entrypoint in `history`; no `service` package is required. |
| `MarketHistoryStartupBackfill` | `market/application/repair`; startup trigger remains `app/feature/market/job` | Existing `MarketHistoryStartupBackfillReadyEventListener` already owns `ApplicationReadyEvent`; core class owns repair/backfill orchestration | Preserve startup property gate and gateway handoff in `app`. |
| `MarketSupportedMarketRefresher` | `market/application/implement` | Supported-market refresh/cache/event collaborator invoked by job and read facade | Do not classify as public service unless a later PR separates a true user-facing use case. |
| `MarketRealtimeFeed` | split: read facade in `market/application/query` or `market/application/implement`, refresh coordinator in `market/application/implement` | Combines `refreshSupportedMarkets`, `getMarket`, and `getSupportedMarkets`; `GetMarketSummaryService` currently injects it | PR-10C must avoid `application/service` -> `application/service`; split only when it improves boundary clarity. |
| `RealtimeMarketPriceReader` | `market/application/query` | Cross-feature fresh price read facade used by order, position, account, and positionpeek application code | Never move to `service`; preserve fresh/stale error semantics. |
| `RealtimeMarketCandleUpdateService` | `market/application/service` | Public application entrypoint accepting realtime candle updates and publishing events | Keep service entrypoint but avoid direct service-to-service dependencies. |
| `ProviderMarketRealtimeEventBridge` | `market/application/service` | Provider event consumer bridge configured in app; translates provider events into feature application updates | This is not provider-owned lifecycle. Keep low-level provider websocket mechanics outside feature service. |


## Acceptance Criteria

1. 계획 파일이 `.omx/plans/2026-05-16-core-domain-refactor-wave-ralplan.md`에 저장된다.
2. 계획은 PR queue, domain order rationale, market split strategy를 포함한다.
3. 계획은 per-domain checklist와 mini spec을 포함하고, 각 mini spec은 scope, non-goal, exact package moves, affected tests, stop condition을 가진다.
4. 각 downstream PR은 해당 domain에서 `application/command`, `application/result`, `application/realtime` Java source count를 줄이거나 0으로 만든다.
5. 각 downstream PR은 behavior/API/DB/schema 변경을 만들지 않는다. DB migration이 필요하다는 결론이 나오면 stop gate로 처리한다.
6. 각 downstream PR은 최소 `cd backend && ./gradlew architectureLint --console=plain`와 해당 domain targeted tests를 실행한다.
7. 마지막 wave 후 `find backend/core/src/main/java/coin/coinzzickmock/feature -type f | grep -E '/application/(command|result|realtime)/'`가 0건이어야 한다.
8. `./gradlew check --console=plain`이 마지막 wave에서 통과해야 한다.
9. branch/PR 생성 시 branch name은 `<type>/<kebab-case-summary>`이고 `codex/*` 접두사를 쓰지 않는다: `docs/process/branch-and-pr-rules.md:5`, `docs/process/branch-and-pr-rules.md:37`.
10. PR-0A가 `application-and-providers.md`의 `application/<purpose>` 예시와 `package-and-wiring.md`의 `application/realtime` split rule 사이의 긴장을 해결한다. strict removal을 택하면 `application-and-providers.md`의 `realtime` 예시는 legacy/예외 문맥으로 갱신하거나 다른 purpose 예시로 바꾼다.

## Master PR Queue

### PR-0: Queue lock and residue baseline

- Branch: `docs/core-domain-refactor-wave-plan`
- Scope: 이 계획 파일을 기준 artifact로 저장하고, 현재 residue command를 기록한다.
- Rationale: 실행 전에 downstream agent가 같은 inventory와 stop gate를 공유한다.
- Verification: `rtk git diff --check`.


#### PR-0 residue baseline (recorded 2026-05-17)

This is the locked baseline for downstream residue ratchets. Re-run the same command before each PR and record before/after counts in that PR body.

Command:

```bash
find backend/core/src/main/java/coin/coinzzickmock/feature -type f | grep -E "/application/(command|result|realtime)/" | sort
```

Summary:

| Domain | Residue | Count |
| --- | --- | ---: |
| `account` | `application/result` | 6 |
| `leaderboard` | `application/result` | 3 |
| `market` | `application/realtime` | 35 |
| `market` | `application/result` | 2 |
| `member` | `application/result` | 1 |
| `position` | `application/realtime` | 3 |
| `position` | `application/result` | 5 |
| `positionpeek` | `application/result` | 6 |
| `reward` | `application/command` | 1 |
| `reward` | `application/result` | 9 |

Exact file list:

- `backend/core/src/main/java/coin/coinzzickmock/feature/account/application/result/AccountMutationResult.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/account/application/result/AccountRefillCreditResult.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/account/application/result/AccountRefillResult.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/account/application/result/AccountRefillStatusResult.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/account/application/result/AccountSummaryResult.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/account/application/result/WalletHistoryResult.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/leaderboard/application/result/LeaderboardEntryResult.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/leaderboard/application/result/LeaderboardMemberRankResult.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/leaderboard/application/result/LeaderboardResult.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/CompletedHourlyCandleBuilder.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/CurrentMarketCandleBootstrapper.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/DelayedClosedMinuteCandlePersistenceScheduler.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketCandleUpdatedEvent.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketFundingScheduleLookup.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketHistoryFinalizedEvent.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketHistoryPersistenceCoordinator.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketHistoryPersistenceResult.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketHistoryPersistenceStatus.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketHistoryRecorder.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketHistoryStartupBackfill.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketMinuteCandleHistoryListener.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketMinuteClosedEvent.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketPriceMovementDirection.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketRealtimeFeed.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketRealtimeFreshnessPolicy.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketRealtimeHealth.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketRealtimeReconnectState.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketRealtimeSourceSnapshot.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketRealtimeSourceType.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketSnapshotStore.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketSummaryUpdatedEvent.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketSupportedMarketRefresher.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketTradePriceMovedEvent.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketTradePriceMovementPublisher.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/ProviderMarketRealtimeEventBridge.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/RealtimeMarketCandleProjector.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/RealtimeMarketCandleState.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/RealtimeMarketCandleUpdate.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/RealtimeMarketCandleUpdateService.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/RealtimeMarketDataStore.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/RealtimeMarketPriceReader.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/RealtimeMarketSummaryProjector.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/RealtimeMarketTickerUpdate.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime/RealtimeMarketTradeTick.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/result/MarketCandleResult.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/market/application/result/MarketSummaryResult.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/member/application/result/MemberProfileResult.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/position/application/realtime/OpenPositionBook.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/position/application/realtime/OpenPositionBookHydrator.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/position/application/realtime/OpenPositionBookWriter.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/position/application/result/ClosePositionResult.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/position/application/result/OpenPositionCandidate.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/position/application/result/PositionHistoryResult.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/position/application/result/PositionMutationResult.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/position/application/result/PositionSnapshotResult.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/positionpeek/application/result/PositionPeekItemBalanceResult.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/positionpeek/application/result/PositionPeekPublicPositionResult.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/positionpeek/application/result/PositionPeekSnapshotRecord.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/positionpeek/application/result/PositionPeekSnapshotResult.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/positionpeek/application/result/PositionPeekStatusResult.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/positionpeek/application/result/PositionPeekTargetResult.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/reward/application/command/GrantProfitPointCommand.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/reward/application/result/AdminShopItemResult.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/reward/application/result/PositionPeekItemBalanceResult.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/reward/application/result/RewardPointHistoryResult.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/reward/application/result/RewardPointResult.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/reward/application/result/RewardRedemptionResult.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/reward/application/result/RewardShopHistoryKind.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/reward/application/result/RewardShopHistoryResult.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/reward/application/result/ShopItemResult.java`
- `backend/core/src/main/java/coin/coinzzickmock/feature/reward/application/result/ShopPurchaseResult.java`

### PR-0A: Realtime classification lock

- Branch: `docs/realtime-classification-lock`
- Scope: 코드 변경 없이 `position/application/realtime` 3개와 `market/application/realtime` 35개의 target package를 확정한다. 필요하면 이 계획 파일을 갱신한다.
- Rationale: 작은 DTO PR을 먼저 끝내도 lifecycle ownership 리스크는 줄지 않는다. `SmartLifecycle`, `TaskScheduler`, startup/backfill, provider bridge, read facade 분류를 실행 전 잠근다.
- Required decisions:
  - `OpenPositionBookHydrator`는 단순 `implement` 이동인지, `app/feature/position/job` trigger와 core hydrator coordinator split인지 결정한다.
  - `DelayedClosedMinuteCandlePersistenceScheduler`는 `job` trigger split을 기본값으로 한다.
  - `MarketRealtimeFeed`는 service-to-service 위반을 만들지 않도록 read facade와 refresh coordinator를 분리할지 결정한다.
  - `application-and-providers.md`의 `realtime` purpose-package 예시는 legacy/예외로 고칠지, `grant`/`history` 같은 다른 예시로 바꿀지 결정한다.
- Verification:
  ```bash
  find backend/core/src/main/java/coin/coinzzickmock/feature/market/application/realtime -maxdepth 1 -type f | xargs -n1 basename | sort
  find backend/core/src/main/java/coin/coinzzickmock/feature/position/application/realtime -maxdepth 1 -type f | xargs -n1 basename | sort
  rtk git diff --check
  ```

### PR-1: Member DTO package migration

- Branch: `refactor/member-application-dto`
- Scope: `member/application/result` 1개를 `member/application/dto`로 이동하고 import/test package를 갱신한다.
- Rationale: 가장 작은 DTO-only migration. withdrawal/leaderboard event는 건드리지 않는다.
- Stop gate: profile result semantic 변경, active/withdrawn lookup 변경, member web response 변경.
- Verification:
  ```bash
  cd backend
  ./gradlew test --tests '*RegisterMemberServiceTest' --tests '*WithdrawMemberServiceTest' --tests '*MemberCredentialPersistenceRepositoryTest' --console=plain
  ./gradlew architectureLint --console=plain
  ```

### PR-2: Leaderboard DTO package migration

- Branch: `refactor/leaderboard-application-dto`
- Scope: `leaderboard/application/result` 3개를 `leaderboard/application/dto`로 이동한다. `application/store`와 `application/event`는 이번 PR에서 이름만 이유 없이 바꾸지 않는다.
- Rationale: small but better test surface. Redis snapshot semantics를 보호한다.
- Stop gate: rank score, fallback, active member filtering, tie behavior 변경.
- Verification:
  ```bash
  cd backend
  ./gradlew test --tests '*GetLeaderboardServiceTest' --tests '*RefreshLeaderboardServiceTest' --tests '*LeaderboardWalletBalanceChangedListenerTest' --tests '*LeaderboardSnapshotStoreAdapterTest' --console=plain
  ./gradlew architectureLint --console=plain
  ```

### PR-3: Account DTO package migration

- Branch: `refactor/account-application-dto`
- Scope: `account/application/result` 6개를 `account/application/dto`로 이동한다. `application/query`는 이미 purpose package로 남기되 DTO/result import만 정리한다.
- Rationale: account는 order/position이 참조하므로 market/position보다 먼저 import drift를 줄인다.
- Stop gate: refill date policy, wallet balance/history calculation, account mutation result semantics 변경.
- Verification:
  ```bash
  cd backend
  ./gradlew test --tests '*GetAccountSummaryServiceTest' --tests '*RefillTradingAccountServiceTest' --tests '*AccountRefillDatePolicyTest' --tests '*AccountPersistenceRepositoryTest' --console=plain
  ./gradlew architectureLint --console=plain
  ```

### PR-4: Reward command/result DTO migration

- Branch: `refactor/reward-application-dto`
- Scope: `reward/application/command/GrantProfitPointCommand`와 `reward/application/result` 9개를 `reward/application/dto`로 이동한다.
- Rationale: `command`와 `result`를 동시에 제거하는 첫 medium DTO migration. `grant`, `refund`, `notification` purpose package는 유지한다.
- Stop gate: point grant/refund/redemption policy 변경, admin authorization 변경, notification side effect 변경.
- Verification:
  ```bash
  cd backend
  ./gradlew test --tests '*RewardRedemptionServiceTest' --tests '*CreateRewardRedemptionServiceTest' --tests '*GetRewardPointServiceTest' --tests '*AdminRewardShopItemServiceTest' --tests '*RewardPointGrantProcessorTest' --tests '*RewardControllerIntegrationTest' --console=plain
  ./gradlew architectureLint --console=plain
  ```

### PR-5: PositionPeek DTO migration plus test guard

- Branch: `refactor/positionpeek-application-dto`
- Scope: `positionpeek/application/result` 6개를 `positionpeek/application/dto`로 이동하고, existing integration test가 import/package 변경을 커버하는지 확인한다.
- Rationale: 파일 수는 작지만 market/position/reward/leaderboard 조회 성격이 섞인 화면 feature라 테스트 guard를 먼저 확인한다.
- Stop gate: public snapshot payload, shop item balance, target visibility, rank/position display semantic 변경.
- Verification:
  ```bash
  cd backend
  ./gradlew test --tests '*PositionPeekControllerIntegrationTest' --console=plain
  ./gradlew architectureLint --console=plain
  ```

### PR-6: Position DTO migration

- Branch: `refactor/position-application-dto`
- Scope: `position/application/result` 5개를 `position/application/dto`로 이동한다. `position/application/realtime` 3개는 PR-7에서 다룬다.
- Rationale: close/realtime 이동 전 DTO import churn을 분리한다.
- Stop gate: open/close position result payload, history projection, mutation result 변경.
- Verification:
  ```bash
  cd backend
  ./gradlew test --tests '*GetOpenPositionsServiceTest' --tests '*ClosePositionServiceTest' --tests '*UpdatePositionLeverageServiceTest' --tests '*UpdatePositionTpslServiceTest' --tests '*PositionHistoryResponseTest' --console=plain
  ./gradlew architectureLint --console=plain
  ```

### PR-7: Position realtime package split

- Branch: `refactor/position-open-position-book-boundary`
- Scope: `position/application/realtime`의 `OpenPositionBook`, `OpenPositionBookHydrator`, `OpenPositionBookWriter`를 책임별로 재분류한다. PR-0A에서 `SmartLifecycle` split을 확정한 뒤 실행한다.
- Planned moves:
  - `OpenPositionBook` -> `position/application/implement/OpenPositionBook` 또는 더 명확히 `PositionOpenBook`으로 rename한다. 단, rename이 과도한 import churn을 만들면 class name은 유지하고 package만 이동한다.
  - `OpenPositionBookWriter` -> `position/application/implement`.
  - `OpenPositionBookHydrator` -> core coordinator는 `position/application/implement`, startup trigger는 필요 시 `app/feature/position/job`로 분리한다. `SmartLifecycle`을 core implement에 그대로 옮기는 방식은 기본값이 아니다.
  - `OpenPositionCandidate`는 PR-6에서 이미 `dto`로 이동되어 있어야 한다.
- Rationale: `OpenPositionBook`은 in-memory execution-detail collaborator이며 기술 timing package가 아니다.
- Stop gate: order pending fill, liquidation, TP/SL processing에서 open position book update timing 변경. `SmartLifecycle`/startup hydration ownership을 package move만으로 해결하려는 상황도 stop gate다.
- Verification:
  ```bash
  cd backend
  ./gradlew test --tests '*OpenPositionBookTest' --tests '*OpenPositionBookHydratorTest' --tests '*OrderPendingFillProcessorTest' --tests '*OrderPositionLiquidationProcessorTest' --tests '*OrderPositionTakeProfitStopLossProcessorTest' --console=plain
  ./gradlew architectureLint --console=plain
  ./gradlew check --console=plain
  ```

### PR-8: Market DTO migration

- Branch: `refactor/market-application-dto`
- Scope: `market/application/result` 2개를 `market/application/dto`로 이동한다. `market/application/realtime`은 그대로 둔다.
- Rationale: 가장 큰 domain인 market도 먼저 DTO import churn만 격리한다.
- Stop gate: candle/summary response payload 변경, stream payload semantic 변경.
- Verification:
  ```bash
  cd backend
  ./gradlew test --tests '*MarketControllerTest' --tests '*GetMarketCandlesServiceTest' --tests '*MarketStream*' --tests '*MarketSse*' --console=plain
  ./gradlew architectureLint --console=plain
  ```

### PR-9: Market realtime event and DTO split

- Branch: `refactor/market-realtime-events-dto`
- Scope: `market/application/realtime` 안의 event/payload/value DTO를 `market/application/dto`로 이동한다.
- Planned moves:
  - `MarketCandleUpdatedEvent`, `MarketSummaryUpdatedEvent`, `MarketTradePriceMovedEvent`, `MarketMinuteClosedEvent`, `MarketHistoryFinalizedEvent` -> `market/application/dto`.
  - `RealtimeMarketCandleUpdate`, `RealtimeMarketTickerUpdate`, `RealtimeMarketTradeTick`, `MarketRealtimeSourceSnapshot`, `MarketRealtimeReconnectState`, `MarketHistoryPersistenceResult`, `MarketHistoryPersistenceStatus`, `MarketRealtimeHealth`, `MarketRealtimeSourceType`, `MarketPriceMovementDirection` -> `market/application/dto`.
  - `RealtimeMarketCandleState`는 PR-0A에서 `market/application/implement`로 확정했으므로 PR-9에서 이동하지 않고 PR-10B에서 store/projector collaborator와 함께 이동한다.
- Rationale: market realtime split의 가장 쉬운 first cut은 payload/event 이동이다.
- Stop gate: event publishing order, listener transaction boundary, stream payload field 변경.
- Verification:
  ```bash
  cd backend
  ./gradlew test --tests '*ProviderMarketRealtimeEventBridgeTest' --tests '*MarketMinuteCandleHistoryListenerTest' --tests '*RealtimeMarketCandleProjectorTest' --tests '*RealtimeMarketSummaryProjectorTest' --tests '*MarketStream*' --tests '*MarketSse*' --console=plain
  ./gradlew architectureLint --console=plain
  ./gradlew check --console=plain
  ```

### PR-10A: Market lifecycle and job boundary split

- Branch: `refactor/market-realtime-lifecycle-boundary`
- Scope: lifecycle/runtime trigger ownership을 먼저 분리한다.
- Planned moves:
  - `DelayedClosedMinuteCandlePersistenceScheduler`: scheduling trigger/runtime config는 `app/feature/market/job`, core persistence coordinator는 `market/application/implement` 또는 `market/application/history`.
  - `MarketHistoryStartupBackfill`: startup trigger는 기존/신규 `job`에 남기고, repository/gateway/recorder orchestration은 `market/application/repair` 또는 `market/application/service`.
  - `CurrentMarketCandleBootstrapper`: startup bootstrap trigger가 있으면 `job`과 core coordinator로 분리한다.
- Rationale: `@Scheduled`, startup, retry/background trigger는 `job`에 둔다는 규칙을 market split 초기에 소진한다.
- Stop gate: startup warmup/backfill, delayed closed-minute persistence timing, transaction boundary 변경.
- Verification:
  ```bash
  cd backend
  ./gradlew test --tests '*MarketHistoryStartupBackfillTest' --tests '*CurrentMarketCandleBootstrapperTest' --tests '*MarketStartupReadyEventIntegrationTest' --tests '*MarketHistoryPersistenceCoordinatorTest' --tests '*MarketSchedulingConfigurationTest' --tests '*MarketRealtimeRefreshSchedulerTest' --tests '*MarketHistoryStartupBackfillReadyEventListenerTest' --tests '*MarketMinuteBoundaryEventPublisherTest' --console=plain
  ./gradlew architectureLint --console=plain
  ./gradlew check --console=plain
  ```

### PR-10B: Market application collaborator split

- Branch: `refactor/market-realtime-collaborator-boundary`
- Scope: lifecycle classes를 제외한 `market/application/realtime` 실행 객체를 `service`, `implement`, existing purpose package로 분류한다.
- Planned moves:
  - Public use-case/application entrypoint: `RealtimeMarketCandleUpdateService`, `MarketMinuteCandleHistoryListener`, `MarketHistoryPersistenceCoordinator`, `MarketHistoryRecorder`, `MarketSupportedMarketRefresher`는 PR-0A 확정 table에 따라 `market/application/service`, `market/application/history`, 또는 `market/application/implement` 중 하나로 이동한다. 확정 전 실행 금지.
  - Execution-detail collaborators: `RealtimeMarketDataStore`, `MarketSnapshotStore`, `RealtimeMarketCandleProjector`, `RealtimeMarketSummaryProjector`, `CompletedHourlyCandleBuilder`, `MarketFundingScheduleLookup`, `MarketRealtimeFreshnessPolicy`, `MarketRealtimeReconnectState`, `MarketTradePriceMovementPublisher` -> `market/application/implement` 또는 existing purpose package.
  - Read facade: `RealtimeMarketPriceReader`, `MarketRealtimeFeed`는 `GetMarketSummaryService` 등 service에서 직접 다른 service를 주입하지 않도록 `implement`/`query` facade로 분리한다.
- Rationale: market package 이름에서 `realtime` 기술 맥락을 제거한다.
- Stop gate: websocket/provider refresh lifecycle, current candle persistence timing, stale freshness policy 변경.
- Verification:
  ```bash
  cd backend
  ./gradlew test --tests '*MarketRealtime*' --tests '*RealtimeMarket*' --tests '*MarketHistory*' --tests '*MarketSchedulingConfigurationTest' --tests '*MarketMinuteBoundaryEventPublisherTest' --tests '*MarketStream*' --tests '*MarketSse*' --console=plain
  ./gradlew architectureLint --console=plain
  ./gradlew check --console=plain
  ```

### PR-10C: Market provider bridge and read facade cleanup

- Branch: `refactor/market-provider-bridge-boundary`
- Scope: `ProviderMarketRealtimeEventBridge`, `MarketRealtimeFeed`, `RealtimeMarketPriceReader`의 남은 package/ownership 문제를 정리한다.
- Planned moves:
  - `ProviderMarketRealtimeEventBridge`는 provider event를 feature application event로 번역하는 entrypoint면 `market/application/service`; provider-owned lifecycle/client mechanics면 provider runtime exception 조건을 문서와 함께 검증한다.
  - `MarketRealtimeFeed`는 `refreshSupportedMarkets` trigger와 `getMarket/getSupportedMarkets` read facade가 섞인 상태이므로 필요하면 작은 collaborator로 분리한다.
  - `RealtimeMarketPriceReader`는 order에서 fresh price reader로 쓰이므로 service가 아닌 read facade로 둔다.
- Rationale: service-to-service 금지와 provider runtime exception을 최종 market split 전에 해소한다.
- Stop gate: low-level provider lifecycle을 feature service로 끌어오거나, feature use case 실행을 provider runtime에 숨기는 경우.
- Verification:
  ```bash
  cd backend
  ./gradlew test --tests '*ProviderMarketRealtimeEventBridgeTest' --tests '*CreateOrderServiceTest' --tests '*OrderPendingFillProcessorTest' --tests '*GetMarketCandlesServiceTest' --tests '*MarketControllerTest' --tests '*MarketStream*' --tests '*MarketSse*' --console=plain
  ./gradlew architectureLint --console=plain
  ./gradlew check --console=plain
  ```

### PR-11: Market cleanup and final residue ratchet

- Branch: `refactor/market-application-residue-ratchet`
- Scope: 남은 `market/application/realtime` Java source가 0인지 확인하고 package residue search를 CI 또는 docs checklist에 고정할지 결정한다.
- Rationale: 가장 위험한 domain 완료 후 전체 residue 제거 상태를 증명한다.
- Stop gate: `application/realtime` 유지가 필요한 새 근거 발견. 이 경우 governing docs를 업데이트하기 전까지 strict removal을 하지 않는다.
- Verification: residue grep 0건, `./gradlew architectureLint --console=plain`, `./gradlew check --console=plain`.

## Reusable Per-Domain Checklist

각 PR은 아래 순서를 그대로 따른다.

1. Inventory
   - `find backend/core/src/main/java/coin/coinzzickmock/feature/<domain>/application -type f | sort`
   - `find backend/core/src/main/java/coin/coinzzickmock/feature/<domain>/application -type f | grep -E '/application/(command|result|realtime)/'`
   - import 사용처: `rg 'feature\\.<domain>\\.application\\.(command|result|realtime)' backend`
2. Classification
   - Application input/output/event/projection DTO -> `application/dto`.
   - Public use case or application event entrypoint -> `application/service`.
   - Queue/store/projector/hydrator/processor/cache/book/reader/telemetry/factory-like execution collaborator -> `application/implement` unless existing purpose package is clearer.
   - Storage-free long-lived business rule/state transition -> `domain`.
   - Existing clear purpose packages such as `market/application/history`, `market/application/repair`, `position/application/close`, `reward/application/grant` stay unless package name itself violates docs.
3. Move
   - Use IDE or scripted-safe refactor in one PR. Keep class rename optional and only when package move alone leaves misleading names.
   - Avoid new interfaces. Repository/provider/gateway contracts stay only where already technology-neutral.
   - Do not introduce `application/implement/common`, `util`, `helper`, or generic `Manager`.
4. Import/test update
   - Update production imports in `backend/core`, `backend/app`, `backend/storage`, `backend/stream`, `backend/external`.
   - Update test package declarations when tests mirror moved package.
   - Keep test fixture changes mechanical.
5. Residue check
   - Domain-local grep must be 0 for packages owned by that PR.
   - Repository-wide grep must not introduce new residue in other domains.
6. Verification
   - Run targeted domain tests first.
   - Run `cd backend && ./gradlew architectureLint --console=plain`.
   - Run `cd backend && ./gradlew check --console=plain` for PR-7 onward and all market PRs, or explain why not.
7. Report
   - Record moved package list, residue count before/after, tests run, not-tested gaps, stop gates not hit.

## Stop And Rollback Gates

Stop immediately and report instead of continuing if any condition occurs.

- Behavior gate: public API response, SSE event field, trading calculation, refill/reward/rank semantic, or persistence meaning changes.
- Schema gate: Flyway migration, generated DB schema, JPA entity mapping, or repository contract change appears necessary.
- Architecture gate: `architectureLint` reports new violations unrelated to pure package movement.
- Compile/test gate: targeted tests fail for a reason other than stale package/import reference.
- Coupling gate: package move forces non-owned domain logic edits beyond imports and package declarations.
- Market gate: provider websocket lifecycle, startup warmup/backfill, stream fan-out, or current candle persistence behavior becomes unclear.
- Reviewability gate: one PR touches more than one domain except import fallout from the moved domain.

Rollback pattern:
- For package-only moves, revert the domain PR branch before merging.
- If a PR has already merged and later wave fails, do not revert previous successful domain PRs unless they caused the failure. Open a fix PR for the failing domain.
- If final residue ratchet would require behavior changes, keep residue documented and create a new spec rather than forcing the migration.

## Per-Domain Mini Specs

### Member

- Scope: Move `MemberProfileResult` from `application/result` to `application/dto`.
- Non-goals: auth lookup behavior, withdrawal policy, member credential schema.
- Exact planned move:
  - `backend/core/src/main/java/coin/coinzzickmock/feature/member/application/result/MemberProfileResult.java`
  - -> `backend/core/src/main/java/coin/coinzzickmock/feature/member/application/dto/MemberProfileResult.java`.
- Likely imports: member service, app web controller/tests.
- Tests: `RegisterMemberServiceTest`, `WithdrawMemberServiceTest`, `RegisterMemberServiceIntegrationTest`, `MemberCredentialPersistenceRepositoryTest`.
- Stop condition: active member filtering or withdrawn member lookup changes.

### Leaderboard

- Scope: Move leaderboard result records to `application/dto`.
- Non-goals: Redis snapshot store, rank score formula, DB fallback.
- Exact planned moves:
  - `LeaderboardEntryResult`, `LeaderboardMemberRankResult`, `LeaderboardResult` -> `leaderboard/application/dto`.
- Likely imports: leaderboard services, web response mapping, tests.
- Tests: `GetLeaderboardServiceTest`, `RefreshLeaderboardServiceTest`, `LeaderboardWalletBalanceChangedListenerTest`, `LeaderboardSnapshotStoreAdapterTest`, `LeaderboardResponseTest`.
- Stop condition: rank ordering, my-rank calculation, active member filter, fallback source semantics changes.

### Account

- Scope: Move account result records to `application/dto`.
- Non-goals: wallet balance math, refill policy, account repository methods.
- Exact planned moves:
  - `AccountMutationResult`, `AccountRefillCreditResult`, `AccountRefillResult`, `AccountRefillStatusResult`, `AccountSummaryResult`, `WalletHistoryResult` -> `account/application/dto`.
- Likely imports: account services, order/position services if result types cross feature, web tests.
- Tests: `GetAccountSummaryServiceTest`, `RefillTradingAccountServiceTest`, `AccountRefillDatePolicyTest`, `AccountPersistenceRepositoryTest`, `AccountRefillStatePersistenceRepositoryTest`.
- Stop condition: account mutation/refill transaction behavior changes.

### Reward

- Scope: Move one command and nine results to `application/dto`.
- Non-goals: shop, redemption, point grant/refund, notification behavior.
- Exact planned moves:
  - `GrantProfitPointCommand` -> `reward/application/dto`.
  - `AdminShopItemResult`, `PositionPeekItemBalanceResult`, `RewardPointHistoryResult`, `RewardPointResult`, `RewardRedemptionResult`, `RewardShopHistoryKind`, `RewardShopHistoryResult`, `ShopItemResult`, `ShopPurchaseResult` -> `reward/application/dto`.
- Existing purpose packages `grant`, `refund`, `notification`, `event` remain.
- Tests:
  ```bash
  cd backend
  ./gradlew test --tests '*RewardRedemptionServiceTest' --tests '*CreateRewardRedemptionServiceTest' --tests '*GetRewardPointServiceTest' --tests '*AdminRewardShopItemServiceTest' --tests '*RewardPointGrantProcessorTest' --tests '*RewardControllerIntegrationTest' --tests '*RewardPointWalletTest' --tests '*RewardShopItemTest' --tests '*RewardPointHistoryTest' --tests '*RewardPointPolicyTest' --console=plain
  ./gradlew architectureLint --console=plain
  ```
- Stop condition: point amount, redemption state, admin authorization, notification side effect changes.

### PositionPeek

- Scope: Move positionpeek result records to `application/dto`; add or adjust tests only if current integration test misses compile-visible DTO factory behavior.
- Non-goals: peek visibility, target selection, shop item balance semantics.
- Exact planned moves:
  - `PositionPeekItemBalanceResult`, `PositionPeekPublicPositionResult`, `PositionPeekSnapshotRecord`, `PositionPeekSnapshotResult`, `PositionPeekStatusResult`, `PositionPeekTargetResult` -> `positionpeek/application/dto`.
- Tests: `PositionPeekControllerIntegrationTest`; add focused test only if a moved DTO has non-trivial factory logic.
- Stop condition: cross-feature query logic or response shape changes.

### Position DTO

- Scope: Move position result DTOs.
- Non-goals: close order reconciliation, open position book, liquidation/TP-SL behavior.
- Exact planned moves:
  - `ClosePositionResult`, `OpenPositionCandidate`, `PositionHistoryResult`, `PositionMutationResult`, `PositionSnapshotResult` -> `position/application/dto`.
- Tests: position service tests, `PositionHistoryResponseTest`, order tests that import `OpenPositionCandidate`.
- Stop condition: position history/mutation payload changes.

### Position Realtime Split

- Scope: Remove `position/application/realtime`.
- Non-goals: reworking order fill algorithms.
- Exact planned moves:
  - `OpenPositionBook` -> `position/application/implement/OpenPositionBook`.
  - `OpenPositionBookWriter` -> `position/application/implement/OpenPositionBookWriter`.
  - `OpenPositionBookHydrator` -> split required if startup lifecycle remains: core hydration coordinator in `position/application/implement`, runtime trigger in `app/feature/position/job`. `SmartLifecycle`을 core implement로 단순 이동하는 것은 금지한다.
  - Optional class rename only if it improves ownership without widening diff.
- Tests:
  ```bash
  cd backend
  ./gradlew test --tests '*OpenPositionBookTest' --tests '*OpenPositionBookHydratorTest' --tests '*OrderPendingFillProcessorTest' --tests '*OrderPositionLiquidationProcessorTest' --tests '*OrderPositionTakeProfitStopLossProcessorTest' --console=plain
  ./gradlew check --console=plain
  ```
- Stop condition: dirty generation, hydration, symbol eviction, order-triggered book update timing, or `SmartLifecycle` ownership changes becoming unclear.

### Market DTO

- Scope: Move market result DTOs first.
- Non-goals: realtime package split, stream topology, provider bridge, history repair behavior.
- Exact planned moves:
  - `MarketCandleResult`, `MarketSummaryResult` -> `market/application/dto`.
- Tests:
  ```bash
  cd backend
  ./gradlew test --tests '*MarketControllerTest' --tests '*GetMarketCandlesServiceTest' --tests '*MarketStream*' --tests '*MarketSse*' --console=plain
  ./gradlew architectureLint --console=plain
  ```
- Stop condition: candle summary response shape changes.

### Market PR-9 Realtime Events DTO

- Scope: Move market event/payload/value records out of `application/realtime`.
- Non-goals: changing listener ordering, websocket lifecycle, startup backfill.
- Exact planned moves:
  | Source | Target |
  | --- | --- |
  | `MarketCandleUpdatedEvent` | `market/application/dto` |
  | `MarketSummaryUpdatedEvent` | `market/application/dto` |
  | `MarketTradePriceMovedEvent` | `market/application/dto` |
  | `MarketMinuteClosedEvent` | `market/application/dto` |
  | `MarketHistoryFinalizedEvent` | `market/application/dto` |
  | `RealtimeMarketCandleUpdate` | `market/application/dto` |
  | `RealtimeMarketTickerUpdate` | `market/application/dto` |
  | `RealtimeMarketTradeTick` | `market/application/dto` |
  | `MarketRealtimeSourceSnapshot` | `market/application/dto` |
  | `MarketHistoryPersistenceResult` | `market/application/dto` |
  | `MarketHistoryPersistenceStatus` | `market/application/dto` |
  | `MarketRealtimeHealth` | `market/application/dto` |
  | `MarketRealtimeSourceType` | `market/application/dto` |
  | `MarketRealtimeReconnectState` | `market/application/dto` |
  | `MarketPriceMovementDirection` | `market/application/dto` |
- Classification rule: if a moved type only carries event/payload data, it goes to `dto`; if it owns product state transition rule, pause for domain classification.
- Tests:
  ```bash
  cd backend
  ./gradlew test --tests '*ProviderMarketRealtimeEventBridgeTest' --tests '*MarketMinuteCandleHistoryListenerTest' --tests '*RealtimeMarketCandleProjectorTest' --tests '*RealtimeMarketSummaryProjectorTest' --tests '*MarketStream*' --tests '*MarketSse*' --console=plain
  ./gradlew check --console=plain
  ```
- Stop condition: event publication timing, field semantics, or listener transaction behavior changes.

### Market PR-10A Lifecycle And Job Boundary

- Scope: Move runtime/lifecycle trigger responsibility out of `market/application/realtime` before ordinary collaborator moves.
- Non-goals: changing history persistence data, repair queue semantics, provider websocket low-level runtime.
- Exact planned moves:
  | Source | Target |
  | --- | --- |
  | `DelayedClosedMinuteCandlePersistenceScheduler` | split: scheduling trigger/config in `app/feature/market/job`; core delayed persistence coordinator in `market/application/implement` |
  | `MarketHistoryStartupBackfill` | `market/application/repair`; startup trigger remains in `app/feature/market/job` |
  | `CurrentMarketCandleBootstrapper` | core bootstrap coordinator in `market/application/implement`; trigger remains in stream/app boundary if one exists |
- Tests:
  ```bash
  cd backend
  ./gradlew test --tests '*MarketHistoryStartupBackfillTest' --tests '*CurrentMarketCandleBootstrapperTest' --tests '*MarketStartupReadyEventIntegrationTest' --tests '*MarketHistoryPersistenceCoordinatorTest' --tests '*MarketSchedulingConfigurationTest' --tests '*MarketRealtimeRefreshSchedulerTest' --tests '*MarketHistoryStartupBackfillReadyEventListenerTest' --tests '*MarketMinuteBoundaryEventPublisherTest' --console=plain
  ./gradlew check --console=plain
  ```
- Stop condition: delayed persistence timing, startup warmup/backfill, or transaction boundary changes.

### Market PR-10B Application Collaborators

- Scope: Move non-lifecycle market execution collaborators to `implement`, `history`, `query`, or `service`.
- Non-goals: provider runtime ownership, stream fan-out behavior, candle/history data semantics.
- Exact planned moves:
  | Source | Target |
  | --- | --- |
  | `CompletedHourlyCandleBuilder` | `market/application/implement` |
  | `MarketFundingScheduleLookup` | `market/application/implement` |
  | `MarketHistoryPersistenceCoordinator` | `market/application/history` |
  | `MarketHistoryRecorder` | `market/application/history` |
  | `MarketMinuteCandleHistoryListener` | `market/application/history` |
  | `MarketRealtimeFreshnessPolicy` | `market/application/implement` |
  | `RealtimeMarketCandleState` | `market/application/implement` |
  | `MarketSnapshotStore` | `market/application/implement` |
  | `MarketSupportedMarketRefresher` | `market/application/implement` |
  | `MarketTradePriceMovementPublisher` | `market/application/implement` |
  | `RealtimeMarketCandleProjector` | `market/application/implement` |
  | `RealtimeMarketCandleUpdateService` | `market/application/service` |
  | `RealtimeMarketDataStore` | `market/application/implement` |
  | `RealtimeMarketSummaryProjector` | `market/application/implement` |
- Tests:
  ```bash
  cd backend
  ./gradlew test --tests '*MarketRealtime*' --tests '*RealtimeMarket*' --tests '*MarketHistory*' --tests '*MarketSchedulingConfigurationTest' --tests '*MarketMinuteBoundaryEventPublisherTest' --tests '*MarketStream*' --tests '*MarketSse*' --console=plain
  ./gradlew check --console=plain
  ```
- Stop condition: provider lifecycle, stream fan-out, stale freshness, or current candle persistence behavior changes.

### Market PR-10C Provider Bridge And Read Facade

- Scope: Resolve remaining provider bridge/read facade ownership without service-to-service dependency.
- Non-goals: low-level provider websocket mechanics, market API response shape, order fill algorithm.
- Exact planned moves:
  | Source | Target |
  | --- | --- |
  | `ProviderMarketRealtimeEventBridge` | `market/application/service` unless PR-0A proves provider-owned lifecycle exception |
  | `MarketRealtimeFeed` | split: read facade in `market/application/query` or `market/application/implement`; refresh coordinator separate |
  | `RealtimeMarketPriceReader` | `market/application/query` or `market/application/implement`; never `service` |
- Tests:
  ```bash
  cd backend
  ./gradlew test --tests '*ProviderMarketRealtimeEventBridgeTest' --tests '*CreateOrderServiceTest' --tests '*OrderPendingFillProcessorTest' --tests '*GetMarketCandlesServiceTest' --tests '*MarketControllerTest' --tests '*MarketStream*' --tests '*MarketSse*' --console=plain
  ./gradlew check --console=plain
  ```
- Stop condition: provider runtime exception ambiguity, service-to-service dependency, or order fresh-price semantics changes.

## Verification Plan

현재 작업 환경은 shell command를 `rtk`로 감싸야 한다. downstream agent가 Codex native hook 환경에서 실행한다면 아래 Gradle 명령은 예를 들어 `rtk bash -lc 'cd backend && ./gradlew architectureLint --console=plain'` 형태로 실행한다. attached tmux/OMX 환경에서 RTK guard가 없으면 원래 명령 그대로 실행해도 된다.

Minimum per PR:

```bash
cd backend
./gradlew architectureLint --console=plain
```

DTO-only PR targeted tests:

```bash
cd backend
./gradlew test --tests '*Member*' --console=plain
./gradlew test --tests '*Leaderboard*' --console=plain
./gradlew test --tests '*Account*' --console=plain
./gradlew test --tests '*Reward*' --console=plain
./gradlew test --tests '*PositionPeek*' --console=plain
```

Position realtime PR:

```bash
cd backend
./gradlew test --tests '*OpenPositionBook*' --tests '*OrderPendingFillProcessorTest*' --tests '*OrderPositionLiquidationProcessorTest*' --tests '*OrderPositionTakeProfitStopLossProcessorTest*' --console=plain
./gradlew check --console=plain
```

Market PRs:

```bash
cd backend
./gradlew test --tests '*Market*' --console=plain
./gradlew check --console=plain
```

Final residue check:

```bash
find backend/core/src/main/java/coin/coinzzickmock/feature -type f | grep -E '/application/(command|result|realtime)/'
```

Expected final output: no lines.

## Risks And Mitigations

- Risk: Package move accidentally changes behavior through opportunistic cleanup.
  - Mitigation: PR checklist bans logic edits; any non-import logic change must be justified or split.
- Risk: Market split creates too much import churn.
  - Mitigation: split market into DTO, event DTO, implement/service, final ratchet PRs.
- Risk: Position realtime split breaks order pending fill flow.
  - Mitigation: run order pending fill/liquidation/TP-SL tests in PR-7.
- Risk: DTO-only PRs miss web/stream compile imports.
  - Mitigation: run `architectureLint` plus targeted tests; run full `check` when touched imports cross app/stream/storage.
- Risk: Old package residue remains because domain-specific purpose package is legitimate.
  - Mitigation: only `command`, `result`, `realtime` are mandatory removal targets. Other purpose packages are reviewed by docs criteria.

## ADR

### Decision

Adopt a staged domain-by-domain migration queue using `order` as canonical convention. Start with small DTO-only domains, then medium DTO domains, then `position` realtime split, and finish with a split `market` migration.

### Drivers

- Reviewability over speed.
- Behavior preservation over structural purity.
- Downstream agent autonomy without repeated interviews.

### Alternatives considered

- Big-bang migration across all domains.
- Largest-residue-first migration starting with `market`.
- DTO-only global sweep followed by a separate realtime wave.

### Why chosen

The chosen queue proves the checklist on small PRs first and delays the highest-risk `market` work until convention, tests, and stop gates have been exercised. It also keeps `market` explicitly scheduled so the hardest work is not forgotten.

### Consequences

- Old and new package conventions coexist for several PRs.
- Import churn is repeated across PRs, but each review stays tractable.
- Market needs at least four PRs and likely broader verification than smaller domains.

### Follow-ups

- After PR-11, consider adding an `architectureLint` strict rule for new `application/command`, `application/result`, and `application/realtime` sources.
- If market lifecycle ownership remains ambiguous, create a dedicated market realtime architecture spec before final strict rule.

## Available-Agent-Types Roster

Native Codex subagents available in this session:

- `explorer`: read-only codebase facts, residue inventory, affected-test lookup.
- `worker`: bounded implementation in disjoint file ownership.
- `default`: general planning, integration review, final synthesis.

OMX workflow lanes available by skill catalog:

- `$ralph`: single-owner autonomous implementation and verification loop.
- `$team`: coordinated parallel implementation across scoped workers.
- `$ultragoal`: durable goal-mode execution for the whole queue.
- `$autoresearch-goal`: research validation mode; not primary here because this is implementation planning, not open research.
- `$performance-goal`: optimization mode; not primary here because no latency/throughput target is being optimized.

## Follow-up Staffing Guidance

### `$ralph` path

Recommended for PR-1 through PR-5 and PR-8.

- One `$ralph` lane per PR.
- Reasoning: medium for DTO-only PRs, high for PR-7 and all market PRs.
- Handoff prompt should include this plan path, exact PR mini spec, residue before/after requirement, and verification commands.
- Ralph must stop on any stop gate and report instead of widening scope.

### `$team` path

Recommended for PR-9 through PR-11 if speed is needed.

- Worker 1: DTO/event move owner, medium reasoning.
- Worker 2: service/implement classification owner, high reasoning.
- Worker 3: test/import repair owner, medium reasoning.
- Explorer: final residue and affected-test inventory, low reasoning.
- Integrator/default: merge worker outputs, run verification, write final report.

Launch hints:

```bash
omx team ".omx/plans/2026-05-16-core-domain-refactor-wave-ralplan.md PR-9 market realtime event DTO split"
omx team ".omx/plans/2026-05-16-core-domain-refactor-wave-ralplan.md PR-10A market lifecycle job boundary split"
omx team ".omx/plans/2026-05-16-core-domain-refactor-wave-ralplan.md PR-10B market application collaborator split"
omx team ".omx/plans/2026-05-16-core-domain-refactor-wave-ralplan.md PR-10C market provider bridge read facade cleanup"
```

Alternative chat command:

```text
$team .omx/plans/2026-05-16-core-domain-refactor-wave-ralplan.md --focus PR-10A
```

Team verification path:

- Team proves no owned residue remains for the PR scope, targeted tests pass, and `architectureLint` passes.
- Ralph or default integrator verifies full diff reviewability, stop gates, and final `./gradlew check` for market/position waves before shutdown.

## Goal-Mode Follow-up Suggestions

- Use `$ultragoal .omx/plans/2026-05-16-core-domain-refactor-wave-ralplan.md` if the whole queue should become a durable tracked execution program.
- Use `$autoresearch-goal` only if market realtime ownership becomes an unresolved research/design question.
- Use `$performance-goal` only if later market refactor introduces measurable startup, stream, or candle update performance goals.

## Changelog

- Initial consensus draft created from `.omx/specs/deep-interview-core-domain-refactor-wave.md`.
- Incorporated order convention and current residue inventory.
- Added PR queue, mini specs, stop gates, verification plan, ADR, and follow-up staffing guidance.
- Architect feedback 반영: PR-0A 추가, lifecycle classification lock, market 35-file table, position hydrator split stop gate.
- Critic feedback 반영: reference documents header, position hydrator mini spec inconsistency resolved, market PR-9/10A/10B/10C source-to-target tables, concrete Gradle verification commands.
- PR-0 반영: ignored `.omx` plan artifact를 기준 artifact로 force-add 대상으로 삼고, 2026-05-17 residue baseline command/summary/file list를 기록.
- PR-0A 반영: `application/realtime`을 legacy migration residue로 확정하고 position/market realtime target package 및 lifecycle/job split 결정을 잠금.
