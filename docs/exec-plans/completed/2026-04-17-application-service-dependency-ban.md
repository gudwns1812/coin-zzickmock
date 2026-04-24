# application service 간 직접 의존 금지 정리

이 계획서는 [PLANS.md](/Users/hj.park/projects/coin-zzickmock/PLANS.md)와 [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)를 따른다.
이 문서는 백엔드 `application/service` 패키지 안의 유스케이스 서비스가 서로를 직접 참조하지 않도록 구조, 린트, 문서를 함께 맞추는 작업의 단일 기준서다.
사용자 요청이 곧 작업 승인으로 해석되는 범위라서, 본 문서는 승인 직후 상태로 `active`에 둔다.

## 목적 / 큰 그림

현재 백엔드에는 `application/service` 아래의 서비스가 다른 서비스의 구현을 직접 주입받는 코드가 있다.
이 구조는 유스케이스 경계를 흐리고, 서비스가 서비스 위에 덧쌓이면서 책임이 어디에 있는지 읽기 어려워지며, 현재 `architectureLint`도 이 문제를 막지 못한다.

이 작업이 끝나면 `application/service`는 "API가 직접 호출하는 유스케이스 진입점" 역할로만 남고, 여러 유스케이스가 함께 쓰는 런타임/처리 로직은 `application`의 목적별 하위 패키지에 있는 비-Service 협력 객체로 이동한다.
또한 회귀 테스트와 `architectureLint`가 둘 다 같은 규칙을 감시하므로, 이후 누가 다시 서비스끼리 직접 물리더라도 로컬 검증 단계에서 즉시 실패해야 한다.

## 진행 현황

- [x] (2026-04-17 12:08+09:00) 작업 범위 확인 완료: 실제 위반 지점을 `GetMarketSummaryService -> MarketRealtimeService`, `ClosePositionService -> GrantProfitPointService`로 확정
- [x] (2026-04-17 12:10+09:00) 계획 문서 작성 및 사용자 직접 요청을 승인 신호로 기록
- [x] (2026-04-17 12:21+09:00) `red` 단계 완료: `ApplicationServiceDependencyRuleTest` 추가 후 현재 위반 두 건으로 실패 확인
- [x] (2026-04-17 12:29+09:00) `green` 단계 진행: market/reward 공유 책임을 비-Service application 협력 객체로 분리
- [x] (2026-04-17 12:32+09:00) `green` 단계 진행: `architectureLint`에 application service 직접 의존 금지 규칙 추가
- [x] (2026-04-17 15:20+09:00) `refactor` 단계 완료: backend 기준 문서와 관련 활성 계획 문서를 새 구조에 맞게 정리
- [x] (2026-04-17 15:28+09:00) 검증 완료: `./gradlew test --tests coin.coinzzickmock.architecture.ApplicationServiceDependencyRuleTest --tests coin.coinzzickmock.feature.market.api.MarketControllerTest --tests coin.coinzzickmock.feature.market.application.realtime.MarketRealtimeFeedTest`, `./gradlew architectureLint`, `./gradlew check` 통과
- [x] (2026-04-17 15:34+09:00) review gate 확인 완료: 변경 범위를 기준으로 독립 각도 검토를 수행했고 blocker 없음
- [ ] PR 생성

## 놀라움과 발견

- 관찰:
  현재 아키텍처 린트는 패키지 루트, 레이어 경계, domain/application/api 금지 import 정도만 검사하고 있어서 `application/service -> application/service` 의존은 통과한다.
  증거:
  [backend/build.gradle](/Users/hj.park/projects/coin-zzickmock/backend/build.gradle) 의 `architectureLint` rules 목록과 `application` 검사 블록에는 해당 규칙이 없다.

- 관찰:
  현재 코드 기준 실제 위반은 두 군데다.
  증거:
  [backend/src/main/java/coin/coinzzickmock/feature/market/application/service/GetMarketSummaryService.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/market/application/service/GetMarketSummaryService.java),
  [backend/src/main/java/coin/coinzzickmock/feature/position/application/service/ClosePositionService.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/position/application/service/ClosePositionService.java).

