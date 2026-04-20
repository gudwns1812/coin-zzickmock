# 백엔드 도메인 로직 단계별 리팩토링

이 계획서는 [PLANS.md](/Users/hj.park/projects/coin-zzickmock/PLANS.md)
와 [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)를 따른다.
이 계획서는 살아 있는 문서입니다. `진행 현황`, `놀라움과 발견`, `의사결정 기록`, `결과 및 회고` 섹션은 작업이 진행되는 내내 최신 상태로 유지해야 합니다.

## 목적 / 큰 그림

현재 주문 체결과 포지션 청산뿐 아니라 회원가입/로그인 정규화와 마켓 히스토리 캔들 병합/집계 같은 핵심 계산도 `application`에 직접 들어 있어, 도메인 규칙이 서비스 오케스트레이션과 섞여 있다.
이 작업의 목적은 주문/포지션/계좌, 회원, 리워드, 마켓 히스토리 영역의 비즈니스 상태 전이와 계산을 `domain`으로 끌어올려, 서비스가 "무슨 일을 시작하는가" 중심으로 읽히게 만드는 것이다.

이 변경이 끝나면 사용자는 이전과 같은 API 결과를 받지만, 내부 구현은 각 application service가 계산기 역할을 덜 맡고 도메인 메서드와 정책을 조합하는 구조로 바뀐다.
이 효과는 관련 단위 테스트를 먼저 실패시키고 다시 통과시키는 `red -> green -> refactor` 루프와, 최종 `./gradlew architectureLint`, `./gradlew check`
통과로 확인한다.

## 진행 현황

- [x] (2026-04-19 17:05+09:00) 범위 초안 확정: `CreateOrderService`, `ClosePositionService`, `TradingAccount`,
  `PositionSnapshot`, 관련 테스트를 1차 리팩토링 대상으로 선택
- [x] (2026-04-19 17:12+09:00) 도메인 기준 문서 재정리
  완료: [04-domain-modeling-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/04-domain-modeling-rules.md)
  를 단일 원문으로 사용
- [x] (2026-04-19 17:28+09:00) 사용자 승인 완료
- [x] (2026-04-19 17:34+09:00) `red` 단계 1차 완료: 기존 포지션 추가 진입 시 평균 진입가 기준 청산가 재계산 실패 테스트 추가 후 `CreateOrderServiceTest` 실패
  확인
- [x] (2026-04-19 17:41+09:00) `green` 단계 완료: `TradingAccount`와 `PositionSnapshot`에 상태 전이 메서드 추가, `PositionCloseOutcome`
  도입, 서비스 계산을 domain 호출로 이동
- [x] (2026-04-19 17:43+09:00) `refactor` 단계 1차 완료: `CreateOrderService`, `ClosePositionService`를 시세 로드 후 domain 호출 중심
  오케스트레이션으로 단순화
- [x] (2026-04-19 17:44+09:00) 회귀 테스트 보강 완료: `ClosePositionServiceTest` 추가로 부분 청산 후 계좌/포지션/포인트 갱신 시나리오 고정
- [x] (2026-04-19 17:45+09:00) 검증 완료: `CreateOrderServiceTest`, `ClosePositionServiceTest`,
  `./gradlew architectureLint`, `GRADLE_USER_HOME=/tmp/gradle-home ./gradlew check` 통과
- [x] (2026-04-19 18:12+09:00) 2차 `red` 단계 완료: `MemberCredentialTest`, `MarketHistoryCandleTest`로 회원 정규화/비밀번호 규칙과 분봉
  병합/시간봉 집계의 domain 메서드 부재를 컴파일 실패로 고정
- [x] (2026-04-19 18:18+09:00) 2차 `green` 단계 완료: `MemberCredential`, `TradingAccount`, `RewardPointWallet`,
  `MarketHistoryCandle`, `HourlyMarketCandle`에 정규화/초기 생성/적립/캔들 병합 규칙 추가
