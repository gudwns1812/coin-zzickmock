# Backend Application And Providers

## Purpose

이 문서는 유스케이스 경계, 레이어 간 의존 방향, `Providers`, 캐시와 공유 메커니즘의 배치를 설명한다.
`application/service`가 어디까지를 맡고, 어디서부터 목적형 협력 객체나 Provider 경계로 분리해야 하는지 여기서 확인한다.

먼저 읽어야 하는 문서:

- [README.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/README.md)
- [01-architecture-foundations.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/01-architecture-foundations.md)

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

## Application Service Boundary

`feature/<name>/application/service`는 컨트롤러나 다른 진입점이 호출하는 유스케이스 클래스만 둔다.
여러 유스케이스가 같이 쓰는 실시간 캐시, 적립 처리기, 조합기 같은 객체는 `application/<purpose>` 하위 패키지의 비-Service 협력 객체로 둔다.

강한 규칙:

- `application/service`는 다른 `application/service`를 직접 참조하지 않는다.
- 공유 로직이 필요하면 먼저 [04-domain-modeling-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/04-domain-modeling-rules.md) 기준으로 `domain` 후보인지 본다.
- `domain`으로 올릴 수 없고 애플리케이션 메커니즘에 가까우면 `application/<purpose>` 하위 패키지로 분리한다.
- `application/service`는 "무슨 일을 시작하는가"를, 목적형 협력 객체는 "그 일을 어떤 메커니즘으로 지원하는가"를 드러내야 한다.

## Cache Boundary

캐시는 "값을 잠깐 들고 있다가 다시 쓰는 메커니즘"이다.
유스케이스 코드 안에 `ConcurrentHashMap` 같은 ad-hoc 저장소를 직접 박아 넣기보다 Spring이 관리하는 경계 뒤에 두는 것이 기본값이다.

강한 규칙:

- 단일 인스턴스 안에서만 필요한 로컬 캐시는 Spring Cache를 기본값으로 사용한다.
- 여러 인스턴스가 값을 공유해야 하는 분산 캐시는 Redis를 표준 구현으로 사용한다.
- 기능 코드와 `application` 협력 객체는 가능하면 Redis client 세부사항보다 Spring Cache 경계를 먼저 의존한다.
- SSE subscriber 목록, 요청 중간 계산값처럼 "연결 수명 관리" 자체가 목적이고 캐시 정책이 아닌 구조는 별도 메모리 상태로 둘 수 있다.
- 캐시 이름, TTL, key prefix 같은 운영 정책은 `infrastructure/config` 또는 설정 프로퍼티로 모으고, 기능 클래스 안에 하드코딩하지 않는다.

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

## Related Documents

- [02-package-and-wiring.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/02-package-and-wiring.md)
- [04-domain-modeling-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/04-domain-modeling-rules.md)
- [06-persistence-external-and-exception-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/06-persistence-external-and-exception-rules.md)
- [05-testing-and-lint.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/05-testing-and-lint.md)
