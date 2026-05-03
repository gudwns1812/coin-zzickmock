# Backend Clean Code And Responsibility

## Purpose

이 문서는 백엔드 구현에서 클래스와 메서드가 너무 많은 책임을 떠안지 않도록 하는 기준이다.
레이어와 패키지 경계는 다른 백엔드 설계 문서가 다루고, 이 문서는 실제 코드를 작성할 때 한 클래스, 한 메서드, 한 변경 단위가 어디까지 책임져야 하는지를 정한다.

먼저 읽어야 하는 문서:

- [README.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/README.md)
- [01-architecture-foundations.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/01-architecture-foundations.md)
- 유스케이스와 서비스 경계가 걸리면 [03-application-and-providers.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/03-application-and-providers.md)
- 도메인 판단이 필요하면 [04-domain-modeling-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/04-domain-modeling-rules.md)

## Core Rule

코드는 "작동한다"에서 끝나지 않고, 다음 작업자가 안전하게 바꿀 수 있어야 한다.
백엔드 코드는 아래 기준을 기본값으로 삼는다.

- 하나의 클래스는 하나의 변경 이유를 가져야 한다.
- 하나의 메서드는 한 단계의 추상화 수준에서 한 가지 일을 해야 한다.
- 유스케이스 메서드는 흐름을 읽히게 하고, 세부 계산과 변환은 이름 있는 협력 객체나 private method로 분리한다.
- 분리는 코드 줄 수가 아니라 책임과 변경 이유를 기준으로 판단한다.
- 분리한 결과가 단순 위임만 늘리면 다시 합친다.

## Method Responsibility

메서드는 호출자가 기대하는 한 가지 결과를 만들어야 한다.
아래 일이 한 메서드 안에 세 가지 이상 섞이면 분리 후보로 본다.

- 입력 검증
- 권한/actor 확인
- 조회
- 정책 판단
- 상태 변경
- 외부 호출
- 영속화
- 이벤트 발행
- 응답 변환
- 로그/텔레메트리 기록

강한 규칙:

- public 유스케이스 메서드는 큰 흐름을 드러내는 orchestration에 집중한다.
- 조건이 길어지면 조건식 자체보다 그 판단의 의미를 이름으로 추출한다.
- 반복문 안에서 조회, 정책 판단, 상태 변경, 저장, 이벤트 발행이 모두 일어나면 처리 단계를 나눈다.
- `if`/`switch` 분기가 도메인 정책을 표현하면 domain policy 후보로 본다.
- 같은 계산이나 검증이 두 유스케이스 이상에 복제되면 domain 또는 application 협력 객체 후보로 본다.
- private method 추출은 같은 클래스의 가독성을 높일 때만 사용한다. 다른 유스케이스도 써야 하는 책임이면 별도 협력 객체로 분리한다.

권장 흐름 예:

```java
@Transactional
public PlaceOrderResult place(PlaceOrderCommand command) {
    Actor actor = providers.auth().currentActor();
    TradingAccount account = loadAccount(actor, command);
    OrderRequest request = orderRequestFactory.create(command);
    OrderDecision decision = orderPolicy.decide(account, request);

    Order order = account.place(decision);
    orderRepository.save(order);
    telemetry.recordOrderPlaced(actor, order);

    return PlaceOrderResult.from(order);
}
```

위 예에서 public 메서드는 흐름을 보여 준다.
계정 조회, 요청 생성, 정책 판단, 결과 변환은 각각 이름 있는 책임으로 드러난다.

## Class Responsibility

클래스는 "무엇을 시작하는가"와 "어떤 메커니즘으로 돕는가"를 섞지 않는다.

규칙:

- `application/service` 클래스는 유스케이스 진입점이다.
- `application/service` 클래스가 캐시 저장소, 파서, 배치 커서 관리, 이벤트 fan-out, 재시도 정책까지 직접 소유하면 분리한다.
- 여러 유스케이스가 공유하는 처리 메커니즘은 `application/<purpose>` 하위 패키지의 비-Service 협력 객체로 둔다.
- 오래 살아야 하는 비즈니스 규칙은 `domain`으로 올릴 수 있는지 먼저 판단한다.
- 프레임워크나 외부 시스템 세부사항은 `infrastructure`로 내린다.
- `Manager`, `Helper`, `Util`, `CommonService`처럼 책임을 숨기는 이름은 새로 만들지 않는다.

분리 기준:

- 변경 요청을 받았을 때 이 클래스의 서로 다른 부분이 서로 다른 이유로 자주 바뀐다.
- 테스트에서 한 동작을 검증하려는데 관련 없는 의존성을 많이 준비해야 한다.
- 메서드 이름이 `process`, `handle`, `sync`, `updateAll`처럼 넓고 내부 단계가 이름으로 드러나지 않는다.
- 클래스 필드가 특정 메서드 묶음에서만 사용되는 그룹으로 나뉜다.
- 실패 처리, 로깅, 영속화, 도메인 판단이 한 클래스에 모두 섞여 있다.

## Layer-Specific Clean Code Rules

### `web`

- controller는 request validation, 인증된 요청 컨텍스트 파싱, application 호출, response mapping만 맡는다.
- controller private method가 비즈니스 규칙을 담기 시작하면 application 또는 domain으로 옮긴다.
- 엔티티나 persistence DTO를 HTTP 응답으로 직접 노출하지 않는다.

### `job`

- scheduler/startup/backfill/retry trigger는 application service/coordinator 호출만 맡는다.
- repository/entity/JPA/Redis/SMTP/external SDK를 직접 다루기 시작하면 application 또는 infrastructure로 책임을 옮긴다.
- trigger class 안에서 비즈니스 규칙이나 트랜잭션 흐름을 새로 만들지 않는다.

### `application`

- application service는 유스케이스 흐름과 트랜잭션 경계를 책임진다.
- domain 규칙을 대신 구현하지 않는다.
- 외부 SDK, SecurityContext, Redis client, JPA entity 세부사항을 직접 다루지 않는다.
- 공유 로직은 `application/service`끼리 호출하지 말고 목적형 협력 객체로 분리한다.
- command/query/result 타입으로 입출력 의미를 드러낸다.

### `domain`

- 도메인은 규칙과 상태 전이를 가장 짧은 언어로 표현한다.
- setter 중심 객체보다 의도가 드러나는 메서드를 둔다.
- 불변식 검증은 생성과 상태 전이 가까이에 둔다.
- 날짜, 금액, 수량, 레버리지처럼 의미 있는 값은 원시 타입으로 흩뿌리지 말고 값 객체 후보로 본다.
- Spring, JPA, HTTP, SDK 타입은 도메인에 들어오지 않는다.

### `infrastructure`

- infrastructure는 기술 세부사항을 숨기는 adapter다.
- 외부 응답, JPA entity, query 결과를 application/domain 언어로 번역하는 책임을 가진다.
- 도메인 정책의 원본을 infrastructure에 두지 않는다.
- retry, timeout, connection 설정 같은 운영 정책은 config나 provider 구현 쪽에 모은다.
- persistence adapter는 create/update/bulk/read 계약의 의미를 흐리지 않는다. 조회 중 생성, generic upsert, 단건 수정 후 `saveAndFlush` 남발은 책임이 섞인 신호로 본다.
- managed JPA entity 단건 수정은 dirty checking을 기본으로 삼고, 다건 상태 변경은 application에서 결정한 의도를 bulk/batch repository method로 명시한다.

## Naming Rules

이름은 책임 분리의 첫 번째 테스트다.

권장:

- 클래스 이름은 책임의 결과나 역할을 드러낸다.
- 메서드 이름은 내부 구현보다 호출자가 얻는 의미를 드러낸다.
- boolean은 `is`, `has`, `can`, `should` 같은 술어형으로 쓴다.
- 넓은 동사는 더 구체적인 동사로 바꾼다.

