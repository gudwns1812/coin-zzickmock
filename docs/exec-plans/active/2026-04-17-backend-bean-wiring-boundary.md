# 스프링 빈 직접 생성 금지 정리

이 계획서는 [PLANS.md](/Users/hj.park/projects/coin-zzickmock/PLANS.md)와 [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)를 따른다.
이 문서는 백엔드에서 스프링이 관리하는 협력 객체를 클래스 내부에서 직접 `new`로 만들지 않고 빈 조립으로 통일하는 작업의 단일 기준서다.

## 목적 / 큰 그림

현재 백엔드에는 Spring 관리 클래스가 정책 객체와 암호화기를 직접 `new`로 생성하는 코드가 남아 있었다.
이 패턴은 협력 관계를 컨테이너 밖으로 빼내고, `domain`이 Spring을 몰라야 한다는 원칙과도 충돌하기 쉬웠다.

이 작업이 끝나면 정책 객체와 암호화기는 `infrastructure/config`에서 빈으로 등록되고, Spring 관리 클래스는 생성자 주입만 사용한다.
또한 테스트 프로필은 컨텍스트별 H2 메모리 DB를 사용해 테스트 스위트 간섭을 줄인다.

## 진행 현황

- [x] (2026-04-17 20:18+09:00) 직접 생성 패턴 대상 확인: `RewardPointGrantProcessor`, `CreateOrderService`, `BcryptMemberPasswordHasher`
- [x] (2026-04-17 20:19+09:00) bean wiring 회귀 테스트 추가 및 focused test 통과
- [x] (2026-04-17 20:34+09:00) `application-test.yml`에서 컨텍스트별 H2 DB 분리 후 `./gradlew check` 통과
- [x] (2026-04-17 20:36+09:00) self-review 완료: blocker 없음, final score 94
- [ ] PR 생성

## 놀라움과 발견

- 관찰:
  CI에서 보인 대량의 `UnsatisfiedDependencyException`은 실제로 정책/암호화기 빈 미등록과 정확히 일치했다.
  증거:
  `CreateOrderService`, `RewardPointGrantProcessor`, `BcryptMemberPasswordHasher`가 모두 직접 `new`를 쓰고 있었다.

- 관찰:
  전체 test suite의 마지막 blocker는 bean wiring이 아니라 테스트 컨텍스트 간 H2 메모리 DB 공유였다.
  증거:
  `application-test.yml`의 datasource URL을 `coinzzickmock-${random.uuid}`로 바꾸기 전에는 `./gradlew check`에서 `JpaMarketHistoryRepositoryTest`가 실패했고, 변경 후 통과했다.

## 의사결정 기록

- 결정:
  정책 객체는 `domain`에 두고, 빈 등록은 `infrastructure/config`에서 한다.
  근거:
  `domain`은 Spring annotation을 몰라야 하기 때문이다.
  날짜/작성자:
  2026-04-17 / Codex

- 결정:
  테스트 프로필에서는 컨텍스트마다 고유한 H2 메모리 DB를 사용한다.
  근거:
  suite 실행 시 컨텍스트 간 상태 간섭을 줄여 CI 안정성을 높인다.
  날짜/작성자:
  2026-04-17 / Codex

## 결과 및 회고

- bean wiring 경계가 코드와 문서에 함께 반영됐다.
- focused test, `architectureLint`, `check`가 모두 통과했다.
- self-review 기준으로 unresolved finding 없이 final score 94로 정리했다.
