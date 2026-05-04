# Leaderboard Rank Ordering Refactor Plan

## 목적 / 큰 그림

PR #95 리뷰에서 확인한 랭킹 정렬 회귀를 바로잡는다.
목표는 Redis snapshot 조회를 우선하되, DB fallback의 로그인 사용자 내 순위를 단순하고 예측 가능한 보정 경로로 정리하는 것이다.
완료 후 `/api/futures/leaderboard`는 Redis snapshot이 불완전할 때 조용한 부분 응답 대신 관측 가능한 로그와 DB fallback으로 복구하고, DB fallback의 `myRank`는 "나보다 score가 높은 active member 수 + 1"로 계산한다.

## 진행 현황

- [x] PR #95 리뷰 스레드와 변경 파일 확인
- [x] 제품/백엔드 설계 문서 최신화
- [x] 리팩토링 계획 초안 작성
- [x] 구현
- [x] 테스트
- [x] review 스킬 기반 검토 확인
- [x] 작업 종료 처리

## 놀라움과 발견

- `findSnapshot`은 Redis에서 `limit`개만 읽은 뒤 서비스 comparator로 다시 정렬한다. 다만 제품 순위 의미에서 동점 tie-break를 세분화하지 않기로 했으므로, boundary 동점 후보 확장은 이번 범위에서 제외한다.
- DB fallback의 `myRank`는 목록 comparator를 재현할 필요가 없다. fallback은 말 그대로 Redis 장애/미집계 시 보정 경로이므로 "내 score보다 높은 사람 수 + 1"만 계산한다.
- `LeaderboardSnapshotStoreAdapter`는 public store 구현체인데도 `limit <= 0`을 자체 방어하지 않아 Redis range end가 `-1`이 되는 경로가 있다.
- 기존 `LeaderboardSnapshotStoreAdapterTest`는 source string 포함 여부를 확인하는 수준이라 limit guard, missing hash, Redis rank 조회 계약을 보호하지 못한다.

## 의사결정 기록

- Redis ZSET은 순위 의미의 원본이 아니라 DB 기반 파생 snapshot이다.
- 동점자는 제품 순위에서 별도 tie-break로 세분화하지 않는다.
- DB fallback의 로그인 사용자 내 순위는 mode score 기준으로 `count(score > myScore) + 1`만 계산한다.
- Redis snapshot 후보의 member hash가 누락되거나 파싱되지 않으면 부분 랭킹을 반환하지 않는다. warning 로그를 남기고 DB fallback을 사용한다.
- `Optional`은 반환 타입에만 사용하고 public/service/store 파라미터로 전파하지 않는다. 로그인 사용자가 없으면 nullable `Long` 또는 명시적 overload로 표현한다.

## 범위

- 이번에 하는 것(in scope):
  - `GetLeaderboardService`의 `Optional<Long>` 파라미터 제거
  - DB fallback `myRank`를 tie-break 없이 `count(score > myScore) + 1`로 계산
  - `LeaderboardSnapshotStore`의 snapshot 조회 계약을 `findSnapshot(mode, limit, currentMemberId)` 중심으로 단순화
  - `LeaderboardSnapshotStoreAdapter`의 중복 snapshot read 경로 통합
  - `limit <= 0` guard, missing hash warning 및 fallback 조건 추가
  - application/store 단위 테스트를 실제 행위 기반 테스트로 보강
- 이번에 하지 않는 것(out of scope):
  - Redis key format을 전면 변경해 composite score를 도입하는 일
  - 랭킹 화면 UI 변경
  - DB schema 변경
- 후속 작업(선택):
  - 제품에서 동점자 사이의 deterministic ordering까지 순위 의미로 요구하게 되면 Redis key/version `v4`에서 tie-break 친화적인 인덱스 설계를 별도 ADR로 검토한다.

## 요구 사항 요약

- 기능 요구 사항:
  - DB fallback `myRank`는 현재 member score보다 높은 active member 수에 1을 더해 계산한다.
  - 동점자는 내 순위 계산에서 위 사람으로 세지 않는다.
  - Redis snapshot이 불완전하면 DB fallback으로 응답한다.
- 비기능 요구 사항:
  - public adapter 경계에서 잘못된 limit을 방어한다.
  - Redis 불일치 상황은 warning 로그로 관측 가능해야 한다.
  - 기존 `findTop(mode, limit, tieSlack)`처럼 호출자가 Redis 보정 폭을 알아야 하는 계약은 제거한다.

## 맥락과 길잡이

- 관련 문서:
  - [backend architecture foundations](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/01-architecture-foundations.md)
  - [backend testing and lint](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/05-testing-and-lint.md)
  - [screen spec](/Users/hj.park/projects/coin-zzickmock/docs/product-specs/coin-futures-screen-spec.md)
- 관련 코드 경로:
  - [GetLeaderboardService.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/leaderboard/application/service/GetLeaderboardService.java)
  - [LeaderboardSnapshotStore.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/leaderboard/application/store/LeaderboardSnapshotStore.java)
  - [LeaderboardSnapshotStoreAdapter.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/leaderboard/infrastructure/store/LeaderboardSnapshotStoreAdapter.java)
  - [GetLeaderboardServiceTest.java](/Users/hj.park/projects/coin-zzickmock/backend/src/test/java/coin/coinzzickmock/feature/leaderboard/application/service/GetLeaderboardServiceTest.java)
  - [LeaderboardSnapshotStoreAdapterTest.java](/Users/hj.park/projects/coin-zzickmock/backend/src/test/java/coin/coinzzickmock/feature/leaderboard/infrastructure/store/LeaderboardSnapshotStoreAdapterTest.java)

