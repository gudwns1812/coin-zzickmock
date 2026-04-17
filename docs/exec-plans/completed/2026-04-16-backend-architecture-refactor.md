# 백엔드 아키텍처 문서 정합화 리팩토링 계획

이 계획서는 [PLANS.md](/Users/hj.park/projects/coin-zzickmock/PLANS.md)와 [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)를 따른다. 이 문서는 살아 있는 문서이며, `진행 현황`, `놀라움과 발견`, `의사결정 기록`, `결과 및 회고` 섹션을 작업 내내 최신 상태로 유지한다.

## 목적 / 큰 그림

이 작업의 목적은 현재 `backend/`가 [BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)와 [docs/design-docs/backend-design/01-architecture-foundations.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/01-architecture-foundations.md)가 요구하는 구조와 어긋나는 부분을 정리하는 것이다.

완료 후에는 사용자가 기존 선물 API를 같은 엔드포인트로 계속 사용할 수 있으면서도, 백엔드 내부 구조가 아래 기준을 만족해야 한다.

- `feature-first` 구조를 유지한다.
- `application/usecase`, `application/port` 같은 기본값 추상화를 제거한다.
- 저장소와 외부 경계 계약은 `application`이 소유하고, 구현은 `infrastructure`가 맡는다.
- API는 저장소를 직접 호출하지 않고 application service만 사용한다.
- 교차 관심사 접근은 `Providers`를 통해 유지한다.
- 예외는 `common/error`의 구조화된 타입과 글로벌 핸들러로 응답된다.

이 변경이 끝나면 `./gradlew architectureLint`와 `./gradlew check`가 통과해야 하고, 기존 선물 API의 핵심 유스케이스를 고정하는 테스트가 함께 통과해야 한다.

## 진행 현황

- [x] (2026-04-16 13:19+09:00) 문서 기준과 현재 backend 구조 차이 조사 완료
- [x] (2026-04-16 13:23+09:00) 사용자 승인 수신: "그냥 바로 진행 해"를 이 계획 승인으로 간주
- [x] (2026-04-16 13:27+09:00) active 실행 계획 문서 생성 완료
- [x] (2026-04-16 15:18+09:00) 애플리케이션 계약과 서비스 패키지 구조를 문서 기준으로 정리
- [x] (2026-04-16 15:19+09:00) API가 직접 저장소를 호출하는 흐름 제거
- [x] (2026-04-16 15:20+09:00) 공통 예외와 글로벌 핸들러 도입
- [x] (2026-04-16 15:30+09:00) 관련 테스트를 보강하고 새 구조에 맞게 갱신
- [x] (2026-04-16 15:31+09:00) 빈 `application/usecase`, `application/port` 디렉터리 제거
- [x] (2026-04-16 15:32+09:00) provider 구현체를 `providers/infrastructure`로 이동해 bootstrap 책임 축소
- [x] (2026-04-16 15:32+09:00) `./gradlew architectureLint` 실행
- [x] (2026-04-16 15:32+09:00) `./gradlew check` 실행
- [x] (2026-04-16 15:33+09:00) 품질 게이트용 자체 리뷰 수행 및 결과 기록
- [x] (2026-04-16 16:18+09:00) 사용자 요청에 따라 `bootstrap` 제거 방향으로 active 계획 갱신
- [x] (2026-04-16 16:21+09:00) `bootstrap` 제거를 반영한 문서, 린트, 패키지 구조 수정 및 재검증
- [x] (2026-04-16 16:56+09:00) PR #1 pending 리뷰 스레드 조사 및 수정 범위 확정
- [x] (2026-04-16 19:54+09:00) PR 리뷰 코멘트를 반영한 테스트 추가 및 핵심 동작 확인
- [x] (2026-04-16 19:55+09:00) PR 리뷰 코멘트를 반영한 문서/코드/린트 규칙 정리
- [x] (2026-04-16 19:56+09:00) 변경 범위 재검증 및 품질 게이트 재실행

## 놀라움과 발견

- 관찰:
  현재 아키텍처 린트는 패키지 상위 구조와 일부 금지 import는 막지만, `application/usecase`, `application/port`, `infrastructure` 소유 계약 같은 문서 기준의 과잉 추상화는 아직 직접 막지 않는다.
  증거:
  `backend/build.gradle`의 `architectureLint` 구현을 읽었고, 실제로 `./gradlew architectureLint`가 현재 코드에서 통과했다.

