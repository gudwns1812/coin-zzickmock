# Backend Architecture Foundations

## Purpose

이 문서는 `coin-zzickmock` 백엔드 상세 설계의 첫 진입 문서다.
루트의 [BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)가 작업 기준과 입구 문서라면, 이 문서는 그 뒤에서 백엔드가 어떤 구조를 목표로 하는지와 어떤 세부 문서를 읽어야 하는지를 정리한다.

다른 규칙 문서를 모두 한 파일에 담지 않는다.
구조, 조립, Provider, DB, 테스트 규칙은 각각 별도 번호 문서에 둔다.

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

## Reading Guide

이 문서는 "무엇이 백엔드의 공통 구조인가"를 알려 주는 첫 문서다.
실제 작업에서는 아래처럼 필요한 문서로 내려간다.

### 구조와 패키지, bean 조립 규칙이 필요할 때

1. [02-package-and-wiring.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/02-package-and-wiring.md)

### 유스케이스 경계, Provider, 캐시, 서비스 분리가 필요할 때

1. [03-application-and-providers.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/03-application-and-providers.md)

### 도메인, DB, 예외, 네이밍 규칙이 필요할 때

1. [04-persistence-and-domain-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/04-persistence-and-domain-rules.md)
2. DB 작업이면 [docs/generated/db-schema.md](/Users/hj.park/projects/coin-zzickmock/docs/generated/db-schema.md)

### 테스트 구조와 `architectureLint` 규칙이 필요할 때

1. [05-testing-and-lint.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/05-testing-and-lint.md)

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

## Document Boundary Rule

이 디렉터리의 상세 설계는 번호 문서별 1차 책임을 유지해야 한다.

강한 규칙:

- 구조 규칙을 추가한다고 해서 DB/테스트/네이밍 규칙 문서까지 함께 키우지 않는다.
- 새로운 설계 규칙이 기존 문서 책임에 맞지 않으면 새 번호 문서를 추가한다.
- 상세 설계 문서를 수정하면 [README.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/README.md)도 함께 갱신해 다음 독자가 바로 찾을 수 있게 한다.

## Related Documents

- [README.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/README.md)
- [02-package-and-wiring.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/02-package-and-wiring.md)
- [03-application-and-providers.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/03-application-and-providers.md)
- [04-persistence-and-domain-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/04-persistence-and-domain-rules.md)
- [05-testing-and-lint.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/05-testing-and-lint.md)
