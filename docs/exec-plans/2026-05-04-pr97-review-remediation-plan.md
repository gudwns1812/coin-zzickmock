# PR #97 리뷰 반영 및 문서 불일치 재발 방지 계획

이 계획서는 `PLANS.md` 규칙을 따른다.

## 목적 / 큰 그림

PR #97 `Add daily futures account refill`에 남은 리뷰를 단순 라인 수정으로 처리하지 않고, 리뷰 의도와 backend 설계 문서를 기준으로 다시 맞춘다. 특히 리필 상태 영속성, JPA 엔티티 생성자 보일러플레이트, reward 예외 일관성, 복잡 주문 조회를 문서 원문과 대조해 구현한다.

완료 후에는 PR #97의 unresolved review thread가 "왜 문서와 다르게 구현했는지"에 답할 수 있는 형태로 정리되고, review 스캐폴딩은 Stage 0에서 governing document cross-check를 먼저 수행한다.

## 진행 현황

- [x] review 스캐폴딩 Stage 0 누락 확인
- [x] `skill edit code-review` 절차로 `code-review` 스킬과 `code-reviewer` 에이전트에 Stage 0 추가
- [x] 사용자 피드백 반영: Lombok은 JPA no-args 생성자용 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`로 정정
- [x] 사용자 피드백 반영: upsert는 제거 대상이 아니라 `insert ignore` / `on duplicate key update` / locked update 중 의도별 선택 대상으로 정정
- [x] 계획 문서 작성
- [x] 사용자 승인
- [x] PR #97 코드 수정
- [x] 테스트 및 검증
- [x] review 스킬 기반 재검토
- [x] architect verification
- [x] deslop pass 및 재검증
- [x] 후속 사용자 지적 반영: refill state native SQL을 Spring Data `nativeQuery`에서 `JdbcTemplate` 경계로 이동
- [x] 작업 종료 처리

## 놀라움과 발견

- review Stage 0은 `plan`, `ralplan`, `critic`에는 반영됐지만 실제 코드 리뷰 실행 표면인 `.codex/skills/code-review/SKILL.md`와 `.codex/agents/code-reviewer.toml`에는 빠져 있었다.
- Lombok 리뷰 의도는 Spring-managed 생성자 주입용 `@RequiredArgsConstructor`가 아니라 JPA 엔티티 protected no-args 생성자 보일러플레이트를 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`로 대체하라는 의미였다.
- upsert 관련 리뷰 의도는 "upsert를 무조건 제거"가 아니라, daily 기본 row 보장과 포인트 상품 추가 지급처럼 다른 mutation 의도에 맞는 SQL을 선택하라는 것이다.
- native SQL 리뷰 의도는 SQL semantics만 맞추는 것이 아니라, Spring Data repository의 `@Query(nativeQuery = true)` 대신 persistence 구현체의 `JdbcTemplate`에 SQL을 두라는 기술 경계까지 포함한다.

## 의사결정 기록

- 결정: review 스캐폴딩은 Stage 0을 Stage 1 spec compliance보다 앞에 둔다.
  근거: 문서 원문을 읽지 않은 상태에서 spec/code quality를 검토하면 같은 오해가 반복된다.
  날짜/작성자: 2026-05-04 / Codex

