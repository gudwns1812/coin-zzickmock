# Backend Application And Providers

## Purpose

이 문서는 유스케이스 경계, 레이어 간 의존 방향, `Providers`, 캐시와 공유 메커니즘의 배치를 설명한다.
`application/service`가 어디까지를 맡고, 어디서부터 목적형 협력 객체나 Provider 경계로 분리해야 하는지 여기서 확인한다.

먼저 읽어야 하는 문서:

- [README.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/README.md)
- [01-architecture-foundations.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/architecture/foundations.md)

## Dependency Rule

Gradle module 의존 규칙은 Java layer 의존 규칙보다 먼저 적용한다. `core`는 backend project dependency를 갖지 않는다.
`stream`, `storage`, `external`은 backend project module 중 `core`에만 의존한다. `app`은 실행 조립을 위해 `core`와 leaf adapter modules에 의존할 수 있지만, leaf concrete import는 configuration/assembly/config package로 제한한다.

의존 방향은 아래만 허용한다.

- `web` -> `application`
- `app web/job` -> `core application` only for use-case calls
- `job` -> `application`
- `application` -> `domain`
- `application` -> `providers`
- `application` -> 필요할 때만 `application` 또는 `domain`이 소유한 계약
- `infrastructure` -> `application`, `domain`, `providers`
- `domain` -> 자신과 `common`만

절대 금지:

- `domain` -> `application`
- `domain` -> `infrastructure`
- `application` -> 구체 SDK 클라이언트
- `web` -> `infrastructure`
- `job` -> `infrastructure`
- `job` -> HTTP/SSE type
- `application/service` -> 다른 `application/service`
- `providers` -> feature `infrastructure`

HTTP delivery Java package는 `web`이다.
예전 `api` package 이름은 더 이상 사용하지 않는다.

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
provider auth 모델은 feature domain type을 직접 담지 않는다.
예를 들어 `providers.auth.Actor`는 member domain의 `MemberRole`이 아니라 provider-owned `ActorRole`을 사용한다.
member 인증과 프로필 조회는 active member만 다룬다.
탈퇴 회원까지 포함해야 하는 account 중복/감사 성격 조회는 `IncludingWithdrawn` 또는 `ForAudit`처럼 method name에 의도를 드러낸다.
현재 auth/member/profile 조회 경로에는 별도 Spring/Redis cache surface가 없으므로 탈퇴 stale 노출은 active DB lookup filtering으로 차단한다.
leaderboard는 withdrawal 이벤트로 snapshot entry를 제거하고, leaderboard-owned dedicated projection query에서 `member_credentials.withdrawn_at IS NULL` active member만 반환한다.

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
주문/포지션/계정처럼 큰 쓰기 흐름에서 service의 목차를 흐리는 실행 세부사항은 단일 `application/implement` 하위 패키지에 둘 수 있다.

강한 규칙:

- `application/service`는 다른 `application/service`를 직접 참조하지 않는다.
- 공유 로직이 필요하면 먼저 [04-domain-modeling-rules.md](/Users/hj.park/projects/coin-zzickmock/backend/core/docs/domain-modeling-rules.md) 기준으로 `domain` 후보인지 본다.
- `domain`으로 올릴 수 없고 애플리케이션 메커니즘에 가까우면 `application/<purpose>` 또는 `application/implement` 하위 패키지로 분리한다.
- `application/implement` is not a sixth layer. It contains concrete application execution-detail collaborators, and every class name must start with the owning domain/use-case domain prefix such as `Order`, `Position`, or `Account`.
- `application/implement`는 feature별 `application` 안에 하나만 둔다. `application/implement/common`, `application/implement/util`, `application/implement/helper` 같은 하위 잡동사니 package를 만들지 않는다.
- `application/service`는 "무슨 일을 시작하는가"를, 목적형 협력 객체는 "그 일을 어떤 메커니즘으로 지원하는가"를 드러내야 한다.

### Application Subpackage Placement