- 관찰:
  `RewardController`는 다른 컨트롤러와 달리 application service를 거치지 않고 저장소를 직접 읽고 있다.
  증거:
  [backend/src/main/java/coin/coinzzickmock/feature/reward/api/RewardController.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/reward/api/RewardController.java)가 `RewardPointRepository`를 직접 주입받고 있다.

- 관찰:
  `FuturesBootstrapConfiguration`은 provider 조립, 외부 connector 조립, feature bean 조립, demo seed까지 한 파일에서 모두 맡고 있다.
  증거:
  작업 시작 시점의 `coin.coinzzickmock.bootstrap.config.FuturesBootstrapConfiguration`에는 `RestClient` bean과 demo account initializer가 함께 선언되어 있었다.

- 관찰:
  `bootstrap`은 계층이라기보다 전역 조립 bucket처럼 동작해서 owner가 다른 설정이 한곳에 섞이기 쉽다.
  증거:
  기존 `FuturesBootstrapConfiguration`은 Bitget connector bean과 demo account seed를 같은 파일에 선언하고 있었고, 두 설정의 소유자는 각각 `providers`와 `feature/account`다.

- 관찰:
  `application/usecase`, `application/port` 파일을 지운 뒤에도 빈 디렉터리가 남아 있어 문서 정합성 관점에서는 한 번 더 정리해야 했다.
  증거:
  `find backend/src/main/java/coin/coinzzickmock/feature -path '*/application/usecase' -o -path '*/application/port'`를 실행해 빈 디렉터리를 확인한 뒤 `rmdir`로 제거했다.

- 관찰:
  주문 서비스 테스트는 기존의 가짜 `OrderPreviewPolicy`에 기대고 있었기 때문에, 정책을 서비스 내부로 옮기자 기대 증거금 값도 함께 바뀌었다.
  증거:
  [backend/src/test/java/coin/coinzzickmock/feature/order/application/service/CreateOrderServiceTest.java](/Users/hj.park/projects/coin-zzickmock/backend/src/test/java/coin/coinzzickmock/feature/order/application/service/CreateOrderServiceTest.java)의 기대 `availableMargin` 값을 `98990`에서 `98995`로 조정했다.

- 관찰:
  PR #1에는 아직 제출되지 않은 pending 리뷰 스레드가 9개 남아 있으며, 대부분이 예외 단순화, 엔티티 네이밍, 계층 책임, 매직 넘버 제거처럼 현재 리팩토링 방향과 직접 연결된다.
  증거:
  GitHub PR `Build coin futures MVP foundation`의 conversation 화면에서 `BadRequestException`, `NotFoundException`, `TradingAccountJpaEntity`, `CreateOrderService.preview`, `GrantProfitPointService`, `BitgetMarketDataGateway` 등에 대한 pending 코멘트를 확인했다.

## 의사결정 기록

- 결정:
  이번 리팩토링은 공개 API 경로와 응답 형식은 유지하고, 내부 구조만 정리한다.
  근거:
  사용자 요청은 "백엔드 아키텍처의 문서대로 현재 backend를 리팩토링"하는 것이므로, 기능 회귀 없이 구조 정합성을 우선 맞추는 것이 안전하다.
  날짜/작성자:
  2026-04-16 / Codex

- 결정:
  `*UseCase`, `*Port` 인터페이스는 모두 제거 대상으로 보지 않고, 문서 기준상 "실제 public contract가 필요한지"를 파일별로 다시 판단한다.
  근거:
  문서는 인터페이스 전면 금지가 아니라 "기계적 기본값"을 금지한다. 따라서 다중 구현이나 경계 표현이 실제로 필요한 경우는 유지할 수 있다.
  날짜/작성자:
  2026-04-16 / Codex

- 결정:
  구현 단계의 `red -> green -> refactor`는 현재 세션 제약상 sub-agent를 쓰지 않고 메인 에이전트가 순차 수행한다.
  근거:
  저장소 문서는 단계별 sub-agent를 권장하지만, 현재 상위 도구 정책상 사용자 명시 요청 없이 sub-agent를 생성할 수 없다. 대신 같은 순서를 로컬에서 강하게 지킨다.
  날짜/작성자:
  2026-04-16 / Codex

- 결정:
  provider 구현체는 익명 `@Bean`보다 `providers/infrastructure`의 concrete class로 내린다.
  근거:
  아키텍처 문서는 provider 구현체를 `Providers` 뒤로 숨긴 명시적 구현으로 다루는 방향을 제시한다. 구체 클래스로 내리면 bootstrap은 조립과 seed만 맡게 되고 역할이 더 선명해진다.
  날짜/작성자:
  2026-04-16 / Codex