피해야 할 이름:

- `process`
- `handle`
- `doWork`
- `executeAll`
- `syncEverything`
- `Manager`
- `Helper`
- `Util`
- `CommonService`

단, framework contract나 외부 API 명세 때문에 넓은 이름이 필요한 경우에는 주변 타입과 패키지로 책임을 좁힌다.

## Planning Checklist

백엔드 계획을 세울 때 아래 질문에 답한다.

- 이번 변경의 유스케이스 진입점은 어디인가?
- 새 책임이 기존 클래스의 같은 변경 이유에 속하는가, 아니면 별도 협력 객체인가?
- 이 규칙은 domain에 있어야 하는가, application orchestration에 있어야 하는가?
- 외부 시스템이나 프레임워크 세부사항이 application/domain으로 새어 들어오지 않는가?
- 새 인터페이스가 실제 경계나 다중 구현을 표현하는가, 아니면 테스트나 습관 때문에 만든 것인가?
- 기존 테스트가 책임 분리를 보호하는가, 아니면 회귀 테스트를 먼저 추가해야 하는가?

## Implementation Checklist

구현 중 아래 신호가 보이면 멈추고 구조를 다시 나눈다.

- 한 메서드가 화면에 한 번에 읽히지 않을 정도로 길어진다.
- `and`가 붙은 메서드명이나 클래스명이 필요해진다.
- 한 private method가 다시 여러 private method를 끌고 다닌다.
- 동일한 조건식, 계산식, 매핑 코드가 반복된다.
- 테스트 setup이 동작 하나에 비해 과하게 커진다.
- 로그, 예외 번역, 저장, 도메인 판단이 순서 없이 섞인다.
- 새 의존성을 넣기 위해 기존 클래스 생성자 파라미터가 계속 늘어난다.

분리 순서:

1. 먼저 domain 규칙인지 판단한다.
2. domain이 아니면 application 협력 객체인지 판단한다.
3. 기술 세부사항이면 infrastructure adapter나 config로 내린다.
4. 단순히 읽기 좋은 단계 이름만 필요한 경우 private method로 추출한다.
5. 추출 후 이름이 책임을 설명하지 못하면 추출 방향을 다시 검토한다.

## Verification Checklist

백엔드 변경 검증에는 기능 테스트뿐 아니라 책임 분리 확인도 포함한다.

- 변경한 public 유스케이스 메서드를 읽었을 때 흐름이 위에서 아래로 설명되는가?
- 새 클래스마다 한 문장으로 책임을 설명할 수 있는가?
- 테스트가 도메인 규칙, application orchestration, infrastructure adapter를 구분해서 검증하는가?
- mock/fake가 너무 많아서 클래스 책임이 과한 신호를 숨기고 있지 않은가?
- `application/service` 간 직접 의존이 없는가?
- `./gradlew architectureLint`가 통과하는가?
- `./gradlew check`가 통과하는가?

## Review Prompts For Agents

백엔드 계획, 구현, 리뷰를 수행하는 에이전트는 아래 질문을 명시적으로 확인한다.

- 이 변경이 기존 클래스의 책임을 넓히는가, 아니면 새 책임을 올바른 위치에 둔 것인가?
- 가장 긴 메서드는 무엇이며, 그 메서드는 한 가지 추상화 수준만 다루는가?
- domain/application/infrastructure 중 어느 레이어가 실제 규칙의 원본을 소유하는가?
- 테스트가 책임 분리를 강화하는가, 아니면 현재 구조에만 맞춘 brittle test인가?
- 더 적은 코드와 더 명확한 이름으로 같은 의도를 표현할 수 있는가?

## Related Documents

- [01-architecture-foundations.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/01-architecture-foundations.md)
- [02-package-and-wiring.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/02-package-and-wiring.md)
- [03-application-and-providers.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/03-application-and-providers.md)
- [04-domain-modeling-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/04-domain-modeling-rules.md)
- [05-testing-and-lint.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/05-testing-and-lint.md)