| 위치 | 책임 | 이름 규칙 | 두지 말 것 |
| --- | --- | --- | --- |
| `application/service` | public use-case entrypoint, transaction boundary, top-level orchestration | `CreateOrderService`, `ClosePositionService`처럼 사용자가 시작하는 흐름 | 다른 service 직접 호출, repository/provider/domain 세부 조합을 모두 품는 giant method |
| `application/implement` | service 흐름을 흐리는 application 실행 절차/계산/조합에 이름을 붙인 concrete collaborator | `OrderFillApplier`, `OrderPlacementFactory`, `PositionCloseProjector`, `AccountBalanceReconciler`처럼 소유 domain/use-case prefix + concise role | 새 layer, generic bucket, interface-first port, `Manager`/`Helper`/`Util`/`CommonService`, prefix 없는 class |
| `application/<purpose>` | 이미 존재하거나 feature 전반에서 공유되는 명확한 mechanism package | `realtime`, `grant`, `placement`처럼 package 자체가 목적을 설명하고 class도 concrete role을 드러냄 | 새 convention이 필요한 큰 service refactor에서 목적 없는 ad-hoc package 증식 |
| `domain` | storage-free이고 오래 살아야 하는 제품 규칙, 불변식, 상태 전이 | 도메인 언어의 model/policy/service/value object | repository/provider 조회, transaction 실행, 외부 시스템/프레임워크 세부사항 |

`application/implement` 추출의 1순위 판단 기준은 service flow readability다.
여러 유스케이스에서 반복될 수 있는 절차/계산/조합이면 내부 private method보다 이름 있는 implement collaborator를 우선한다.
반대로 단순 field 전달이나 한 번 쓰이고 service 흐름을 흐리지 않는 세부 구현은 premature commonization을 피하고 private method 또는 result/domain factory에 남긴다.
한 번만 쓰이더라도 repository/provider/domain orchestration이 섞여 public service의 목차를 흐리면 `application/implement`로 분리할 수 있다.

현재 코드에서 지켜야 하는 대표 패턴:

- `GetMarketCandlesService`는 query orchestration에 집중한다. persisted range read는 `MarketPersistedCandleReader`, interval rollup은 `MarketCandleRollupProjector`, historical cache/provider 보충은 `MarketHistoricalCandleAppender`, lookup tagging은 `MarketHistoryLookupTelemetry`가 맡는다.
- 즉시 체결 주문과 pending open-order fill은 계정 예약, 포지션 생성/증가, mutation result 해석을 각각 구현하지 않는다. 두 진입점은 filled order request를 만들고, 상태 적용은 concise role naming을 따르는 `OrderFillApplier` 같은 `application/implement` collaborator가 맡는다.
- application 협력 객체는 `*Service` 이름을 남발하지 않는다. 예를 들어 `Applier`, `Projector`, `Appender`, `Reader`, `Telemetry`처럼 책임의 결과나 메커니즘을 드러낸다.
- 협력 객체가 telemetry를 기록하더라도 business flow의 원본 판단을 숨기면 안 된다. public service에서는 어떤 단계가 실행되는지 읽혀야 한다.

## Job Boundary

`job`은 유스케이스를 직접 구현하는 레이어가 아니라 application을 깨우는 얇은 trigger다.

허용:

- scheduled tick에서 application service/coordinator 호출
- startup warmup/backfill trigger에서 application service/coordinator 호출
- retry/background trigger에서 application service/coordinator 호출

금지:

- repository/entity/JPA/Redis/SMTP/external SDK 직접 호출
- HTTP/SSE type 의존
- transaction orchestration이나 business policy 직접 구현
- application service 사이의 우회 호출을 만들기 위한 second application layer 역할

## Cache Boundary

캐시는 "값을 잠깐 들고 있다가 다시 쓰는 메커니즘"이다.
유스케이스 코드 안에 `ConcurrentHashMap` 같은 ad-hoc 저장소를 직접 박아 넣기보다 Spring이 관리하는 경계 뒤에 두는 것이 기본값이다.

강한 규칙:

- 단일 인스턴스 안에서만 필요한 로컬 캐시는 Spring Cache를 기본값으로 사용한다.
- 여러 인스턴스가 값을 공유해야 하는 분산 캐시는 Redis를 표준 구현으로 사용한다.
- 기능 코드와 `application` 협력 객체는 가능하면 Redis client 세부사항보다 Spring Cache 경계를 먼저 의존한다.
- SSE subscriber 목록, 요청 중간 계산값처럼 "연결 수명 관리" 자체가 목적이고 캐시 정책이 아닌 구조는 별도 메모리 상태로 둘 수 있다.
- 제한이 있는 SSE subscriber lifecycle은 broker별 ad-hoc map/semaphore 조합보다 `common/web/SseSubscriptionRegistry`를 우선 사용한다.
- 캐시 이름, TTL, key prefix 같은 운영 정책은 `infrastructure/config` 또는 설정 프로퍼티로 모으고, 기능 클래스 안에 하드코딩하지 않는다.

