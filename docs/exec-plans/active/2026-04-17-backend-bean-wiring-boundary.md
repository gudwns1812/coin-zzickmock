# 스프링 빈 직접 생성 금지 정리

이 계획서는 [PLANS.md](/Users/hj.park/projects/coin-zzickmock/PLANS.md)와 [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)를 따른다.
이 문서는 백엔드에서 스프링이 관리하는 협력 객체를 클래스 내부에서 `new`로 직접 만들지 않고 빈 조립으로 통일하는 작업의 단일 기준서다.
사용자 요청이 곧 작업 승인으로 해석되는 범위라서, 본 문서는 승인 직후 상태로 `active`에 둔다.

## 목적 / 큰 그림

현재 백엔드에는 Spring `@Component` 또는 `@Service`가 협력 객체를 직접 `new`로 생성하는 코드가 남아 있다.
이 패턴은 협력 관계를 컨테이너 밖으로 빼내 버려 설정 가능성과 테스트 구성을 줄이고, "기본값은 concrete class"라는 원칙을 "아무 데서나 직접 생성"으로 오해하게 만든다.

이 작업이 끝나면 정책 객체와 인프라 협력 객체는 모두 스프링 빈으로 조립되고, 스프링 관리 객체는 생성자 주입으로만 이들을 받는다.
또한 백엔드 설계 문서에는 "concrete class 우선"과 "스프링 빈은 직접 `new`하지 않는다"의 경계를 함께 적어, 이후 같은 해석 혼선을 줄인다.

## 진행 현황

- [x] (2026-04-17 19:02+09:00) 작업 범위 조사 완료: `RewardPointGrantProcessor`, `CreateOrderService`, `BcryptMemberPasswordHasher`를 직접 생성 패턴 후보로 확정
- [x] (2026-04-17 19:05+09:00) 계획 문서 작성 및 관련 기준 문서 연결 완료
- [x] (2026-04-17 19:12+09:00) `red` 단계 완료: `BackendBeanWiringTest` 추가 후 누락된 정책/암호화기 빈 때문에 실패 확인
- [x] (2026-04-17 19:18+09:00) `green` 단계 완료: 정책 객체와 `BCryptPasswordEncoder`를 설정 빈으로 등록하고 생성자 주입으로 전환
- [x] (2026-04-17 19:20+09:00) `refactor` 단계 진행: 설계 문서에 bean wiring 경계 추가, 백엔드 직접 생성 패턴 재검색 완료
- [ ] 검증: 관련 테스트, `./gradlew architectureLint`, `./gradlew check`
- [ ] 품질 점수 확인
- [ ] PR 생성

## 놀라움과 발견

- 관찰:
  현재 스프링 관리 객체 안에서 `private final ... = new ...()` 패턴은 많지 않지만, 남아 있는 것들이 전부 "정책 계산기"나 "인프라 협력 객체"라서 규칙으로 일반화하기 좋다.
  증거:
  `rg -n "private final .* = new [A-Z]" backend/src/main/java` 결과로 `RewardPointGrantProcessor`, `CreateOrderService`, `BcryptMemberPasswordHasher`가 확인됐다.

- 관찰:
  domain 규칙 문서에 따라 정책 클래스 자체에 Spring annotation을 붙이면 설계 원칙과 충돌한다.
  증거:
  [docs/design-docs/backend-design/01-architecture-foundations.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/01-architecture-foundations.md)의 `domain` 금지 규칙에 `Spring annotation`이 명시돼 있다.

## 의사결정 기록

- 결정:
  "기본값은 concrete class" 원칙은 인터페이스 남발을 막는 규칙으로 유지하되, 스프링 관리 협력 객체는 concrete class라 하더라도 빈으로 등록하고 주입받게 한다.
  근거:
  concrete class 우선과 DI 컨테이너 사용은 충돌하지 않는다. 오히려 concrete class를 빈으로 등록하면 과도한 추상화 없이도 조립 책임을 한곳에 둘 수 있다.
  날짜/작성자:
  2026-04-17 / Codex

## 결과 및 회고

- 현재까지 정책 객체와 `BCryptPasswordEncoder`는 각 feature의 `infrastructure/config`에서 빈으로 조립되도록 바뀌었다.
- `RewardPointGrantProcessor`, `CreateOrderService`, `BcryptMemberPasswordHasher`는 직접 생성 대신 생성자 주입으로 전환됐다.
- 남은 일은 Gradle 검증, 품질 점수 기록, PR 생성이다.

## 맥락과 길잡이

관련 문서:

- [BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)
- [QUALITY_SCORE.md](/Users/hj.park/projects/coin-zzickmock/QUALITY_SCORE.md)
- [docs/design-docs/backend-design/01-architecture-foundations.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/01-architecture-foundations.md)

관련 코드:

- [backend/src/main/java/coin/coinzzickmock/feature/reward/application/grant/RewardPointGrantProcessor.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/reward/application/grant/RewardPointGrantProcessor.java)
- [backend/src/main/java/coin/coinzzickmock/feature/reward/domain/RewardPointPolicy.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/reward/domain/RewardPointPolicy.java)
- [backend/src/main/java/coin/coinzzickmock/feature/order/application/service/CreateOrderService.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/order/application/service/CreateOrderService.java)
- [backend/src/main/java/coin/coinzzickmock/feature/order/domain/OrderPreviewPolicy.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/order/domain/OrderPreviewPolicy.java)
- [backend/src/main/java/coin/coinzzickmock/feature/member/infrastructure/security/BcryptMemberPasswordHasher.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/member/infrastructure/security/BcryptMemberPasswordHasher.java)

이 작업에서 말하는 "직접 생성 금지"는 도메인 모델 생성 자체를 금지한다는 뜻이 아니다.
`TradingAccount`, `RewardPointWallet`, `OrderPreview`처럼 한 번의 유스케이스에서 만들어 쓰는 값 객체나 결과 객체는 계속 `new`로 만든다.
이번에 금지하는 대상은 Spring 컨테이너가 이미 조립할 수 있는 장기 협력 객체, 즉 정책 클래스, 암호화기, 계산기 같은 재사용 협력 객체를 Spring 빈 내부에서 다시 `new`로 붙드는 패턴이다.

## 작업 계획

먼저 현재 구조를 고정하는 테스트를 보강한다.
`CreateOrderServiceTest`는 서비스 생성자에 `OrderPreviewPolicy`를 주입하는 형태로 바꿔도 기존 주문 미리보기/실행 동작이 유지되는지 확인하는 쪽으로 수정한다.
reward 쪽은 도메인 정책 단위 테스트를 유지하면서, 필요하면 Spring 컨텍스트 기동 테스트를 통해 `RewardPointPolicy` 빈 조립이 깨지지 않는지 확인한다.

그 다음 `RewardPointPolicy`와 `OrderPreviewPolicy`를 Spring 빈으로 등록하고, `RewardPointGrantProcessor`와 `CreateOrderService`는 생성자 주입으로 바꾼다.
`BcryptMemberPasswordHasher`는 `BCryptPasswordEncoder`를 직접 만들지 않고 구성 빈에서 주입받도록 바꿔 인프라 협력 객체 생성도 같은 원칙으로 맞춘다.

마지막으로 백엔드 설계 문서에는 "Spring 관리 객체 안에서 정책/협력 객체를 직접 생성하지 않는다"는 규칙과 예시를 추가하고, 전체 백엔드 검색으로 남은 유사 패턴이 없는지 다시 확인한다.

## 구체적인 단계

1. 관련 테스트와 생성자 사용 지점을 읽고 필요한 `red` 범위를 정한다.
2. 테스트 코드를 먼저 수정해 새 생성자 시그니처와 기대 동작을 고정한다.
3. 정책 객체와 암호화기를 Spring 빈으로 등록한다.
4. 스프링 관리 클래스의 직접 생성 패턴을 생성자 주입으로 바꾼다.
5. 설계 문서를 갱신하고 전수 검색으로 유사 패턴이 남지 않았는지 확인한다.
6. `cd backend && ./gradlew test --tests ...`, `./gradlew architectureLint`, `./gradlew check`를 실행한다.
7. 변경 범위만 대상으로 품질 검토를 수행하고 점수를 기록한다.
8. 내 변경만 stage/commit/push 후 PR을 생성한다.

## 검증과 수용 기준

실행 명령:

- `cd backend && ./gradlew test --tests coin.coinzzickmock.feature.order.application.service.CreateOrderServiceTest --tests coin.coinzzickmock.feature.reward.domain.RewardPointPolicyTest`
- `cd backend && ./gradlew architectureLint`
- `cd backend && ./gradlew check`

수용 기준:

- `RewardPointGrantProcessor`와 `CreateOrderService`는 더 이상 정책 객체를 직접 `new`하지 않는다.
- `BcryptMemberPasswordHasher`는 `BCryptPasswordEncoder`를 직접 `new`하지 않고 빈 주입을 사용한다.
- 백엔드 설계 문서는 스프링 관리 협력 객체의 직접 생성 금지 원칙을 명시한다.
- `rg -n "private final .* = new [A-Z]" backend/src/main/java` 재실행 시 이번 규칙 대상이 되는 패턴이 남지 않는다.

## 반복 실행 가능성 및 복구

- 이 작업은 DB 스키마를 바꾸지 않으므로 마이그레이션 복구는 필요 없다.
- 정책 객체를 `@Component`로 바꾸더라도 생성자 주입만 맞추면 반복 실행 시 부작용이 없다.
- 현재 워킹트리에 다른 작업이 이미 섞여 있으므로 commit 단계에서는 이번 작업 파일만 선택적으로 stage 해야 한다.

## 산출물과 메모

- 예상 PR 제목:
  Wire backend policies through Spring beans
- 변경 메모:
  초안 단계에서 스프링 관리 객체 내부의 직접 생성 패턴을 먼저 조사했고, 정책 객체와 인프라 협력 객체를 하나의 규칙으로 묶을 수 있음을 확인했다.