- 결정:
  `bootstrap` 패키지는 유지하지 않고, `CoinZzickmockApplication`은 루트 패키지로 올리며 설정은 owner의 `infrastructure/config`로 재배치한다.
  근거:
  강한 계층을 적용할 때 전역 bucket보다 소유 패키지가 드러나는 배치가 경계를 더 명확하게 만든다. Bitget connector는 `providers`, demo seed는 `feature/account`, Querydsl 설정은 현재 사용처인 `feature/position`에 귀속하는 편이 구조 의도를 잘 드러낸다.
  날짜/작성자:
  2026-04-16 / Codex

- 결정:
  이번 후속 루프에서는 PR #1의 pending 리뷰 스레드 전체를 같은 active 계획의 연장선으로 반영한다.
  근거:
  사용자 요청은 "내가 PR에 리뷰 달았는데 확인해서 문서/린트/코드를 수정"하는 것이고, 현재 열려 있는 active 계획과 같은 브랜치/PR 범위 안에서 닫히는 작업이기 때문이다.
  날짜/작성자:
  2026-04-16 / Codex

## 결과 및 회고

이번 리팩토링으로 공개 API 경로는 유지한 채 내부 구조를 문서 기준에 더 가깝게 맞췄다.

- `application/usecase`, `application/port` 기본 추상화를 제거했다.
- 저장소 계약을 각 feature의 `application/repository`로 옮겼다.
- 애플리케이션 서비스는 `application/service`로 모으고 concrete class를 직접 주입받게 바꿨다.
- `RewardController`가 저장소를 직접 읽지 않도록 `GetRewardPointService`, `GetShopItemsService`를 추가했다.
- `common/error` 아래에 구조화된 예외와 글로벌 핸들러를 추가했다.
- provider 구현체를 `providers/infrastructure`로 이동해 `bootstrap/config`의 책임을 줄였다.
- `bootstrap` 패키지를 제거하고 `CoinZzickmockApplication`을 루트 패키지로 이동했다.
- Bitget, Querydsl, demo account seed 설정을 각각 owner의 `infrastructure/config`로 나눴다.
- 문서와 아키텍처 린트도 같은 기준으로 갱신해 루트 `*Application`과 owner별 config 배치를 자동 검증하게 만들었다.
- PR 리뷰 반영으로 `CoreException` 단일 예외 모델, `*Entity` 네이밍, 공통 감사 필드, `OrderPreviewPolicy`, `RewardPointPolicy`, `BitgetTickerSnapshotMapper`를 도입했다.
- 계정 도메인이 `memberEmail`을 명시적으로 소유하게 바꿔 저장소가 임의 기본값을 주입하지 않도록 정리했다.

남은 리스크는 크지 않지만 두 가지가 있다.

1. `BitgetMarketDataGateway`는 외부 호출 실패 시 fallback을 위해 `catch (Exception)`을 유지한다. 경계에서의 예외 번역으로 볼 수는 있지만, 추후에는 `ExternalServiceException` 같은 명시적 타입으로 더 좁히는 편이 좋다.
2. 이번 작업은 application/service와 repository 중심 검증에는 충분하지만, controller slice 테스트는 아직 없다. 이후 API 회귀 리스크를 더 낮추려면 controller 테스트를 추가하는 것이 좋다.

## 맥락과 길잡이

이번 작업에서 가장 먼저 읽어야 하는 코드는 아래와 같다.

- [backend/src/main/java/coin/coinzzickmock/CoinZzickmockApplication.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/CoinZzickmockApplication.java)
- [backend/src/main/java/coin/coinzzickmock/providers/infrastructure/config/BitgetConnectorConfiguration.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/providers/infrastructure/config/BitgetConnectorConfiguration.java)
- [backend/src/main/java/coin/coinzzickmock/feature/account/infrastructure/config/AccountDemoSeedConfiguration.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/account/infrastructure/config/AccountDemoSeedConfiguration.java)
- [backend/src/main/java/coin/coinzzickmock/feature/position/infrastructure/config/PositionQuerydslConfiguration.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/position/infrastructure/config/PositionQuerydslConfiguration.java)
- [backend/src/main/java/coin/coinzzickmock/feature/account/application/service/GetAccountSummaryService.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/account/application/service/GetAccountSummaryService.java)
- [backend/src/main/java/coin/coinzzickmock/feature/market/application/service/GetMarketSummaryService.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/market/application/service/GetMarketSummaryService.java)
- [backend/src/main/java/coin/coinzzickmock/feature/order/application/service/CreateOrderService.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/order/application/service/CreateOrderService.java)
- [backend/src/main/java/coin/coinzzickmock/feature/position/application/service/ClosePositionService.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/position/application/service/ClosePositionService.java)
- [backend/src/main/java/coin/coinzzickmock/feature/reward/api/RewardController.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/reward/api/RewardController.java)