- [x] (2026-04-19 18:20+09:00) 2차 `refactor` 단계 완료: 회원 서비스, 리워드 서비스, `MarketHistoryRecorder`를 domain 호출 중심으로 단순화
- [x] (2026-04-19 18:26+09:00) 3차 `red` 단계 완료: `FuturesOrderTest`, `RewardShopCatalogTest`, `MemberCredentialTest` 확장으로
  주문 상태 결정, 상점 카탈로그, 인증 입력 규칙의 domain 이동 필요성을 컴파일 실패로 고정
- [x] (2026-04-19 18:30+09:00) 3차 `green/refactor` 단계 완료: `FuturesOrder.place`, `RewardShopCatalog`, `RewardShopItem`,
  `MemberCredential.requirePasswordInput/requireSameMember` 도입 및 `AuthController`, `CreateOrderService`,
  `WithdrawMemberService`, `GetShopItemsService`, `GetRewardPointService` 정리
- [x] (2026-04-19 18:31+09:00) 최종 검증 완료: 주요 회귀 테스트,
  `GRADLE_USER_HOME=/tmp/gradle-home ./gradlew architectureLint --console=plain`,
  `GRADLE_USER_HOME=/tmp/gradle-home ./gradlew check --console=plain` 통과

## 놀라움과 발견

- 관찰:
  현재 `CreateOrderService`는 주문 저장뿐 아니라 계좌 사용 가능 증거금 차감, 체결 상태 결정, 포지션 평균 진입가 계산까지 직접 수행한다.
  증거:
  [backend/src/main/java/coin/coinzzickmock/feature/order/application/service/CreateOrderService.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/order/application/service/CreateOrderService.java)

- 관찰:
  현재 `ClosePositionService`는 실현 손익, 청산 수수료, 해제 증거금, 잔여 포지션 수량 계산을 모두 직접 수행한다.
  증거:
  [backend/src/main/java/coin/coinzzickmock/feature/position/application/service/ClosePositionService.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/position/application/service/ClosePositionService.java)

- 관찰:
  도메인 타입 대부분은 단순 `record`라서 상태 전이 메서드를 어디에 둘지 먼저 결정해야 한다.
  증거:
  `backend/src/main/java/coin/coinzzickmock/feature/*/domain/*.java`를 확인하면 행동을 가진 타입은 `OrderPreviewPolicy`,
  `RewardPointPolicy` 위주다.

- 관찰:
  현재 추가 진입 로직은 평균 진입가는 다시 계산하지만, 청산가는 새 주문의 preview 값만 그대로 써서 기존 포지션과 합쳐진 평균 진입가를 반영하지 못한다.
  증거:
  `./gradlew test --tests coin.coinzzickmock.feature.order.application.service.CreateOrderServiceTest --console=plain`
  실행 시 `recalculatesLiquidationPriceFromWeightedEntryPriceWhenIncreasingExistingPosition()`가 실패했다.

- 관찰:
  전체 `./gradlew check`는 샌드박스 기본 Gradle 홈 경로의 lock 파일 권한 문제로 바로 실행되지 않았고, 임시 `GRADLE_USER_HOME`을 써야 했다.
  증거:
  기본 실행은 `/Users/hj.park/.gradle/...gradle-8.14.3-bin.zip.lck (Operation not permitted)`로 실패했고,
  `GRADLE_USER_HOME=/tmp/gradle-home ./gradlew check --console=plain`은 성공했다.

- 관찰:
  `RegisterMemberService`, `AuthenticateMemberService`, `CheckMemberAvailabilityService`는 같은 문자열 정규화와 필수값 검증을 반복하고 있다.
  증거:
  각 서비스가 `trim()`, `isBlank()`, 길이 검증을 직접 수행한다.

- 관찰:
  `MarketHistoryRecorder`는 분봉 최초 생성, 분봉 병합, 시간봉 rollup 계산을 모두 직접 수행해 domain 캔들 타입이 데이터 컨테이너에 머물러 있다.
  증거:
  [backend/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketHistoryRecorder.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketHistoryRecorder.java)

