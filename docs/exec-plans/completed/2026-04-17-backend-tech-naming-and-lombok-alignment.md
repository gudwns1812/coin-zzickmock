# 백엔드 기술명 네이밍과 Lombok 생성자 정렬

이 ExecPlan은 살아 있는 문서입니다. `진행 현황`, `놀라움과 발견`, `의사결정 기록`, `결과 및 회고` 섹션은 작업이 진행되는 내내 최신 상태로 유지해야 합니다.

이 문서는 `/Users/hj.park/projects/coin-zzickmock/PLANS.md` 기준을 따른다. 이번 작업은 `/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md`, `/Users/hj.park/projects/coin-zzickmock/BACKEND.md`, `/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/01-architecture-foundations.md`를 함께 기준으로 삼는다.

## 목적 / 큰 그림

백엔드 클래스 이름에서 `Jpa`, `SpringData` 같은 기술 이름을 걷어내고, 스프링 빈의 단순 생성자 주입 보일러플레이트는 Lombok으로 줄인다. 이 작업이 끝나면 영속성 클래스 이름은 역할 중심으로 읽히고, 스프링 관리 클래스는 생성자 코드 노이즈 없이 필요한 의존성만 드러낸다. 검증은 아키텍처 린트와 백엔드 테스트를 통해 수행한다.

## 진행 현황

- [x] 2026-04-17 16:20+09:00 관련 기준 문서와 현재 코드에서 네이밍/Lombok 규칙의 어긋남을 확인했다.
- [x] 2026-04-17 15:45+09:00 문서 원문과 아키텍처 린트 규칙을 사용자 의도에 맞게 갱신했다.
- [x] 2026-04-17 15:50+09:00 `Jpa*`, `*SpringDataRepository` 이름을 역할 중심 이름으로 바꾸고 참조를 정리했다.
- [x] 2026-04-17 15:52+09:00 단순 생성자 주입 보일러플레이트를 Lombok `@RequiredArgsConstructor`로 바꿨다.
- [x] 2026-04-17 16:05+09:00 변경 범위를 직접 리뷰하고 `git diff --cached --check`로 포맷/공백 문제 없음을 확인했다.
- [x] 2026-04-17 15:55+09:00 `./gradlew architectureLint`, `./gradlew check`를 통과했다.
- [x] 2026-04-17 16:08+09:00 브랜치 `backend-naming-lombok-alignment`를 푸시하고 PR #12를 생성했다.

## 놀라움과 발견

- 관찰: 초기 조사 시점에는 상세 설계 문서가 엔티티 이름에서만 기술명을 금지하고, repository adapter나 config에서는 기술명을 드러내도 된다고 적고 있었다.
  증거: 작업 시작 전 `docs/design-docs/backend-design/01-architecture-foundations.md`의 `Naming Rule` 섹션에 `JpaOrderRepositoryAdapter` 예시와 `기술 세부사항은 repository adapter, config, connector 이름에서만 드러낸다.` 문장이 함께 있었다.

- 관찰: Lombok은 이미 일부 controller와 gateway에만 부분 적용되어 있고, 동일한 성격의 다른 스프링 빈들은 수동 생성자를 유지하고 있었다.
  증거: 작업 시작 전 `AccountController`, `MarketController`, `RewardController`는 `@RequiredArgsConstructor`를 쓰지만 `AuthController`, `GetAccountSummaryService`, `JpaAccountRepository` 등은 명시적 생성자를 유지했다.

- 관찰: `MarketHistoryPersistenceRepository`에서 upsert 조회를 `findBy...`로 좁히면 기존 테스트가 깨졌다.
  증거: `MarketHistoryPersistenceRepositoryTest`가 `count("market_candles_1m") == 1` 기대에서 실패했고, 기존 범위 조회 방식으로 되돌린 뒤 `./gradlew check`가 통과했다.

## 의사결정 기록

- 결정: 이번 정리는 문서, 린트, 코드 이름, 테스트 이름을 한 번에 맞춘다.
  근거: 문서만 바꾸면 기존 클래스 이름이 계속 살아남고, 코드만 바꾸면 기준 문서가 다시 예외를 허용해 회귀한다.
  날짜/작성자: 2026-04-17 / Codex

- 결정: 기술명 금지 대상은 우선 우리 소스 안의 영속성 관련 클래스와 인터페이스 이름(`Jpa*`, `*SpringDataRepository`)으로 잡는다.
  근거: 사용자가 지적한 실제 위반이 이 범주에 몰려 있고, 패키지 경로와 Spring/JPA 타입 자체로 구현 세부는 충분히 드러난다.
  날짜/작성자: 2026-04-17 / Codex

- 결정: Lombok 적용은 스프링 관리 클래스 중 "생성자가 단순 필드 대입만 하는 경우"에 한정한다.
  근거: 생성자에 검증이나 전처리 로직이 있는 경우까지 기계적으로 치환하면 의도가 흐려질 수 있다.
  날짜/작성자: 2026-04-17 / Codex

## 결과 및 회고