- 관찰:
  market 실시간 캐시와 reward 포인트 적립은 둘 다 "여러 유스케이스가 재사용할 수 있는 메커니즘"이지, 외부 진입점이 직접 호출하는 유스케이스는 아니었다.
  증거:
  `GetMarketSummaryService`는 조회 흐름만 가지면 충분했고, `MarketController`만 SSE 구독을 쓴다. `ClosePositionService`는 reward 적립만 필요했고, `GrantProfitPointService`는 별도 API 진입점이 없었다.

## 의사결정 기록

- 결정:
  `application/service`는 유스케이스 진입점만 두고, 공유 런타임/처리 로직은 `application.<purpose>` 하위 패키지의 비-Service 협력 객체로 분리한다.
  근거:
  서비스끼리 서로 호출하게 두는 것보다 "무엇이 유스케이스이고 무엇이 공유 메커니즘인지"를 이름과 패키지로 구분하는 편이 구조가 오래 간다.
  날짜/작성자:
  2026-04-17 / Codex

- 결정:
  회귀 방지는 테스트와 Gradle 린트를 둘 다 둔다.
  근거:
  테스트는 개발자가 구조 의도를 읽기 쉽게 문서화하고, 린트는 `check` 경로에서 항상 자동 실행되므로 둘을 같이 두는 편이 안전하다.
  날짜/작성자:
  2026-04-17 / Codex

- 결정:
  market 실시간 캐시는 `feature.market.application.realtime.MarketRealtimeFeed`, reward 적립은 `feature.reward.application.grant.RewardPointGrantProcessor`로 이동한다.
  근거:
  둘 다 유스케이스 진입점이 아니라 공유 메커니즘에 가깝기 때문에 `service`보다 목적형 비-Service 패키지가 구조를 더 잘 드러낸다.
  날짜/작성자:
  2026-04-17 / Codex

## 결과 및 회고

- `ApplicationServiceDependencyRuleTest`가 현재 구조 위반을 먼저 실패로 고정했다.
- market/reward 공유 책임을 각각 `application.realtime`, `application.grant`로 이동시켜 서비스 간 직접 참조를 제거했다.
- `architectureLint`에도 같은 금지 규칙을 추가해 테스트와 lint가 함께 회귀를 막도록 했다.
- `./gradlew architectureLint`와 `./gradlew check`가 모두 통과했고, 변경 범위 review gate에서도 blocker를 남기지 않았다.
- 남은 일은 작업 브랜치 정리, commit, push, PR 생성뿐이다.

## 맥락과 길잡이

관련 문서:

- [BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)
- [AGENTS.md](/Users/hj.park/projects/coin-zzickmock/AGENTS.md)
- [docs/design-docs/backend-design/01-architecture-foundations.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/01-architecture-foundations.md)

관련 코드:

- [backend/src/main/java/coin/coinzzickmock/feature/market/application/service/GetMarketSummaryService.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/market/application/service/GetMarketSummaryService.java)
- [backend/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketRealtimeFeed.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketRealtimeFeed.java)
- [backend/src/main/java/coin/coinzzickmock/feature/position/application/service/ClosePositionService.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/position/application/service/ClosePositionService.java)
- [backend/src/main/java/coin/coinzzickmock/feature/reward/application/grant/RewardPointGrantProcessor.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/reward/application/grant/RewardPointGrantProcessor.java)
- [backend/build.gradle](/Users/hj.park/projects/coin-zzickmock/backend/build.gradle)

이 작업에서는 "서비스"를 단순히 Spring `@Service` 빈 전부로 보지 않고, `feature/<name>/application/service` 아래에 놓인 유스케이스 클래스라는 뜻으로 쓴다.
금지 대상은 이 유스케이스 클래스가 다른 유스케이스 클래스를 생성자 주입, 필드 참조, 메서드 인자 등으로 직접 참조하는 경우다.

## 작업 계획

