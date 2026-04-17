# Backend Architecture Foundations

## Purpose

이 문서는 `coin-zzickmock` 백엔드의 상세 설계 원문이다.
루트의 [BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)가 작업 기준과 입구 문서라면, 이 문서는 그 기준 뒤에 있는 구조적 결정과 세부 형태를 설명한다.

## Architecture Target

백엔드는 다음 세 축을 동시에 만족해야 한다.

- `feature-first`
- 고정된 레이어 집합
- `Providers`를 통한 명시적 교차 관심사 접근

기본 철학은 "clean architecture lite"다.
레이어는 단순하지만 규칙은 엄격해야 한다.

핵심 원칙:

- 기능은 `feature` 아래에서 수직으로 자른다.
- 모든 기능은 같은 레이어 집합을 사용한다.
- 도메인은 프레임워크와 외부 SDK를 모른다.
- 애플리케이션은 유스케이스를 조합하지만 기술 세부사항을 모른다.
- 인프라는 영속성, 외부 시스템, 프레임워크 연결을 담당한다.
- 인증, 커넥터, 텔레메트리, 기능 플래그는 개별 기능 곳곳에서 직접 붙이지 않고 `Providers`를 통해서만 접근한다.
- 계층 분리를 이유로 `port`/`usecase` 인터페이스를 기계적으로 늘리지 않는다.
- 기본값은 concrete class다. 인터페이스는 실제 경계와 대체 구현이 있을 때만 도입한다.
- Spring이 관리하는 장기 협력 객체는 concrete class라도 각 클래스 안에서 직접 `new`하지 않고 빈 조립으로 연결한다.

## Fixed Layer Set

모든 기능은 아래 4개 레이어만 사용한다.

1. `api`
2. `application`
3. `domain`
4. `infrastructure`

이 집합은 고정이다.
새 기능을 만든다고 `service`, `util`, `helper`, `manager`, `common` 같은 임의 레이어를 추가하지 않는다.

### `api`

HTTP 요청/응답, 인증된 요청 컨텍스트 파싱, DTO 검증, 응답 매핑만 담당한다.

- `@RestController`
- request/response DTO
- request validation
- application input/result 매핑

금지:

- 엔티티 직접 반환
- 비즈니스 규칙 구현
- 리포지토리 직접 호출
- 외부 SDK 직접 호출

### `application`

유스케이스 실행과 트랜잭션 경계의 중심이다.

- command/query 모델
- use case orchestration
- domain 호출
- 필요한 repository/gateway/provider 계약 호출
- `Providers` 사용
- 트랜잭션 경계 정의
- `application/service`는 유스케이스 진입점만 담당
- 여러 유스케이스가 함께 쓰는 공유 런타임/처리 로직은 `application`의 목적별 하위 패키지로 분리

금지:

- HTTP 세부사항 의존
- JPA 엔티티 세부 구현에 잠김
- Spring Security 세부 API 직접 사용
- 외부 SDK 직접 사용
- `application/service`가 다른 `application/service`를 직접 주입하거나 호출

### `domain`

가장 오래 살아야 하는 규칙의 자리다.

- aggregate
- entity
- value object
- domain service
- domain policy
- domain event

금지:

- Spring annotation
- JPA repository 의존
- HTTP 응답 타입 의존
- 외부 API 클라이언트 의존
- feature flag SDK, telemetry SDK, security context 직접 사용

### `infrastructure`

프레임워크와 외부 기술을 연결하는 어댑터다.

- JPA/MyBatis/Redis 구현
- 외부 API/메시징 구현
- `Providers` 구현체
- mapper
- config

금지:

- 도메인 규칙의 원본 소유
- 컨트롤러 수준 요청 규칙 처리
- 유스케이스를 우회한 비즈니스 플로우 조립

## Dependency Rule

의존 방향은 아래만 허용한다.

- `api` -> `application`
- `application` -> `domain`
- `application` -> `providers`
- `application` -> 필요할 때만 `application` 또는 `domain`이 소유한 계약
- `infrastructure` -> `application`, `domain`, `providers`
- `domain` -> 자신과 `common`만

절대 금지:

- `domain` -> `application`
- `domain` -> `infrastructure`
- `application` -> 구체 SDK 클라이언트
- `api` -> `infrastructure`
- `application/service` -> 다른 `application/service`