- 결정: `AccountRefillStateEntity`의 수동 protected no-args 생성자는 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`로 대체한다.
  근거: 리뷰 의도는 JPA 엔티티 no-args 생성자 보일러플레이트를 Lombok으로 줄이는 것이다.
  날짜/작성자: 2026-05-04 / Codex

- 결정: upsert 계열 SQL은 금지하지 않고 mutation 의도별로 선택한다.
  근거: 같은 unique key 기반 mutation이라도 no-op create, insert-or-increment, locked consume은 서로 다른 계약이다.
  날짜/작성자: 2026-05-04 / Codex

- 결정: refill state의 `INSERT IGNORE`/`ON DUPLICATE KEY UPDATE` SQL은 Spring Data native query가 아니라 `JdbcTemplate`으로 실행한다.
  근거: backend persistence 문서는 JPA/QueryDSL로 유지하기 어려운 native SQL을 `JdbcTemplate` 경계에 두도록 한다.
  날짜/작성자: 2026-05-04 / Codex

## 결과 및 회고

- `AccountRefillStateEntity`는 수동 protected no-args 생성자 대신 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`를 사용한다.
- refill state repository 계약은 daily no-op provision, extra count grant, locked consume으로 분리했다.
- daily provision은 `insert ignore`, extra refill grant는 `on duplicate key update remaining_count = remaining_count + :count`를 사용한다.
- 위 native SQL은 `AccountRefillStateEntityRepository`의 `@Query(nativeQuery = true)`가 아니라 `AccountRefillStatePersistenceRepository`의 `JdbcTemplate.update(...)`로 실행한다.
- reward domain/application invalid path는 `CoreException(ErrorCode.INVALID_REQUEST, message)` 중심으로 정리했고 application service의 normal-flow try/catch 번역을 제거했다.
- pending limit order 조회는 JPQL string에서 `JPAQueryFactory`/`QFuturesOrderEntity` 기반 QueryDSL 조회로 옮겼다.
- `./gradlew test --console=plain`과 `./gradlew check --console=plain`이 통과했다.
- review 스킬 기반 재검토에서 지적된 native upsert metadata 문제를 반영해 duplicate branch에서 `version`과 `updated_at`도 갱신하도록 고쳤다.
- architect verification에서 지적된 `datePolicy.today()` 이중 호출과 `db-schema.md` 불일치를 반영했고, 최종 architect 재검증은 승인됐다.
- deslop pass에서는 변경 파일 범위 안에서 중복 `toDomain()` 반환과 import 순서만 정리했고, post-deslop `./gradlew check --console=plain`이 통과했다.

## 범위

- 이번에 하는 것(in scope):
  - PR #97 unresolved review thread를 사용자 의도 기준으로 재해석하고 반영한다.
  - review Stage 0 스캐폴딩 변경과 PR #97 코드 변경을 분리해 다룬다.
  - 리필 상태 영속성 계약을 create / grant / locked read-update로 분리한다.
  - JPA 엔티티 no-args 생성자 보일러플레이트를 Lombok으로 정리한다.
  - reward domain/application 예외 일관성을 한 번에 정리한다.
  - 복잡한 pending order 조회를 QueryDSL 쪽으로 옮긴다.

- 이번에 하지 않는 것(out of scope):
  - architectureLint 신규 규칙 추가
  - PR #97 범위를 벗어난 reward 전체 재설계
  - 스키마가 필요하지 않은 경우의 불필요한 DB migration

- 후속 작업(선택):
  - JPA 엔티티 no-args 생성자 Lombok 관례를 backend 설계 문서에 명시적으로 추가한다.
  - upsert 계열 SQL 선택 기준을 persistence 설계 문서에 보강한다.

## 요구 사항 요약

- 기능 요구 사항:
  - daily refill state는 기본 리필권, 추가 리필권, 소비 경로를 구분한다.
  - reward shop refill item은 필요한 경우 기존 row에 count를 증가시킬 수 있다.
  - pending limit order 조회 동작은 기존 semantics를 유지한다.

- 비기능 요구 사항:
  - governing docs를 먼저 읽고 문서 대조표를 작성한 뒤 구현한다.
  - 조회와 생성을 섞는 repository 계약을 피한다.
  - "upsert 제거" 같은 단순 규칙 대신 mutation 의도를 드러낸다.
  - 테스트와 `rg` guard로 회귀를 확인한다.

## 맥락과 길잡이

- 관련 문서:
  - `BACKEND.md`
  - `docs/design-docs/backend-design/02-package-and-wiring.md`
  - `docs/design-docs/backend-design/04-domain-modeling-rules.md`
  - `docs/design-docs/backend-design/06-persistence-rules.md`
  - `docs/design-docs/backend-design/07-clean-code-responsibility.md`
  - `docs/generated/db-schema.md`
  - `.omx/wiki/docs--exec-plans--completed--2026-04-17-backend-tech-naming-and-lombok-alignment.md`
  - `.omx/wiki/docs--exec-plans--completed--2026-04-17-market-history-latest-price-persistence.md`

- 관련 코드 경로:
  - `backend/src/main/java/coin/coinzzickmock/feature/account/infrastructure/persistence/AccountRefillStateEntity.java`
  - `backend/src/main/java/coin/coinzzickmock/feature/account/infrastructure/persistence/AccountRefillStateEntityRepository.java`
  - `backend/src/main/java/coin/coinzzickmock/feature/account/infrastructure/persistence/AccountRefillStatePersistenceRepository.java`
  - `backend/src/main/java/coin/coinzzickmock/feature/account/application/service/RefillTradingAccountService.java`
  - `backend/src/main/java/coin/coinzzickmock/feature/reward/**`
  - `backend/src/main/java/coin/coinzzickmock/feature/order/infrastructure/persistence/FuturesOrderEntityRepository.java`
  - `backend/src/main/java/coin/coinzzickmock/feature/order/infrastructure/persistence/OrderPersistenceRepository.java`