먼저 `backend/src/test/java` 아래에 아키텍처 회귀 테스트를 추가해, `application/service/*Service.java` 파일이 다른 `application/service/*Service.java` 이름을 소스에서 참조하면 실패하게 만든다.
현재 스냅샷에서는 위반 두 건이 있으므로 이 테스트는 먼저 실패해야 한다.

그 다음 market 기능에서는 실시간 캐시/구독 책임을 `application.realtime` 같은 목적형 패키지의 협력 객체로 옮기고, `GetMarketSummaryService`와 `MarketController`는 각자 필요한 범위만 그 객체에 의존하게 정리한다.
reward/position 경계에서는 포인트 적립 로직을 `reward.application`의 비-Service 처리 객체로 옮겨 `ClosePositionService`가 더 이상 `GrantProfitPointService`를 보지 않게 한다.

동시에 `backend/build.gradle`의 `architectureLint`에 `APPLICATION_SERVICE_NO_SERVICE_DEPENDENCY` 규칙을 추가해, 이후 동일 패턴이 생기면 `./gradlew architectureLint`와 `./gradlew check`가 모두 실패하도록 만든다.
마지막으로 backend 기준 문서와 활성 계획 문서에서 "서비스는 서비스에 의존하지 않는다"는 원칙과 새 패키지 예시를 반영한다.

## 구체적인 단계

1. `application/service` 회귀 테스트 파일을 추가한다.
2. `cd backend && ./gradlew test --tests ...` 로 실패를 확인한다.
3. market/reward 관련 공유 로직을 비-Service application 협력 객체로 이동한다.
4. 관련 서비스와 컨트롤러를 새 협력 객체 기준으로 다시 연결한다.
5. `backend/build.gradle`에 lint 규칙을 추가한다.
6. backend 문서와 활성 계획 문서를 갱신한다.
7. `cd backend && ./gradlew test`, `./gradlew architectureLint`, `./gradlew check`를 실행한다.
8. 변경 범위만 대상으로 품질 검토를 수행하고 점수를 남긴다.
9. 의미 있는 작업 브랜치에서 내 범위만 commit/push 하고 PR을 만든다.

## 검증과 수용 기준

실행 명령:

- `cd backend && ./gradlew test --tests coin.coinzzickmock.architecture.ApplicationServiceDependencyRuleTest`
- `cd backend && ./gradlew architectureLint`
- `cd backend && ./gradlew check`

수용 기준:

- `application/service` 아래 어떤 클래스도 다른 `application/service` 클래스를 직접 참조하지 않는다.
- market 기능의 실시간 캐시/구독은 계속 동작하되, 그 책임 클래스는 더 이상 `application/service` 패키지에 있지 않다.
- 포지션 종료 시 보상 포인트 적립은 계속 동작하되, `ClosePositionService`는 다른 서비스를 주입받지 않는다.
- `architectureLint` report에 새 규칙이 포함되고, 위반이 있으면 실패한다.
- backend 기준 문서가 같은 규칙을 명시한다.

## 반복 실행 가능성 및 복구

- 이 작업은 DB 스키마를 바꾸지 않으므로 반복 실행 시 마이그레이션 복구 절차가 필요 없다.
- lint 규칙 추가 후 기존 위반이 남아 있으면 `check`가 실패하는 것이 정상이다. 이 경우 구조 수정이 먼저다.
- 클래스 이동은 IDE 리팩터링 없이도 package/import 정리만으로 재시도 가능하다.
- 현재 작업트리에는 프론트/문서 관련 별도 변경도 있으므로, commit 단계에서는 이번 작업 파일만 선택적으로 staging 해야 한다.

## 산출물과 메모

- 예상 PR 제목:
  application service 직접 의존 금지
- 변경 메모:
  초안 단계에서 구조 위반 두 건과 lint 공백을 먼저 확정했다. 이후 구현이 진행되면 진행 현황, 의사결정 기록, 결과 및 회고를 계속 갱신한다.
- 리뷰 메모:
  Review target은 backend 구조 변경 및 관련 문서/계획 파일로 제한했다.
  가독성/성능/보안/테스트/아키텍처 관점 검토에서 unresolved blocker는 없었다.