현재 구조에서 정리해야 하는 핵심 문제는 아래와 같다.

1. `application/usecase`, `application/port` 패키지가 문서의 기본 골격과 어긋난다.
2. `AccountRepository`, `OrderRepository`, `PositionRepository`, `RewardPointRepository` 계약이 `infrastructure` 패키지에 놓여 있어 계약 소유 위치가 어긋난다.
3. `RewardController`가 application service가 아니라 저장소를 직접 사용한다.
4. `BitgetMarketSnapshotReader`는 `LoadMarketSnapshotPort`라는 pass-through 계약만 구현하는 중간 계층이라 단순화 후보이다.
5. `IllegalArgumentException`이 application 흐름 곳곳에서 발생해 공통 오류 모델이 없다.
6. `bootstrap` 패키지가 전역 bucket처럼 동작해 설정 owner 경계를 흐린다.

## 작업 계획

먼저 공통 예외 모델을 `CoreException` 단일 타입 중심으로 다시 단순화한다. `backend/src/main/java/coin/coinzzickmock/common/error/` 아래에서 `NotFoundException`, `BadRequestException` 같은 세분화 클래스를 제거하고, `ErrorCode`의 HTTP 상태와 메시지로 분류를 유지한다. 관련 설계 문서의 예외 규칙도 같은 방향으로 갱신한다.

그 다음 feature별 application 구조를 리뷰 의도에 맞게 다듬는다. 주문 미리보기는 `CreateOrderService.preview`가 진입가격, 실행 가능 여부, 수수료 타입, 증거금 계산을 모두 책임지도록 정리하고, 컨트롤러는 계산 결과를 그대로 응답으로 매핑만 하게 만든다. 문자열 상수와 보상 포인트 임계값은 명시적 상수로 끌어올린다.

이후 controller와 persistence를 정리한다. 컨트롤러 생성자 보일러플레이트는 Lombok으로 줄이고, 계정 도메인이 `memberEmail`을 명시적으로 가지도록 바꿔 저장소가 임의 기본 이메일을 주입하지 않게 만든다. JPA 엔티티는 기술명을 뺀 이름으로 통일하고, 반복되는 `created_at`/`updated_at` 필드는 공통 기반 클래스로 추출한다.

마지막으로 provider 조립을 정리한다. `BitgetMarketDataGateway`가 외부 응답을 직접 `MarketSnapshot`으로 조립하던 책임은 `providers/infrastructure/mapper`의 전용 mapper로 분리하고, gateway는 호출과 fallback 경계만 담당하게 만든다.

## 구체적인 단계

1. `backend/src/test/java`에 PR 리뷰 반영으로 바뀌면 안 되는 핵심 동작 테스트를 먼저 추가하거나 보강한다.
   목표는 다음 세 가지다.
   - 주문 preview가 진입가격, 실행 가능 여부, 수수료 타입을 서비스에서 일관되게 계산하는지
   - 계정/포지션 서비스가 `memberEmail`을 잃지 않고 계정 잔액을 갱신하는지
   - 보상 포인트 임계값이 상수화 뒤에도 같은 결과를 유지하는지

2. `backend/src/main/java/coin/coinzzickmock/common/error/`와 설계 문서를 정리해 `CoreException` 단일 타입 정책으로 맞춘다.

3. 리뷰가 걸린 feature 코드를 정리한다.
   - account: `memberEmail`을 도메인에 올리고 기본 이메일 보정 제거
   - order: preview 계산 책임 정리, 상수화, 응답 값 계산 위치 정리
   - reward: 포인트 tier 매직 넘버 제거
   - provider: Bitget 응답 mapper 분리

4. controller와 persistence 보일러플레이트를 줄이고 엔티티 네이밍/공통 감사 필드를 정리한다.

5. 아래 명령을 `backend/`에서 실행한다.
   `GRADLE_USER_HOME=/tmp/gradle-home ./gradlew test --console=plain`
   `GRADLE_USER_HOME=/tmp/gradle-home ./gradlew architectureLint --console=plain`
   `GRADLE_USER_HOME=/tmp/gradle-home ./gradlew check --console=plain`

## 검증과 수용 기준

아래 조건을 모두 만족하면 이번 리팩토링을 완료로 본다.