- 선행 조건:
  - 스캐폴딩 변경과 PR #97 코드 변경을 같은 커밋으로 섞지 않는다.
  - 현재 uncommitted scaffolding 변경을 인지하고 PR 코드 수정 전에 작업 범위를 분리한다.

## 문서 원문 대조표

| 작업 영역 | 반드시 읽은 원문 문서 | 적용해야 하는 규칙 | 구현 선택 | 금지한 shortcut | 검증 방법 |
| --- | --- | --- | --- | --- | --- |
| review scaffolding | `.codex/skills/code-review/SKILL.md`, `.codex/agents/code-reviewer.toml`, `.codex/skills/ralplan/SKILL.md`, `.codex/skills/plan/SKILL.md` | Stage 0에서 governing docs와 최신 review intent를 먼저 확인한다. Stage 0이 막히면 Stage 1로 넘어가지 않는다. | `code-review`와 `code-reviewer`에 Stage 0을 추가했고, 출력 계약과 final checklist에도 반영했다. | `critic`/`plan`만 고치고 실제 review 표면도 고쳤다고 간주 | `rg "Stage 0|Governing Document Cross-check"`, TOML parse, `git diff --check` |
| JPA entity Lombok | `docs/design-docs/backend-design/02-package-and-wiring.md`, `.omx/wiki/docs--exec-plans--completed--2026-04-17-backend-tech-naming-and-lombok-alignment.md`, 사용자 최신 피드백 | 이번 리뷰의 Lombok 의미는 JPA 엔티티 protected no-args 생성자를 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`로 대체하는 것이다. Spring-managed `@RequiredArgsConstructor` 규칙과 섞지 않는다. | `AccountRefillStateEntity`의 수동 protected 생성자를 Lombok no-args annotation으로 대체한다. | JPA 엔티티 리뷰를 Spring bean 생성자 주입 문제로 재해석 | compile, entity test, `rg "protected AccountRefillStateEntity\\(\\)"` |
| refill state mutation SQL | `docs/design-docs/backend-design/06-persistence-rules.md`, `docs/design-docs/backend-design/07-clean-code-responsibility.md`, `docs/generated/db-schema.md`, `.omx/wiki/docs--exec-plans--completed--2026-04-17-market-history-latest-price-persistence.md` | create/update/read 계약을 분리한다. upsert는 금지어가 아니라 의도에 맞게 사용한다. 조회가 생성 부작용을 숨기지 않는다. JPA/QueryDSL로 표현하기 어려운 native SQL은 Spring Data native query가 아니라 `JdbcTemplate` 경계에 둔다. | daily 기본 row 보장은 `INSERT IGNORE` 우선 검토. extra refill grant는 필요하면 `ON DUPLICATE KEY UPDATE remaining_count = remaining_count + :count`. 두 native SQL은 persistence 구현체의 `JdbcTemplate.update(...)`로 실행한다. 소비는 locked read + domain mutation + dirty checking. | upsert 무조건 제거, `find-create-find`, `ensureBy...`로 read/create 섞기, no-op인데 `on duplicate update col = col` 남발, Spring Data `@Query(nativeQuery = true)`에 mutation SQL 유지 | repository/application tests, SQL `rg`, duplicate race test |
| refill consume concurrency | `docs/design-docs/backend-design/06-persistence-rules.md`, `docs/generated/db-schema.md` | 차감/소비는 동시성 제어가 필요하므로 locked row를 기준으로 domain invariant를 적용한다. | `findByMemberIdAndRefillDateForUpdate`로 잠근 뒤 `remainingCount` 차감. 저장은 managed entity dirty checking을 기본으로 한다. | SQL upsert로 소비 처리, generic `save` fallback으로 insert/update 섞기 | 동시 차감 테스트, remaining count 음수 방지 테스트 |
| reward exception consistency | `docs/design-docs/backend-design/09-exception-rules.md`, `docs/design-docs/backend-design/04-domain-modeling-rules.md` | domain/application 실패는 `CoreException`/`ErrorCode` 중심으로 통일한다. `catch (Exception)` 계열은 경계 번역에만 둔다. | affected reward domain/application invalid path를 한 번에 정리하고 서비스의 try/catch normal flow를 제거한다. | `PurchaseShopItemService`만 고치고 domain `IllegalArgumentException`/`IllegalStateException` 방치 | invalid price/amount/out-of-stock/insufficient point/cancel/refund tests |
| pending order query | `docs/design-docs/backend-design/06-persistence-rules.md`, `docs/design-docs/backend-design/02-package-and-wiring.md` | 동적 조건 조합과 타입 세이프 조회는 QueryDSL을 기본 선택지로 사용한다. | `QFuturesOrderEntity` 기반 custom repository/adapter로 이전하고 `createdAt asc`, status, symbol, side, price semantics를 유지한다. | 긴 JPQL string에 복잡 조건 계속 추가 | query parity tests, repository tests |

대조표 점검:

- [x] 작업 영역별 원문 문서를 실제로 읽었다.
- [x] 제품/DB 문서와 설계 문서의 책임 차이를 구분했다.
- [x] 주변 코드 패턴이 원문 문서와 충돌할 때 어느 쪽을 우선할지 명시했다.
- [x] 금지한 shortcut을 구현 단계와 리뷰 단계에서 다시 확인할 수 있다.

## 작업 계획

1. 현재 uncommitted scaffolding 변경과 PR #97 코드 변경 범위를 분리한다.
2. PR #97 코드 수정 전에 이 문서의 대조표를 기준으로 review comment별 구현 선택을 확정한다.
3. `AccountRefillStateEntity`의 JPA no-args 생성자를 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`로 정리한다.
4. refill state repository/application 계약을 `provisionDailyStateIfAbsent`, `grantExtraRefillCount`, `findBy...ForUpdate` 식으로 의도별로 나눈다.
5. daily 기본 row 보장은 `INSERT IGNORE`를 우선 적용하되, duplicate 외 오류를 묻을 위험이 없는지 테스트로 확인한다.
6. extra refill grant가 기존 row 증가 의미라면 `ON DUPLICATE KEY UPDATE remaining_count = remaining_count + :count`를 사용한다.
7. refill 소비는 locked entity mutation과 dirty checking으로 처리한다.
8. reward 예외 일관성을 affected path 전체에서 정리한다.
9. pending order 복잡 조회를 QueryDSL custom repository/adapter로 옮긴다.
10. 필요한 경우 docs/schema를 갱신한다.
11. 테스트와 `rg` guard를 실행하고 review 스킬로 다시 검토한다.