- 관찰:
  `CreateOrderService`에는 마지막으로 주문 상태 `"FILLED"`/`"PENDING"` 결정 규칙이 남아 있었고, `GetShopItemsService`에는 리워드 상점 품목 정의가 하드코딩되어
  있었다.
  증거:
  각각 `FuturesOrder.place(...)`, `RewardShopCatalog.defaultItems()` 도입 전 서비스 코드에서 직접 문자열/목록을 만들고 있었다.

- 관찰:
  2차, 3차 리팩토링 후 application/service와 realtime 레이어를 다시 훑어보면 남은 분기 대부분은 입력 검증, cache guard, 외부 조회 실패 처리처럼 오케스트레이션 성격이다.
  증거:
  `backend/src/main/java/coin/coinzzickmock/feature/*/application/service/*.java`,
  `backend/src/main/java/coin/coinzzickmock/feature/market/application/realtime/*.java` 재검토 결과, 손익/증거금/포인트/정규화/캔들 계산은
  domain으로 이동했다.

## 의사결정 기록

- 결정:
  이번 1차 리팩토링은 주문/청산 흐름의 모든 개념을 한 번에 새 aggregate로 재설계하지 않고, 현재 API와 repository 계약을 유지한 채 도메인 상태 전이 메서드와 도메인 전용 계산 객체를
  추가하는 범위로 제한한다.
  근거:
  한 번에 aggregate 재설계까지 가면 저장소 계약, 엔티티 매핑, API 결과가 모두 흔들려 회귀 위험이 커진다. 우선 서비스가 직접 들고 있는 핵심 계산부터 domain으로 올리는 편이 안전하다.
  날짜/작성자:
  2026-04-19 / Codex

- 결정:
  `TradingAccount`에는 사용 가능 증거금 차감과 청산 후 잔고 반영 메서드를 두고, `PositionSnapshot`에는 신규 포지션 생성, 기존 포지션 가중 평균 진입가 반영, 부분 청산 후 잔여
  상태 계산 메서드를 우선 둔다.
  근거:
  현재 서비스 코드에서 가장 자주 반복되고, 제품 의미가 가장 분명한 상태 전이가 이 두 타입에 모여 있다.
  날짜/작성자:
  2026-04-19 / Codex

- 결정:
  부분 청산 결과는 서비스 내부 임시 변수 묶음 대신 `PositionCloseOutcome` 도메인 값 객체로 묶는다.
  근거:
  실현 손익, 수수료, 해제 증거금, 잔여 포지션은 같은 도메인 계산의 결과이므로 한 타입으로 반환하는 편이 서비스 오케스트레이션을 단순하게 만든다.
  날짜/작성자:
  2026-04-19 / Codex

- 결정:
  남은 절차지향 로직은 범위가 큰 aggregate 재설계 대신, `member`, `reward`, `market-history` 순서로 묶어 단계별로 domain 메서드/정적 팩터리/값 객체에 옮긴다.
  근거:
  한 번에 전체 재설계를 하면 저장소와 API까지 흔들 수 있으므로, 현재 계약을 유지한 채 반복 계산과 정규화부터 걷어내는 편이 안전하다.
  날짜/작성자:
  2026-04-19 / Codex

- 결정:
  남아 있던 소규모 규칙도 service 상수로 남기지 않고, 주문 상태 결정은 `FuturesOrder`, 리워드 상점 품목은 `RewardShopCatalog`, 탈퇴 본인확인은
  `MemberCredential`로 흡수한다.
  근거:
  규칙 크기는 작아도 서비스가 문자열/목록/권한 비교를 직접 가지면 다시 절차형 코드가 퍼지기 쉽다. 현재 구조에서는 정적 팩터리와 값 객체로 옮기는 편이 가장 작은 변경으로 일관성을 지킬 수 있다.
  날짜/작성자:
  2026-04-19 / Codex

## 결과 및 회고