- `backend/src/main/java/coin/coinzzickmock/feature/*` 아래에 더 이상 `application/usecase`와 `application/port` 패키지가 남아 있지 않다.
- API 레이어는 저장소 구현 또는 저장소 계약을 직접 주입받지 않는다.
- 주문 생성, 포지션 청산, 계정 요약, reward 조회 관련 테스트가 통과한다.
- `./gradlew architectureLint`와 `./gradlew check`가 통과한다.
- 기존 API 엔드포인트 경로는 그대로 유지된다.

## 반복 실행 가능성 및 복구

이 작업은 패키지 이동과 import 수정이 많기 때문에 중간 실패가 나도 `git diff`로 변경 범위를 확인하면서 같은 순서로 재시도할 수 있다. DB migration은 이번 작업 범위에 포함하지 않으므로 스키마 파괴 위험은 없다.

테스트 명령은 여러 번 반복 실행해도 안전하다. Gradle 캐시가 꼬이면 `GRADLE_USER_HOME=/tmp/gradle-home`를 유지한 채 다시 실행한다.

## 산출물과 메모

완료 후 이 섹션에 아래 증거를 짧게 남긴다.

- 핵심 패키지 이동 결과
- 테스트 통과 핵심 줄
- architecture lint 요약 줄
- check 통과 요약 줄

증거:

- `backend/src/main/java/coin/coinzzickmock/feature/*` 아래에서 `application/usecase`, `application/port` 디렉터리가 제거되었다.
- `backend/src/main/java/coin/coinzzickmock/bootstrap` 디렉터리가 제거되고, 루트 `CoinZzickmockApplication`과 owner별 `infrastructure/config`만 남았다.
- `backend/src/main/java/coin/coinzzickmock/common/error` 아래에서 `BadRequestException`, `NotFoundException`이 제거되고 `CoreException`만 남았다.
- `backend/src/main/java/coin/coinzzickmock/feature/*/infrastructure/persistence`의 JPA 엔티티가 `*JpaEntity`에서 `*Entity`로 정리되었다.
- `GRADLE_USER_HOME=/tmp/gradle-home ./gradlew test --console=plain`
  `BUILD SUCCESSFUL in 6s`
- `GRADLE_USER_HOME=/tmp/gradle-home ./gradlew architectureLint --console=plain`
  `"status":"passed","violations":0`
- `GRADLE_USER_HOME=/tmp/gradle-home ./gradlew check --console=plain`
  `BUILD SUCCESSFUL`

## 인터페이스와 의존성

이번 작업에서 의도하는 핵심 의존성 방향은 아래와 같다.

- `api` -> concrete `application/service`
- `application/service` -> `application`이 소유한 repository/gateway 계약 + `Providers`
- `infrastructure/persistence` -> 해당 feature의 `application` 계약 구현
- `domain` -> 순수 도메인 타입만 유지

작업이 끝나면 아래와 같은 안정적인 타입 이름이 존재해야 한다.

- `coin.coinzzickmock.common.error.CoreException`
- `coin.coinzzickmock.feature.account.application.service.GetAccountSummaryService`
- `coin.coinzzickmock.feature.reward.application.service.GetRewardPointService`
- `coin.coinzzickmock.feature.order.application.service.CreateOrderService`
- `coin.coinzzickmock.feature.position.application.service.ClosePositionService`

변경 메모:
2026-04-16 13:27+09:00 / 사용자 승인 후 백엔드 아키텍처 문서 정합화 리팩토링 계획을 새 active 계획으로 작성했다.
2026-04-16 15:33+09:00 / 서비스, 저장소 계약, 예외, provider 구현체, 테스트, 검증 결과를 반영해 계획 문서를 완료 상태로 갱신했다.
2026-04-16 16:18+09:00 / 사용자 요청에 따라 `bootstrap` 제거 방향을 반영하도록 계획을 다시 열고, owner별 config 재배치 작업을 추가했다.
2026-04-16 16:21+09:00 / 루트 `CoinZzickmockApplication`, owner별 `infrastructure/config`, 아키텍처 린트 규칙, 설계 문서 갱신과 `architectureLint`/`check` 재검증 결과를 반영했다.
2026-04-16 19:56+09:00 / PR #1 pending 리뷰 스레드를 반영해 예외 단일화, 엔티티 네이밍/공통 감사 필드, 주문/보상 정책, Bitget mapper 분리, Lombok 보일러플레이트 축소와 재검증 결과를 반영했다.
2026-04-16 20:58+09:00 / `진행 현황` 기준으로 남은 구현 항목이 없음을 다시 확인했고, `active`에서 `completed`로 정리할 수 있도록 완료 상태 메모를 남겼다.
