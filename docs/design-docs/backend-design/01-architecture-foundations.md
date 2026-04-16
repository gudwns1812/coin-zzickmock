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
- repository/port 호출
- `Providers` 사용
- 트랜잭션 경계 정의

금지:

- HTTP 세부사항 의존
- JPA 엔티티 세부 구현에 잠김
- Spring Security 세부 API 직접 사용
- 외부 SDK 직접 사용

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
- `application` -> feature 내부 port 또는 `providers`
- `infrastructure` -> `application`, `domain`, `providers`
- `domain` -> 자신과 `common`만

절대 금지:

- `domain` -> `application`
- `domain` -> `infrastructure`
- `application` -> 구체 SDK 클라이언트
- `api` -> `infrastructure`

## Package Shape

현재 패키지 루트는 `coin.coinzzickmock`이므로, 목표 구조는 아래를 기본값으로 한다.

```text
backend/src/main/java/coin/coinzzickmock/
  bootstrap/
    CoinZzickmockApplication.java
    config/
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
  feature/
    market/
      api/
      application/
        command/
        query/
        usecase/
        result/
        port/
      domain/
        model/
        service/
        policy/
      infrastructure/
        persistence/
        connector/
        mapper/
    member/
      api/
      application/
      domain/
      infrastructure/
```

강한 규칙:

- 최상위는 `bootstrap`, `common`, `providers`, `feature`만 사용한다.
- 기능 코드는 반드시 `feature/<feature-name>/` 아래에 둔다.
- `feature` 바깥에 새 업무용 패키지를 만들지 않는다.
- `support`, `core`, `extern`, `storage`처럼 기술/성격 기준의 광역 패키지는 새로 만들지 않는다.

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
  port/
    LoadAccountPort.java
    SaveOrderPort.java
  usecase/
    PlaceOrderUseCase.java
    PlaceOrderService.java
```

강한 규칙:

- 하나의 클래스가 여러 유스케이스를 포괄하지 않는다.
- `FooService` 하나에 여러 unrelated 메서드를 몰아넣지 않는다.
- 쓰기 작업은 command, 읽기 작업은 query 또는 read model을 분리한다.
- 트랜잭션은 유스케이스 단위로 잡는다.

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

규칙:

- JPA 엔티티는 도메인 모델과 동일 객체로 취급하지 않는다.
- 영속성 모델과 도메인 모델 사이의 변환 책임을 명시한다.
- 외부 API 응답 DTO를 application/domain으로 직접 전파하지 않는다.

DB를 참고하거나 수정하는 작업에서는 반드시 [docs/generated/db-schema.md](/Users/hj.park/projects/coin-zzickmock/docs/generated/db-schema.md)를 함께 본다.

- 스키마를 읽는 작업: 먼저 `db-schema.md`를 확인하고 현재 테이블/컬럼/관계를 파악한다.
- 스키마를 바꾸는 작업: 코드 변경과 함께 `db-schema.md`도 최신 상태로 갱신한다.
- 엔티티, repository, migration, SQL을 바꿨는데 `db-schema.md`가 그대로면 작업이 덜 끝난 것으로 본다.

## Exception Rule

예외 모델은 `CoreException` 중심으로 통일한다.

권장 구조:

```text
common/error/
  ErrorCode.java
  CoreException.java
  NotFoundException.java
  ExternalServiceException.java
  ErrorResponse.java
  GlobalExceptionHandler.java
```

규칙:

- 도메인/애플리케이션 실패는 구조화된 에러 코드로 표현한다.
- 외부 연동 실패는 infrastructure 또는 application 경계에서 번역한다.
- HTTP 응답 변환은 글로벌 핸들러 한 곳에서 수행한다.
- `catch (Exception)`은 경계에서 번역할 때만 허용한다.

## Naming Rule

클래스와 메서드는 역할이 드러나야 한다.

권장:

- `PlaceOrderUseCase`
- `LoadPortfolioPort`
- `JpaOrderRepositoryAdapter`
- `CurrentActor`
- `MarketDataConnector`

금지:

- `OrderManager`
- `OrderHelper`
- `CommonService`
- `Util`
- `Processor`

불리언은 반드시 술어형으로 작성한다.

- `isActive`
- `hasPosition`
- `canTrade`
- `shouldLiquidate`

## Spring Rule

Spring은 조립 도구이지 아키텍처가 아니다.

규칙:

- `@RestController`는 `api`에만 둔다.
- `@Configuration`은 `bootstrap/config` 또는 infrastructure config에 둔다.
- `@Entity`, `@Embeddable`은 infrastructure persistence 쪽에 둔다.
- `@Transactional`은 application 유스케이스 경계에서 사용한다.
- `domain`에는 Spring annotation을 두지 않는다.

## Test Rule

테스트도 레이어 경계를 반영해야 한다.

- domain: 빠른 단위 테스트
- application: 유스케이스 테스트, provider/port fake 사용
- infrastructure: repository/connector 통합 테스트
- api: controller slice 또는 통합 테스트

중요:

- application 테스트는 `Providers` fake로 인증/플래그/텔레메트리 조건을 통제할 수 있어야 한다.
- 외부 시스템 통합 테스트는 실제 SDK 호출보다 adapter 계약 검증에 집중한다.

## Architecture Lint Contract

백엔드에는 테스트 외에 프로젝트 전용 아키텍처 린트를 둔다.

- Gradle task: `./gradlew architectureLint`
- 통합 검증: `./gradlew check`
- 리포트 경로: `backend/build/reports/architecture-lint/violations.jsonl`

이 린터는 일반 스타일 린터가 아니라 구조 린터다.

- 최상위 패키지는 `bootstrap`, `common`, `providers`, `feature`만 허용
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