현재까지 주문 체결 후 계좌 차감, 포지션 평균 진입가/청산가 재계산, 포지션 청산 후 손익/수수료/잔여 포지션 계산, 회원 정규화/비밀번호 입력/탈퇴 본인확인, 리워드 포인트 월렛 상태 전이와 상점 카탈로그, 분봉
병합/시간봉 집계까지 domain으로 옮겼다.
application/service와 realtime 레이어 재점검 결과 남아 있는 분기는 외부 조회, 요청 검증, cache guard 같은 오케스트레이션 성격이며, 핵심 도메인 계산은 이번 범위에서 정리됐다.
남은 종료 조건은 `AGENTS.md` 기준 review 스킬 검토와 branch/commit/push/PR 단계다.

## 맥락과 길잡이

먼저 읽을 문서:

- [BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)
- [docs/design-docs/backend-design/04-domain-modeling-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/04-domain-modeling-rules.md)
- [docs/design-docs/backend-design/03-application-and-providers.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/03-application-and-providers.md)
- [docs/product-specs/coin-futures-platform-mvp.md](/Users/hj.park/projects/coin-zzickmock/docs/product-specs/coin-futures-platform-mvp.md)

핵심 코드:

- [backend/src/main/java/coin/coinzzickmock/feature/order/application/service/CreateOrderService.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/order/application/service/CreateOrderService.java)
- [backend/src/main/java/coin/coinzzickmock/feature/position/application/service/ClosePositionService.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/position/application/service/ClosePositionService.java)
- [backend/src/main/java/coin/coinzzickmock/feature/account/domain/TradingAccount.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/account/domain/TradingAccount.java)
- [backend/src/main/java/coin/coinzzickmock/feature/position/domain/PositionSnapshot.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/position/domain/PositionSnapshot.java)
- [backend/src/main/java/coin/coinzzickmock/feature/order/domain/OrderPreviewPolicy.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/order/domain/OrderPreviewPolicy.java)
- [backend/src/test/java/coin/coinzzickmock/feature/order/application/service/CreateOrderServiceTest.java](/Users/hj.park/projects/coin-zzickmock/backend/src/test/java/coin/coinzzickmock/feature/order/application/service/CreateOrderServiceTest.java)

이번 작업에서 말하는 "도메인 리팩토링"은 service 코드를 domain으로 무조건 옮기는 것이 아니다.
저장소 호출 순서, 외부 시세 로딩, 결과 DTO 조립 같은 유스케이스 오케스트레이션은 계속 `application/service`에 두고, 비즈니스 개념의 상태 전이와 계산만 domain으로 이동한다.

## 작업 계획

첫 번째 마일스톤은 `red` 단계다.
`CreateOrderServiceTest`와 필요한 새 테스트를 이용해, 시장가 주문 체결 시 계좌 사용 가능 증거금이 줄고 포지션 평균 진입가가 올바르게 계산되는지, 포지션 청산 시 잔고/사용 가능 증거금/잔여
포지션이 기대대로 바뀌는지를 테스트로 먼저 고정한다.
이 단계에서는 아직 domain 메서드가 없으므로, 새 테스트 또는 테스트 확장이 현재 컴파일 실패나 assertion 실패를 만들어야 한다.

두 번째 마일스톤은 `green` 단계다.
`TradingAccount`에 증거금 사용과 청산 정산을 표현하는 메서드를 추가하고, `PositionSnapshot`에 신규 오픈, 수량 증가, 부분 청산 후 잔여 상태 계산을 표현하는 메서드를 추가한다.
필요하면 주문 체결 후 계좌와 포지션에 동시에 적용할 계산을 담는 새 domain policy 또는 calculation result 타입을 `feature/order/domain` 또는
`feature/position/domain`에 추가한다.
이 단계의 목표는 서비스가 직접 수치 계산을 품지 않고도 기존 API 결과를 유지하게 만드는 것이다.

세 번째 마일스톤은 `refactor` 단계다.
`CreateOrderService`와 `ClosePositionService`를 읽었을 때 "시세 로드 -> domain 호출 -> 저장 -> 결과 반환" 흐름이 드러나도록 메서드 구조와 이름을 정리한다.
도메인 기준 문서와 정식 ExecPlan에도 실제 반영 결과를 기록한다.