## 작업 계획

1. `GetLeaderboardService`에서 public `Optional<Long>` 파라미터를 제거한다.
2. `findDatabaseMyRank`는 `currentMemberId == null`이면 empty를 반환하고, 그 외에는 현재 member entry를 찾은 뒤 `mode.score(other) > mode.score(me)`인 active member 수에 1을 더한다.
3. `LeaderboardSnapshotStore`에서 `findTop(mode, limit, tieSlack)`을 제거하거나 default-only compatibility에서 없애고, snapshot 조회 계약을 `findSnapshot(mode, limit, Long currentMemberId)`로 모은다.
4. `LeaderboardSnapshotStoreAdapter`는 active version을 한 번 읽은 뒤 top-N 후보와 myRank를 같은 version에서 계산한다.
5. hydrate 과정에서 missing hash 또는 JSON parse failure가 발생하면 member id와 version을 warning으로 남기고 snapshot empty를 반환해 DB fallback이 열리게 한다.
6. `limit <= 0` guard를 adapter 입구에 둔다.
7. 테스트를 source string 검사에서 행위 기반으로 바꾼다.

## 구체적인 단계

1. Red tests:
   - DB fallback에서 동점자는 위 사람으로 세지 않고, 더 높은 score를 가진 사람만 `myRank`에 반영하는지 검증한다.
   - `limit <= 0`이 Redis 전체 range 조회로 이어지지 않는지 검증한다.
   - missing member hash가 있으면 snapshot을 반환하지 않고 fallback 가능한 empty를 반환하는지 검증한다.
2. Green implementation:
   - service/store signatures와 fake 구현을 정리한다.
   - adapter 중복 read path를 통합한다.
   - limit guard와 missing hash handling을 구현한다.
3. Refactor:
   - `Optional.get()`과 `Optional` 파라미터를 제거한다.
   - comparator 이름을 순위 의미가 드러나도록 정리한다.
   - 로그 메시지에 `version`, `memberKey`, `mode`를 포함한다.
4. Verification:
   - focused leaderboard tests
   - backend `architectureLint`
   - 필요 시 backend `check`

## 수용 기준

- `GetLeaderboardService` public method 또는 store 조회 계약에 `Optional` 파라미터가 남지 않는다.
- DB fallback의 `myRank`는 `count(score > myScore) + 1`로 계산된다.
- DB fallback에서 같은 score의 동점자는 내 위 사람으로 세지 않는다.
- `limit <= 0`은 adapter에서 `Optional.empty()` 또는 명시적 guard 결과로 끝나며 Redis 전체 range를 조회하지 않는다.
- member hash 누락 또는 parse 실패가 있으면 warning 로그가 남고 DB fallback이 가능하다.
- 리뷰 finding 1은 "동점자는 순위 의미에서 세분화하지 않는다"는 제품 결정으로 재분류한다. findings 2~3은 구현과 테스트로 닫는다.

## 위험과 완화

- 위험: DB fallback 내 순위가 Redis의 exact same-score ordering과 다를 수 있다.
  - 예방: 제품 문서에 fallback은 `count(score > myScore) + 1` 기준이라고 명시한다.
  - 완화: Redis가 정상일 때는 Redis snapshot 경로를 우선 사용한다.
  - 복구: exact tie ordering이 제품 요구가 되면 별도 ADR로 Redis/DB tie-break 인덱스를 설계한다.
- 위험: Redis myRank와 top-N 목록이 다른 version을 읽을 수 있다.
  - 예방: active pointer를 한 번 읽고 같은 version을 모든 Redis read에 넘긴다.
  - 완화: 중간에 hash 누락이 발견되면 snapshot empty로 처리한다.
  - 복구: DB fallback으로 응답한다.
- 위험: 테스트가 Redis 서버에 의존하면 느리고 불안정해진다.
  - 예방: adapter 계약은 mock/fake RedisOperations 또는 slice 테스트 중 가장 가벼운 경로로 고정한다.
  - 완화: 실제 Redis 통합 검증은 별도 profile 작업으로 분리한다.

## 검증 절차

- 실행 명령:
  - `./gradlew test --tests coin.coinzzickmock.feature.leaderboard.application.service.GetLeaderboardServiceTest --tests coin.coinzzickmock.feature.leaderboard.infrastructure.store.LeaderboardSnapshotStoreAdapterTest`
  - `./gradlew architectureLint --console=plain`
  - `./gradlew check`
- 기대 결과:
  - focused tests와 lint가 통과한다.
  - `check`가 기존 회귀 없이 통과한다.
- 실패 시 확인할 것:
  - DB fallback이 `>=`로 동점자를 위 사람으로 세고 있지 않은지 확인한다.
  - profitRate mode에서 비교 score가 wallet balance 파생 수익률과 일관되는지 확인한다.
  - missing hash를 부분 응답으로 허용하는 테스트 double이 남아 있는지 확인한다.

## 산출물과 메모

- PR #95 review findings:
  - Redis top-N 후보가 동점 정렬 전에 잘릴 수 있음
    - 처리 방향: 동점 tie-break를 순위 의미로 보지 않기로 했으므로 boundary 동점 후보 확장은 이번 구현에서 제외한다.
  - DB myRank 정렬 기준이 목록 정렬과 다름
    - 처리 방향: DB fallback `myRank`는 목록 정렬을 재현하지 않고 `count(score > myScore) + 1`로 계산한다.
  - Adapter 경계에서 limit guard가 없음
    - 처리 방향: adapter 입구 guard와 회귀 테스트를 추가한다.
- 문서 최신화는 이 계획과 함께 제품 명세/백엔드 설계 문서에 반영했다.