## 구체적인 단계

1. PR #97 review thread와 관련 파일을 다시 읽어 코멘트별 대상 파일/라인을 고정한다.
2. JPA no-args Lombok 변경:
   - `AccountRefillStateEntity`에 `lombok.AccessLevel`, `lombok.NoArgsConstructor` import를 추가한다.
   - 클래스에 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`를 붙인다.
   - 수동 protected 생성자를 삭제한다.
3. refill persistence 변경:
   - repository port 이름에서 `ensureBy...`를 제거한다.
   - no-op create는 `provisionDailyStateIfAbsent`로 명명한다.
   - count 증가는 `grantExtraRefillCount`로 명명한다.
   - 소비는 locked read 후 entity method로 차감한다.
4. reward exception 변경:
   - reward domain invalid path의 예외 타입과 ErrorCode를 표로 정한다.
   - application service의 normal-flow try/catch 번역을 제거한다.
5. order QueryDSL 변경:
   - custom repository 또는 adapter surface를 정한다.
   - 기존 JPQL 조건과 ordering을 QueryDSL predicate/order로 옮긴다.
6. 테스트를 추가/수정한다.
7. `rg` guard와 Gradle 검증을 실행한다.
8. 이 문서 진행 현황과 결과를 갱신한다.

## 수용 기준(테스트 가능한 형태)

- `AccountRefillStateEntity`는 수동 protected no-args 생성자 대신 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`를 사용한다.
- daily refill state 기본 row 보장은 no-op create 의도를 드러내며, duplicate path가 count를 변경하지 않는다.
- extra refill grant는 기존 row가 있을 때 count 증가 semantics를 테스트로 보장한다.
- refill state native SQL은 Spring Data `nativeQuery`가 아니라 `JdbcTemplate`에서 실행된다.
- refill 소비 경로는 locked read와 domain mutation을 사용하고, `remainingCount`가 음수가 되지 않는다.
- repository 계약명에서 조회와 생성을 섞는 `ensureBy...` 류 naming이 제거된다.
- `find-create-find` 대체 구현이 없다.
- reward invalid path는 일관된 `CoreException`/`ErrorCode` 결과를 낸다.
- pending order 조회는 QueryDSL 기반으로 동작하며 기존 조건과 정렬을 유지한다.
- `code-review` Stage 0이 실제 리뷰 출력에 governing document cross-check 또는 blocking finding을 포함한다.