문서, 린트, 코드 이름, Lombok 생성자 규칙을 한 번에 맞췄다. persistence 구현체와 Spring Data 인터페이스에서 `Jpa`, `SpringData` 같은 기술명을 제거했고, 관련 테스트 이름과 문서 링크도 함께 갱신했다. 스프링 관리 클래스의 단순 생성자 주입은 `@RequiredArgsConstructor`로 정리해 생성자 보일러플레이트를 줄였다.

검증 단계에서는 `MarketHistoryPersistenceRepository`의 upsert 조회를 `findBy...`로 좁히면 기존 테스트가 깨진다는 점을 확인했고, 원래의 범위 조회 방식으로 되돌려 동작 회귀 없이 마무리했다. 최종적으로 `./gradlew architectureLint`, `./gradlew check`를 통과했고 PR #12까지 생성했다.

## 맥락과 길잡이

기술명 네이밍 위반은 작업 시작 시점에 `backend/src/main/java/coin/coinzzickmock/feature/*/infrastructure/persistence` 아래에 모여 있었다. 예를 들어 `JpaAccountRepository`, `JpaOrderRepository`, `JpaPositionRepository`, `JpaMarketHistoryRepository`와 `TradingAccountSpringDataRepository`, `MemberCredentialSpringDataRepository` 같은 타입이 있었다. 이들은 모두 infrastructure/persistence 패키지 안에 있으므로 패키지 경로만으로도 영속성 책임이 드러난다.

Lombok 정리는 스프링이 직접 관리하는 클래스에서만 한다. 대상 후보는 `@Service`, `@Component`, `@Repository`, `@RestController`가 붙어 있고, 생성자 본문이 `this.field = field;` 대입만 수행하는 파일이다. JPA 엔티티나 record, 예외 타입은 이번 범위에 넣지 않는다.

## 작업 계획

먼저 상세 설계 문서의 `Naming Rule`과 `Spring Rule` 근처 문구를 고쳐 기술명을 이름에 넣지 않는 원칙과 Lombok 기본 규칙을 명확히 적는다. 다음으로 `backend/build.gradle`의 아키텍처 린트에서 `JpaEntity` 파일명만 잡던 규칙을 확장해, persistence 소스 파일 이름에 `Jpa` 또는 `SpringData` 기술명이 들어가면 실패하게 만든다.

그 다음 실제 코드에서 persistence 구현체와 Spring Data 인터페이스 이름을 역할 중심 이름으로 바꿨다. 예를 들어 `JpaAccountRepository`는 `AccountPersistenceRepository`로, `TradingAccountSpringDataRepository`는 `TradingAccountEntityRepository`로 바꿨고 테스트와 문서 링크도 같이 갱신했다. 이어서 스프링 관리 클래스 중 수동 생성자만 남아 있는 파일들에 Lombok `@RequiredArgsConstructor`를 적용해 생성자 본문을 제거했다.

## 구체적인 단계

1. 문서와 린트 규칙을 수정한다.
2. persistence 클래스/인터페이스와 테스트 클래스 이름을 바꾸고 import 및 참조를 정리한다.
3. 수동 생성자만 있는 스프링 빈에 `@RequiredArgsConstructor`를 적용한다.
4. `cd backend && ./gradlew architectureLint --console=plain`를 실행한다.
5. `cd backend && ./gradlew check --console=plain`를 실행한다.
6. 변경 범위만 대상으로 품질 리뷰를 수행한다.

## 검증과 수용 기준

수용 기준은 아래와 같다.

- `backend/src/main/java`와 `backend/src/test/java`에 `Jpa*` 또는 `*SpringDataRepository` 이름을 가진 우리 소스 클래스가 남아 있지 않다.
- 상세 설계 문서에 "기술명을 클래스명에 넣지 않는다"와 "스프링 관리 클래스의 단순 생성자 주입은 Lombok `@RequiredArgsConstructor`를 기본값으로 한다"는 기준이 적혀 있다.
- `./gradlew architectureLint`가 통과한다.
- `./gradlew check`가 통과한다.

## 반복 실행 가능성 및 복구

이 작업은 이름 변경과 Lombok 치환 중심이라 반복 실행해도 안전하다. 이름 변경 중 컴파일 에러가 생기면 IDE 자동 import나 빌드 오류를 기준으로 빠진 참조를 채우면 된다. 린트 규칙이 과도하게 넓어져 예상 밖 파일까지 막는 경우에는 해당 규칙 범위를 persistence 패키지로 다시 좁힌다.

## 산출물과 메모

검증이 끝나면 이 섹션에 핵심 로그와 대표 변경 파일을 짧게 남긴다.

## 인터페이스와 의존성

이번 작업은 새 라이브러리를 추가하지 않는다. 기존 `org.projectlombok:lombok` 의존성을 그대로 사용하며, 스프링 관리 클래스는 `@RequiredArgsConstructor`를 통해 final 필드 기반 생성자 주입을 유지한다. 영속성 인터페이스는 여전히 `JpaRepository<...>`를 확장하지만, 이름은 기술명이 아니라 역할 중심으로 유지해야 한다.