## 구체적인 단계

1. `backend/src/test/java/coin/coinzzickmock/feature/order/application/service/CreateOrderServiceTest.java`를 읽고 `red`
   범위를 확장한다.
2. 필요하면 `ClosePositionService` 대상 테스트를 새로 추가하거나 기존 테스트를 만든다.
3. 실패 테스트를 먼저 만들고 `cd backend && ./gradlew test --tests ...`로 의도한 실패를 확인한다.
4. `TradingAccount`, `PositionSnapshot`, 필요 시 새 domain policy/result 타입을 추가해 계산을 이동한다.
5. `CreateOrderService`, `ClosePositionService`를 domain 호출 중심으로 다시 작성한다.
6. 관련 테스트를 다시 실행해 통과시킨다.
7. `cd backend && ./gradlew architectureLint`와 `cd backend && ./gradlew check`를 실행한다.
8. 변경 범위를 대상으로 `AGENTS.md` 기준 review 스킬 검토를 수행하고 결과를 기록한다.
9. branch/commit/push/PR까지 진행한다.

## 검증과 수용 기준

실행 명령:

-
`cd /Users/hj.park/projects/coin-zzickmock/backend && ./gradlew test --tests coin.coinzzickmock.feature.order.application.service.CreateOrderServiceTest --console=plain`
-
`cd /Users/hj.park/projects/coin-zzickmock/backend && ./gradlew test --tests coin.coinzzickmock.feature.position.application.service.ClosePositionServiceTest --console=plain`
- `cd /Users/hj.park/projects/coin-zzickmock/backend && ./gradlew architectureLint --console=plain`
- `cd /Users/hj.park/projects/coin-zzickmock/backend && ./gradlew check --console=plain`

수용 기준:

- `CreateOrderService`는 계좌 차감과 포지션 평균 진입가 계산을 직접 수행하지 않고 domain 메서드 또는 domain policy를 호출한다.
- `ClosePositionService`는 실현 손익, 수수료, 잔여 포지션 계산을 직접 수행하지 않고 domain 메서드 또는 domain policy를 호출한다.
- `TradingAccount`와 `PositionSnapshot`은 생성용 데이터 컨테이너를 넘어 의미 있는 상태 전이 메서드를 가진다.
- 주문 미리보기 API 결과와 주문 실행/포지션 종료의 기존 외부 동작은 회귀하지 않는다.
- `architectureLint`와 `check`가 모두 통과한다.

## 반복 실행 가능성 및 복구

- 이 작업은 DB 스키마를 바꾸지 않는 범위로 시작하므로 migration 복구 절차는 필요 없다.
- 도메인 메서드 추가 중 구조가 흔들리면 service 코드에서 계산을 유지한 채 테스트만 먼저 복원할 수 있다.
- 현재 워킹트리에 다른 문서 변경이 섞여 있을 수 있으므로, commit 단계에서는 이번 작업 파일만 선택적으로 stage 해야 한다.

## 산출물과 메모

- 예상 PR 제목:
  주문/포지션 도메인 상태 전이 리팩토링

- 변경 메모:
  이번 1차 범위는 주문 preview 자체보다 체결 이후 상태 변경과 포지션 청산 계산을 domain으로 올리는 데 집중한다.

- 예상 주요 수정 파일:
  `backend/src/main/java/coin/coinzzickmock/feature/account/domain/TradingAccount.java`
  `backend/src/main/java/coin/coinzzickmock/feature/position/domain/PositionSnapshot.java`
  `backend/src/main/java/coin/coinzzickmock/feature/order/application/service/CreateOrderService.java`
  `backend/src/main/java/coin/coinzzickmock/feature/position/application/service/ClosePositionService.java`
  `backend/src/test/java/coin/coinzzickmock/feature/order/application/service/CreateOrderServiceTest.java`

- 변경 메모:
  `red` 단계가 끝나면 실패 테스트 이름과 실패 원인을 `진행 현황`과 `놀라움과 발견`에 바로 기록한다.