여기서 말하는 "계약"은 무조건 인터페이스를 만들라는 뜻이 아니다.
기본값은 concrete class 의존이다.
다만 아래 조건 중 하나라도 충족하면 `application` 또는 `domain`이 소유하는 인터페이스를 둘 수 있다.

- 운영 구현이 둘 이상이거나 곧 둘 이상이 될 것이 명확할 때
- 외부 시스템 경계를 격리해야 하고, 그 계약이 애플리케이션 언어로 표현될 때
- 테스트를 위해서가 아니라, 런타임 책임 분리가 실제로 필요할 때

반대로 아래는 금지한다.

- 구현체가 하나뿐인데 메서드 전달만 하는 `*Port`
- 컨트롤러 하나만 쓰는데 형식적으로 만든 `*UseCase` 인터페이스
- 이미 `Providers`나 gateway가 있는데 그 위에 다시 한 겹 씌우는 중복 추상화
- Spring `@Service`, `@Component`, `@Configuration` 안에서 정책 객체나 암호화기 같은 협력 객체를 필드 초기화로 직접 `new`하는 패턴

## Bean Wiring Boundary

이 저장소에서 "기본값은 concrete class"는 "아무 데서나 직접 생성해도 된다"는 뜻이 아니다.
이 말의 목적은 인터페이스 남발을 막는 것이고, 조립 책임까지 각 유스케이스 클래스에 흩뿌리라는 뜻은 아니다.

강한 규칙:

- Spring이 관리하는 협력 객체는 concrete class라도 생성자 주입으로 연결한다.
- 같은 객체를 여러 유스케이스나 인프라 어댑터가 재사용할 수 있다면, 해당 객체의 생성 책임은 `infrastructure/config` 또는 provider configuration으로 모은다.
- `domain`은 Spring annotation을 모르므로 `domain policy`, `domain service`, 계산기 객체를 빈으로 쓰고 싶다면 도메인 클래스에는 annotation을 붙이지 않고 `feature/<name>/infrastructure/config`에서 `@Bean`으로 등록한다.
- `new`를 써도 되는 경우는 값 객체, 엔티티, 결과 DTO, 컬렉션 같은 "한 유스케이스 안에서 즉시 소비되는 짧은 수명 객체"를 만들 때다.
- `new`를 피해야 하는 경우는 정책 객체, 암호화기, 파서, 재사용 계산기처럼 장기 협력 객체를 Spring 관리 클래스 내부에서 붙들 때다.

예시:

- 허용: `new TradingAccount(...)`, `new RewardPointWallet(...)`
- 허용: `new ConcurrentHashMap<>()` 같은 내부 자료구조
- 금지: `@Service` 안에서 `private final RewardPointPolicy policy = new RewardPointPolicy();`
- 금지: `@Component` 안에서 `private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();`

## Package Shape

현재 패키지 루트는 `coin.coinzzickmock`이므로, 목표 구조는 아래를 기본값으로 한다.

```text
backend/src/main/java/coin/coinzzickmock/
  CoinZzickmockApplication.java
  common/
    api/
    error/
    annotation/
  providers/
    Providers.java
    auth/
    connector/
    telemetry/
    featureflag/
    infrastructure/
      config/
  feature/
    market/
      api/
      application/
        command/
        query/
        realtime/
        result/
        service/
      domain/
        model/
        service/
        policy/
      infrastructure/
        config/
        persistence/
        connector/
        mapper/
    member/
      api/
      application/
        grant/
      domain/
      infrastructure/
```

강한 규칙:

- 루트 패키지 바로 아래에는 `*Application` 진입점만 둔다.
- 루트 패키지의 최상위 하위 패키지는 `common`, `providers`, `feature`만 사용한다.
- 기능 코드는 반드시 `feature/<feature-name>/` 아래에 둔다.
- `feature` 바깥에 새 업무용 패키지를 만들지 않는다.
- `support`, `core`, `extern`, `storage`처럼 기술/성격 기준의 광역 패키지는 새로 만들지 않는다.
- `application/usecase`, `application/port`는 기본 골격이 아니다. 실제로 필요한 경우에만 추가한다.

## Providers

교차 관심사는 반드시 `Providers`를 통해서만 접근한다.

```java
public interface Providers {
    AuthProvider auth();
    ConnectorProvider connector();
    TelemetryProvider telemetry();
    FeatureFlagProvider featureFlags();
}
```