## Use Case Shape

애플리케이션은 유스케이스 중심으로 작성한다.

```text
feature/order/application/
  dto/
    PlaceOrderCommand.java
    PlaceOrderResult.java
    PendingOrderCandidate.java
  service/
    PlaceOrderService.java
  implement/
    OrderFillApplier.java
```

강한 규칙:

- 하나의 application service는 하나의 유스케이스 또는 강하게 응집된 흐름만 맡는다.
- `FooService` 하나에 여러 unrelated 메서드를 몰아넣지 않는다.
- 쓰기 입력, 읽기 결과, event payload, repository projection은 `application/dto`에 둔다. `*Command`와 `*Result` class name은 의미가 분명하면 유지할 수 있지만 package convention은 `dto`다.
- `application/command`와 `application/result`는 이전 코드의 migration residue이며 새 코드의 기본값이 아니다.
- 트랜잭션은 유스케이스 단위로 잡는다.
- 기본 주입 대상은 concrete `*Service`다.
- `*UseCase` 인터페이스는 public contract가 실제로 필요한 경우에만 만든다.
- `*Port`는 외부 경계를 표현하는 계약이 정말 필요할 때만 만든다.
- 구현이 하나뿐이고 호출 전달만 하는 인터페이스는 만들지 않는다.

## Application DTO Result Factory Rule

`application/service`는 유스케이스를 조율하고, result 필드 조립과 변환 책임은 각 `application/dto/*Result` 또는 목적이 분명한 DTO가 소유한다.

강한 규칙:

- service가 domain model, snapshot, projection entry를 result constructor로 직접 펼치지 않는다.
- 변환이 필요하면 result record 내부에 `from(...)`, `of(...)`, `pending(...)` 같은 명명된 static factory method를 둔다.
- service는 repository/provider 호출, 유효성 검증, transaction orchestration, token 발급처럼 유스케이스 진행에 필요한 입력값만 준비한다.
- token 발급, 외부 조회, repository 호출처럼 side effect가 있는 작업은 result factory 안으로 숨기지 않는다. service가 값을 준비하고 result factory에는 준비된 값이나 순수 callback만 넘긴다.
- 여러 result가 공유하는 복잡한 projection 전용 로직은 기존 `Assembler`, `Projector`, `Reader` 같은 목적형 협력 객체로 남길 수 있다. 이 경우에도 service가 직접 constructor mapping을 반복하지 않는다.

### Technical Package Split Recipe

`application/realtime`처럼 timing, transport, 또는 technical context를 package 이름으로 묶은 코드는 migration 때 책임별로 다시 분류한다.

1. public use case 또는 application event entrypoint를 식별해 `application/service`로 둔다.
2. application input/output, event payload, repository projection contract를 식별해 `application/dto`로 둔다.
3. queue, worker, cache, book, hydrator, processor 같은 execution-detail collaborator를 식별해 `application/implement`로 둔다.
4. storage-free이고 오래 살아야 하는 제품 규칙이나 상태 전이는 `domain` 후보로 검토한다.
5. 이동 후 targeted tests로 behavior를 보호하고, old package residue search를 실행한다.

예:

```java
// 권장
return OpenOrderResult.from(order);

// 비권장
return new OpenOrderResult(order.orderId(), order.symbol(), ...);
```

## Related Documents

- [02-package-and-wiring.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/architecture/package-and-wiring.md)
- [04-domain-modeling-rules.md](/Users/hj.park/projects/coin-zzickmock/backend/core/docs/domain-modeling-rules.md)
- [06-persistence-rules.md](/Users/hj.park/projects/coin-zzickmock/backend/storage/docs/persistence-rules.md)
- [08-external-integration-rules.md](/Users/hj.park/projects/coin-zzickmock/backend/external/docs/external-integration-rules.md)
- [09-exception-rules.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/errors/exception-rules.md)
- [05-testing-and-lint.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/architecture/testing-and-architecture-lint.md)