## 위험과 완화

- 위험 시나리오 1: `INSERT IGNORE`가 duplicate 외 오류까지 숨긴다.
  - 예방: 입력값과 non-null 제약을 application/domain에서 먼저 보장한다.
  - 완화: affected repository test에서 정상 duplicate no-op과 비정상 입력을 구분한다.
  - 복구: no-op create가 부적합하면 explicit insert + duplicate key translation으로 전환한다.

- 위험 시나리오 2: `ON DUPLICATE KEY UPDATE`가 도메인 검증을 우회해 count를 잘못 증가시킨다.
  - 예방: application에서 증가량과 권한을 먼저 결정하고 SQL은 이미 결정된 persistence 변경만 적용한다.
  - 완화: grant tests에서 증가량, 중복 지급, 음수/0 증가량을 검증한다.
  - 복구: grant 경로를 locked entity mutation으로 전환한다.

- 위험 시나리오 3: reward 예외 정리가 PR 범위를 과도하게 키운다.
  - 예방: review comments가 닿은 reward domain/application path로 범위를 제한한다.
  - 완화: ErrorCode 표를 먼저 만들고 테스트를 그 표에 맞춘다.
  - 복구: PR #97과 무관한 reward admin 확장은 후속으로 분리한다.

## 검증 절차

- 실행 명령:
  - `rg "Stage 0|Governing Document Cross-check" .codex/skills/code-review/SKILL.md .codex/agents/code-reviewer.toml`
  - `rg "protected AccountRefillStateEntity\\(" backend/src/main/java`
  - `rg "nativeQuery = true|@Modifying|insertDailyStateIfAbsent|insertOrAddRefillCount" backend/src/main/java/coin/coinzzickmock/feature/account/infrastructure/persistence/AccountRefillStateEntityRepository.java backend/src/main/java/coin/coinzzickmock/feature/account/infrastructure/persistence/AccountRefillStatePersistenceRepository.java`
  - `rg "ensureBy|on duplicate key update remaining_count = remaining_count|findWithLockingByMemberIdAndRefillDate\\(.*\\).*orElseGet" backend/src/main/java/coin/coinzzickmock/feature/account`
  - `cd backend && ./gradlew test --console=plain`
  - `cd backend && ./gradlew check --console=plain`

- 기대 결과:
  - Stage 0 문구가 review skill/agent 양쪽에 남아 있다.
  - `AccountRefillStateEntity` 수동 no-args 생성자가 없다.
  - refill state native SQL은 `JdbcTemplate`으로 이동했고 Spring Data repository에 `nativeQuery = true` mutation이 없다.
  - refill repository에 read/create를 섞는 계약과 find-create-find 대체가 없다.
  - reward invalid path 테스트가 일관된 ErrorCode를 확인한다.
  - QueryDSL query parity 테스트가 통과한다.

- 실패 시 확인할 것:
  - `INSERT IGNORE`가 MySQL/H2 테스트 환경에서 동일하게 동작하는지
  - QueryDSL generated Q-type이 빌드에 생성되는지
  - reward 기존 테스트가 `IllegalArgumentException` 자체를 기대하고 있지 않은지

## 반복 실행 가능성 및 복구

- 반복 실행 시 안전성:
  - annotation 치환과 repository naming 변경은 컴파일 오류를 기준으로 빠진 참조를 보완할 수 있다.
  - SQL mutation 변경은 repository test를 기준으로 반복 검증한다.

- 위험한 단계:
  - refill count 증가 SQL
  - reward 예외 타입 전환
  - QueryDSL custom repository wiring

- 롤백 또는 재시도 방법:
  - 각 영역을 별도 commit 또는 최소한 별도 diff chunk로 유지한다.
  - 실패 시 Lombok, refill persistence, reward exception, QueryDSL 중 실패 영역만 되돌릴 수 있게 한다.

## 산출물과 메모

- 관련 로그:
  - `code-review` Stage 0 추가 검증: TOML parse 통과, `git diff --check` 통과
  - backend verification: `./gradlew test --console=plain` 통과
  - backend verification: `./gradlew check --console=plain` 통과
  - architect verification: APPROVE
  - post-deslop verification: `./gradlew check --console=plain` 통과
  - 후속 native SQL 경계 보정: target repository test 통과, `./gradlew check --console=plain` 통과, `git diff --check` 통과

- 남은 TODO:
  - 없음