`Providers`는 교차 관심사의 유일한 진입점이다.
애플리케이션 유스케이스와 인프라 어댑터는 필요한 경우 이 인터페이스를 통해 기능을 사용한다.

### Application Service Boundary

`feature/<name>/application/service`는 컨트롤러나 다른 진입점이 호출하는 유스케이스 클래스만 둔다.
여러 유스케이스가 같이 쓰는 실시간 캐시, 적립 처리기, 조합기 같은 객체는 `application/<purpose>` 하위 패키지의 비-Service 협력 객체로 둔다.
이렇게 나누면 `service`는 "무슨 일을 시작하는가"를, 목적형 협력 객체는 "그 일을 어떤 메커니즘으로 지원하는가"를 드러낸다.

강한 규칙:

- `application/service`는 다른 `application/service`를 직접 참조하지 않는다.
- 공유 로직이 필요하면 먼저 `domain`으로 올릴 수 있는지 본다.
- `domain`으로 올릴 수 없고 애플리케이션 메커니즘에 가까우면 `application/<purpose>` 하위 패키지로 분리한다.

### Why Providers Exists

- 인증 방식을 바꿔도 유스케이스 시그니처를 흔들지 않기 위해
- 커넥터의 공통 정책을 한 곳에 모으기 위해
- 텔레메트리 호출을 흩뿌리지 않기 위해
- 기능 플래그 제품을 바꿔도 코드베이스를 오염시키지 않기 위해
- 테스트에서 교차 관심사를 쉽게 대체하기 위해

### Allowed Provider Categories

#### `AuthProvider`

- 현재 actor 조회
- 권한/역할 확인
- 시스템 배치 실행 여부 구분

금지:

- application/domain에서 `SecurityContextHolder` 직접 사용
- controller 밖에서 `Authentication` 타입 전파

#### `ConnectorProvider`

- 공통 인증
- 재시도, 타임아웃, 서킷브레이킹
- 공통 outbound logging
- 공통 observability tagging
- 외부 시스템별 gateway 노출

강한 규칙:

- 기능 코드에서 `RestClient`, `WebClient`, Feign, SDK를 직접 잡지 않는다.
- 저수준 HTTP 세부사항은 `ConnectorProvider` 구현에 숨긴다.

#### `TelemetryProvider`

- 유스케이스 시작/종료 기록
- 실패 유형 기록
- 비즈니스 이벤트 계측

금지:

- 애플리케이션 전역에 메트릭 라이브러리 API를 직접 흩뿌리는 것
- 도메인 모델 안에서 로그/메트릭 SDK 호출

#### `FeatureFlagProvider`

- flag on/off
- variant 조회
- actor/context 기반 평가

금지:

- 컨트롤러/서비스 곳곳에서 플래그 SDK 직접 호출
- 플래그 키 문자열을 하드코딩한 분기 확산

## Use Case Shape

애플리케이션은 유스케이스 중심으로 작성한다.

```text
feature/order/application/
  command/
    PlaceOrderCommand.java
  result/
    PlaceOrderResult.java
  service/
    PlaceOrderService.java
```

강한 규칙:

- 하나의 application service는 하나의 유스케이스 또는 강하게 응집된 흐름만 맡는다.
- `FooService` 하나에 여러 unrelated 메서드를 몰아넣지 않는다.
- 쓰기 작업은 command, 읽기 작업은 query 또는 read model을 분리한다.
- 트랜잭션은 유스케이스 단위로 잡는다.
- 기본 주입 대상은 concrete `*Service`다.
- `*UseCase` 인터페이스는 public contract가 실제로 필요한 경우에만 만든다.
- `*Port`는 외부 경계를 표현하는 계약이 정말 필요할 때만 만든다.
- 구현이 하나뿐이고 호출 전달만 하는 인터페이스는 만들지 않는다.

## Domain Rules

- 도메인은 비즈니스 언어로만 이름 짓는다.
- 도메인은 저장 방식이나 API 응답 형식을 모른다.
- 값 검증은 가능한 도메인 생성 시점에 닫는다.
- 상태 변경은 의미 있는 메서드로 노출한다.

권장:

- `activate()`
- `changeLeverage(...)`
- `closePosition(...)`

비권장:

- `update(...)`
- `process(...)`
- `handle(...)`

## Persistence And External Systems

영속성과 외부 연동은 모두 `infrastructure`에 둔다.

권장 구조:

- 영속성: `feature/<feature>/infrastructure/persistence`
- 외부 연동: `feature/<feature>/infrastructure/connector`
- 매핑: `feature/<feature>/infrastructure/mapper`

기본 선택:

- 운영 DB는 `MySQL`을 기준으로 설계한다.
- 테스트 DB는 인메모리 `H2`를 기본값으로 사용한다.
- DB 마이그레이션은 `Flyway`를 표준으로 사용한다.
- 저장소와 엔티티 기반 영속성 구현은 `Spring Data JPA`를 기본으로 한다.
- 동적 조건 조합과 타입 세이프 조회는 OpenFeign 포크 `QueryDSL`을 기본 선택지로 사용한다.
- JPA/QueryDSL로 유지하기 어려운 복잡한 native query에 한해 `JdbcTemplate` 사용을 허용한다.

규칙:

- JPA 엔티티는 도메인 모델과 동일 객체로 취급하지 않는다.
- 영속성 모델과 도메인 모델 사이의 변환 책임을 명시한다.
- 외부 API 응답 DTO를 application/domain으로 직접 전파하지 않는다.
- 단순 조회/저장은 JPA repository 또는 QueryDSL adapter에서 우선 해결한다.
- JPA repository, gateway, connector 위에 이유 없는 중간 인터페이스를 하나 더 만들지 않는다.
- 인터페이스가 꼭 필요하면 계약은 `application` 또는 `domain`이 소유하고, 구현은 `infrastructure`에 둔다.
- `JdbcTemplate`은 복잡한 집계, 윈도 함수, DB 전용 최적화처럼 JPA/QueryDSL 추상화보다 SQL 자체가 더 명확한 경우에만 제한적으로 사용한다.
- `JdbcTemplate`을 쓰더라도 SQL은 `infrastructure/persistence`에만 두고, application/domain에는 노출하지 않는다.
- 스키마 변경의 원문은 `Flyway` migration으로 남기고, `db-schema.md`는 그 결과를 통합 요약하는 문서로 유지한다.
- 테스트는 운영과 다른 DB 엔진을 쓰더라도 MySQL 동작과 의미가 어긋나지 않게 작성한다.

DB를 참고하거나 수정하는 작업에서는 반드시 [docs/generated/db-schema.md](/Users/hj.park/projects/coin-zzickmock/docs/generated/db-schema.md)를 함께 본다.

- 스키마를 읽는 작업: 먼저 `db-schema.md`를 확인하고 현재 테이블/컬럼/관계를 파악한다.
- 스키마를 바꾸는 작업: 먼저 `backend/src/main/resources/db/migration` 아래에 새 버전의 `Flyway` migration 파일을 추가하고, 코드 변경과 함께 `db-schema.md`도 최신 상태로 갱신한다.
- 엔티티, repository, migration, SQL을 바꿨는데 `db-schema.md`가 그대로면 작업이 덜 끝난 것으로 본다.

## Exception Rule

예외 모델은 `CoreException` 중심으로 통일한다.

권장 구조:

```text
common/error/
  ErrorCode.java
  CoreException.java
  ErrorResponse.java
  GlobalExceptionHandler.java
```

규칙:

- 도메인/애플리케이션 실패는 구조화된 에러 코드로 표현한다.
- `CoreException`은 기본 예외 타입 하나로 유지하고, `BadRequestException`, `NotFoundException`처럼 상태별 하위 클래스를 기계적으로 만들지 않는다.
- 외부 연동 실패는 infrastructure 또는 application 경계에서 번역한다.
- HTTP 응답 변환은 글로벌 핸들러 한 곳에서 수행한다.
- `catch (Exception)`은 경계에서 번역할 때만 허용한다.

## Naming Rule

클래스와 메서드는 역할이 드러나야 한다.

권장:

- `PlaceOrderService`
- `TradingAccountRepository`
- `OrderPersistenceRepository`
- `CurrentActor`
- `MarketDataConnector`
- `TradingAccountEntity`

금지:

- `LoadPortfolioPort`
- `GetMarketSummaryUseCase`
- `OrderManager`
- `OrderHelper`
- `CommonService`
- `TradingAccountJpaEntity`

추가 규칙:

- 엔티티, repository 구현, Spring Data 인터페이스 이름에는 `Jpa`, `SpringData`, `MyBatis`, `Redis` 같은 기술명을 넣지 않는다.
- 기술 세부사항은 패키지 경로, annotation, `JpaRepository` 같은 프레임워크 타입으로만 드러내고 클래스명에는 넣지 않는다.
- `Util`
- `Processor`

단, 위 금지 예시는 이름 자체보다 "쓸모 없는 한 겹 추상화"를 금지한다는 뜻이다.
실제 다중 구현 계약이라면 인터페이스를 쓸 수 있지만, 그 경우에도 역할 이름이 먼저 드러나야 한다.

불리언은 반드시 술어형으로 작성한다.

- `isActive`
- `hasPosition`
- `canTrade`
- `shouldLiquidate`

## Spring Rule

Spring은 조립 도구이지 아키텍처가 아니다.

규칙:

- `@RestController`는 `api`에만 둔다.
- `@Configuration`은 owner가 드러나는 `feature/.../infrastructure/config` 또는 `providers/infrastructure/config`에 둔다.
- 앱 시작 시 필요한 seed나 초기화도 전역 bucket이 아니라 소유 feature의 `infrastructure/config`에서 선언한다.
- `@Entity`, `@Embeddable`은 infrastructure persistence 쪽에 둔다.
- `@Transactional`은 application 유스케이스 경계에서 사용한다.
- `domain`에는 Spring annotation을 두지 않는다.
- `@RestController`가 application service 대신 형식적인 `*UseCase` 인터페이스만 바라보도록 강제하지 않는다.
- 스프링이 관리하는 클래스에서 final 필드 생성자 주입만 필요할 때는 수동 생성자 대신 Lombok `@RequiredArgsConstructor`를 기본값으로 사용한다.
- 생성자 안에서 값 검증, 정규화, 파생 필드 계산 같은 추가 로직이 있을 때만 수동 생성자를 남긴다.

## Test Rule

테스트도 레이어 경계를 반영해야 한다.

- domain: 빠른 단위 테스트
- application: 유스케이스 테스트, provider/필요한 계약 fake 사용
- infrastructure: repository/connector 통합 테스트
- api: controller slice 또는 통합 테스트

중요:

- application 테스트는 `Providers` fake로 인증/플래그/텔레메트리 조건을 통제할 수 있어야 한다.
- 테스트 더블이 필요하다는 이유만으로 production 인터페이스를 먼저 만들지 않는다.
- 외부 시스템 통합 테스트는 실제 SDK 호출보다 adapter 계약 검증에 집중한다.
- DB가 필요한 테스트는 기본적으로 인메모리 `H2`를 사용한다.
- 테스트용 DDL/쿼리는 가능하면 MySQL과 의미 차이가 나지 않게 유지하고, 차이가 생기면 그 이유를 테스트 코드나 설정에서 드러낸다.

## Architecture Lint Contract

백엔드에는 테스트 외에 프로젝트 전용 아키텍처 린트를 둔다.

- Gradle task: `./gradlew architectureLint`
- 통합 검증: `./gradlew check`
- 리포트 경로: `backend/build/reports/architecture-lint/violations.jsonl`

이 린터는 일반 스타일 린터가 아니라 구조 린터다.

- 루트 패키지에는 `*Application`만 허용하고, 최상위 하위 패키지는 `common`, `providers`, `feature`만 허용
- `support`, `core`, `extern`, `storage` 같은 레거시 광역 패키지 금지
- `feature.<name>.<layer>` 구조 강제
- `domain` 레이어의 Spring/JPA 의존 금지
- `application` 레이어의 SecurityContext 직접 접근 금지
- `api` 레이어의 persistence import 직접 의존 금지

로그 출력 규칙:

- stdout에도 JSON line으로 출력한다.
- 같은 내용을 `violations.jsonl`에도 남긴다.
- 각 레코드는 `tool`, `event`, `rule`, `file`, `line`, `message`, `suggestedFix`를 가진다.

권장 조회 예:

- `rg '"event":"architecture_violation"' backend/build/reports/architecture-lint/violations.jsonl`
- `rg '"rule":"DOMAIN_FRAMEWORK_FREE"' backend/build/reports/architecture-lint/violations.jsonl`

즉, 루프는 아래처럼 동작한다.

1. `./gradlew architectureLint --console=plain`
2. JSONL violation 확인
3. `message`와 `suggestedFix`를 다음 수정 입력으로 사용
4. 수정 후 다시 lint 실행
