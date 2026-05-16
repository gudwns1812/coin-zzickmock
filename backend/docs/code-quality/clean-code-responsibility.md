# Backend Clean Code And Responsibility

## Purpose

이 문서는 백엔드 구현에서 클래스와 메서드가 너무 많은 책임을 떠안지 않도록 하는 기준이다.
레이어와 패키지 경계는 다른 백엔드 설계 문서가 다루고, 이 문서는 실제 코드를 작성할 때 한 클래스, 한 메서드, 한 변경 단위가 어디까지 책임져야 하는지를 정한다.

먼저 읽어야 하는 문서:

- [README.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/README.md)
- [01-architecture-foundations.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/architecture/foundations.md)
- 유스케이스와 서비스 경계가 걸리면 [03-application-and-providers.md](/Users/hj.park/projects/coin-zzickmock/backend/core/docs/application-and-providers.md)
- 도메인 판단이 필요하면 [04-domain-modeling-rules.md](/Users/hj.park/projects/coin-zzickmock/backend/core/docs/domain-modeling-rules.md)

## Core Rule

코드는 "작동한다"에서 끝나지 않고, 다음 작업자가 안전하게 바꿀 수 있어야 한다.
백엔드 코드는 아래 기준을 기본값으로 삼는다.

- 하나의 클래스는 하나의 변경 이유를 가져야 한다.
- 하나의 메서드는 한 단계의 추상화 수준에서 하나의 개념적 책임을 가져야 한다.
- 유스케이스 메서드는 흐름을 읽히게 하고, 세부 계산과 변환은 이름 있는 협력 객체나 private method로 분리한다.
- 분리는 코드 줄 수가 아니라 책임과 변경 이유를 기준으로 판단한다.
- 분리한 결과가 단순 위임만 늘리면 다시 합친다.

## Method Responsibility

메서드는 호출자가 기대하는 한 가지 결과를 만들어야 한다.
클린 코드와 리팩터링 원칙에서 말하는 "한 가지 일"은 단순히 내부 statement 수가 적다는 뜻이 아니라, 같은 추상화 수준에서 하나의 개념적 책임으로 설명된다는 뜻이다.
메서드를 설명할 때 서로 다른 변경 이유를 가진 행위를 `그리고`, `또는`, `그 다음 모든 것`으로 이어 붙여야 한다면 책임이 섞였는지 의심한다.
단, public 유스케이스 메서드에서 같은 추상화 수준의 단계 이름으로 드러나는 것은 orchestration으로 본다.
문제는 각 단계의 세부 구현이 한 메서드 안에 직접 섞이는 경우다.
아래 책임들이 세부 구현으로 직접 섞이면 분리 후보로 본다.

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
- public 메서드와 그 아래 단계 메서드들은 같은 추상화 수준을 유지한다. 세부 구현이 끼어들면 이름 있는 하위 메서드나 협력 객체로 내린다.
- 조건이 길어지면 조건식 자체보다 그 판단의 의미를 이름으로 추출한다.
- 반복문 안에서 조회, 정책 판단, 상태 변경, 저장, 이벤트 발행이 모두 일어나면 처리 단계를 나눈다.
- `if`/`switch` 분기가 도메인 정책을 표현하면 domain policy 후보로 본다.
- 같은 계산이나 검증이 두 유스케이스 이상에 복제되면 domain 또는 application 협력 객체 후보로 본다.
- private method 추출은 같은 클래스의 가독성을 높일 때만 사용한다. 다른 유스케이스도 써야 하는 책임이면 별도 협력 객체로 분리한다.
- private method가 클래스 필드를 과도하게 공유하거나, 파라미터 없이 숨은 상태에 의존해 많은 일을 하면 협력 객체 분리 후보로 본다.

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

## Method Reading Order

메서드의 위치는 호출 흐름이 위에서 아래로 읽히도록 조정한다.
파일을 처음 읽는 사람이 public 유스케이스 메서드에서 시작해 바로 아래의 하위 단계로 내려가며 의도를 따라갈 수 있어야 한다.

규칙:

- public 유스케이스 메서드를 먼저 두고, 그 메서드가 호출하는 private method를 호출 순서에 가깝게 아래에 배치한다.
- 같은 public 메서드에 속한 private method 묶음은 흩뜨리지 않는다.
- private method 내부에서 다시 하위 private method를 호출한다면, 그 하위 메서드는 호출자 바로 아래에 둔다.
- 여러 public 메서드가 공유하는 private method는 해당 클래스의 공통 개념으로 이름이 충분히 명확할 때만 아래쪽 공통 영역에 모은다.
- 읽기 순서를 위해 기술 세부 구현을 public 흐름 가까이에 끌어올리지 않는다. 추상화 수준이 낮은 구현은 private method나 협력 객체 뒤로 숨긴다.
- 메서드 재배치는 동작 변경 없이 한다. 위치 변경과 로직 변경을 같은 diff에 섞어 리뷰가 어려워지면 분리한다.

권장 배치 예:

```java
public PlaceOrderResult place(PlaceOrderCommand command) {
    Actor actor = currentActor();
    TradingAccount account = loadAccount(actor, command);
    Order order = placeOrder(account, command);

    return toResult(order);
}

private Actor currentActor() {
    return providers.auth().currentActor();
}

private TradingAccount loadAccount(Actor actor, PlaceOrderCommand command) {
    return accountReader.getActiveAccount(actor, command.accountId());
}

private Order placeOrder(TradingAccount account, PlaceOrderCommand command) {
    OrderRequest request = orderRequestFactory.create(command);
    OrderDecision decision = orderPolicy.decide(account, request);

    return account.place(decision);
}

private PlaceOrderResult toResult(Order order) {
    return PlaceOrderResult.from(order);
}
```

위 예처럼 위쪽은 이야기의 목차처럼 읽히고, 아래쪽은 목차의 각 장을 펼쳐 보는 순서가 된다.

## Method Paragraphs And Blank Lines

메서드 안에도 문단이 있다.
빈 줄은 장식이 아니라, 같은 추상화 수준의 단계가 바뀌는 지점을 보여 주는 읽기 장치다.
검증, 조회, 정책 판단, 상태 변경, 저장, 응답 변환이 한 덩어리로 붙어 있으면 코드가 맞아도 다음 사람이 흐름을 놓친다.

규칙:

- 서로 다른 개념적 단계 사이에는 빈 줄을 둔다.
- 같은 하위 책임을 이루는 statement들은 붙여서 한 문단으로 읽히게 한다.
- 짧은 guard clause는 붙여도 되지만, guard 이후 본 흐름이 시작되면 빈 줄로 구분한다.
- 반복문이나 조건문 안에서도 조회, 판단, mutation, side effect가 단계로 바뀌면 빈 줄로 문단을 나눈다.
- 빈 줄이 네 덩어리 이상 계속 필요해지면 메서드 추출이나 협력 객체 분리를 먼저 검토한다.
- 빈 줄로 낮은 추상화 수준의 세부 구현을 숨기려 하지 않는다. 문단을 나눠도 한 메서드가 여러 책임을 품고 있으면 분리한다.

권장 예:

```java
private OrderDecision decideOrder(TradingAccount account, PlaceOrderCommand command) {
    OrderRequest request = orderRequestFactory.create(command);
    MarketPrice price = marketPriceReader.currentPrice(command.symbol());

    if (account.cannotTrade(command.symbol())) {
        throw new CoreException(OrderErrorType.TRADING_NOT_ALLOWED);
    }

    RiskSnapshot risk = riskPolicy.evaluate(account, request, price);

    return orderPolicy.decide(account, request, risk);
}
```

위 예에서 요청/시세 준비, guard, 위험 평가, 최종 판단은 각각 다른 읽기 단위다.
빈 줄은 이 단위가 바뀌는 곳에만 둔다.

피해야 할 예:

```java
private OrderDecision decideOrder(TradingAccount account, PlaceOrderCommand command) {
    OrderRequest request = orderRequestFactory.create(command);
    MarketPrice price = marketPriceReader.currentPrice(command.symbol());
    if (account.cannotTrade(command.symbol())) {
        throw new CoreException(OrderErrorType.TRADING_NOT_ALLOWED);
    }
    RiskSnapshot risk = riskPolicy.evaluate(account, request, price);
    return orderPolicy.decide(account, request, risk);
}
```

위 코드는 모든 statement가 붙어 있어 단계 전환이 눈에 들어오지 않는다.
작은 메서드라도 읽기 단위가 바뀌면 문단을 나눈다.

## Class Responsibility

클래스는 "무엇을 시작하는가"와 "어떤 메커니즘으로 돕는가"를 섞지 않는다.

규칙:

- `application/service` 클래스는 유스케이스 진입점이다.
- `application/service` 클래스가 캐시 저장소, 파서, 배치 커서 관리, 이벤트 fan-out, 재시도 정책까지 직접 소유하면 분리한다.
- service flow readability를 1순위로 본다. public service 메서드는 lock, transaction, 검증, 조회, 판단, 저장, 이벤트/결과 반환 같은 유스케이스 목차가 먼저 읽혀야 한다.
- 여러 유스케이스가 공유하는 처리 메커니즘은 `application/<purpose>` 하위 패키지의 비-Service 협력 객체로 둔다.
- service 흐름을 흐리는 application 실행 절차/계산/조합은 단일 `application/implement` 패키지의 concrete collaborator로 둘 수 있다.
- 오래 살아야 하는 비즈니스 규칙은 `domain`으로 올릴 수 있는지 먼저 판단한다.
- 프레임워크나 외부 시스템 세부사항은 `infrastructure`로 내린다.
- `application/implement` class 이름은 `Order`, `Position`, `Account`처럼 owning domain/use-case prefix로 시작하고, prefix 뒤에는 `OrderFillApplier`처럼 간결한 role을 둔다.
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
- 테스트 편의를 위한 생성자나 fixture 조립 로직을 운영 controller에 넣지 않는다.
- SSE broker는 stream별 fan-out과 telemetry만 맡고, 제한이 있는 subscriber lifecycle은 `common/web/SseSubscriptionRegistry` 같은 공통 메커니즘으로 분리한다.

### `job`

- scheduler/startup/backfill/retry trigger는 application service/coordinator 호출만 맡는다.
- repository/entity/JPA/Redis/SMTP/external SDK를 직접 다루기 시작하면 application 또는 infrastructure로 책임을 옮긴다.
- trigger class 안에서 비즈니스 규칙이나 트랜잭션 흐름을 새로 만들지 않는다.

### `application`

- application service는 유스케이스 흐름과 트랜잭션 경계를 책임진다.
- domain 규칙을 대신 구현하지 않는다.
- 외부 SDK, SecurityContext, Redis client, JPA entity 세부사항을 직접 다루지 않는다.
- 공유 로직은 `application/service`끼리 호출하지 말고 목적형 협력 객체로 분리한다.
- `application/implement`는 새 layer가 아니며 feature별 `application` 안에 하나만 둔다. `common`, `util`, `helper` 같은 하위 package를 만들지 않는다.
- 한 번만 쓰이고 service 흐름을 흐리지 않는 세부 구현은 premature commonization을 피하고 private method, result factory, 또는 domain factory에 남긴다.
- 여러 유스케이스에서 반복될 수 있는 절차/계산/조합이면 이름 있는 implement collaborator로 승격한다. 한 번만 쓰이더라도 repository/provider/domain orchestration이 섞여 service 목차를 흐리면 implement로 분리할 수 있다.
- application DTO, query, result, projection 타입으로 입출력 의미를 드러낸다. `*Command`나 `*Result` class name을 쓰더라도 새 package convention은 `application/dto`다.
- application/repository, gateway, provider 같은 운영 인터페이스에 `default` 메서드를 두지 않는다.
- 여러 진입점이 같은 계정/포지션 mutation 정책을 적용하면 각 service에 복제하지 않고 `OrderFillApplier`처럼 transaction 내부에서 호출되는 `application/implement` 협력 객체로 모은다.
- 조회 service가 DB range 계산, rollup, cache/provider 보충, telemetry tagging을 모두 품기 시작하면 `Reader`, `Projector`, `Appender`, `Telemetry` 같은 목적형 객체로 나눈다.

### `domain`

- 도메인은 규칙과 상태 전이를 가장 짧은 언어로 표현한다.
- setter 중심 객체보다 의도가 드러나는 메서드를 둔다.
- 불변식 검증은 생성과 상태 전이 가까이에 둔다.
- 날짜, 금액, 수량, 레버리지처럼 의미 있는 값은 원시 타입으로 흩뿌리지 말고 값 객체 후보로 본다.
- Spring, JPA, HTTP, SDK 타입은 도메인에 들어오지 않는다.
- 큰 record가 호환 accessor를 유지하더라도 새 규칙은 `identity`, `exposure`, `accounting` 같은 값 객체 view를 통해 추가한다.

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
- 서로 다른 개념적 단계가 빈 줄 없이 한 덩어리로 붙어 흐름이 잘 보이지 않는다.
- `and`가 붙은 메서드명이나 클래스명이 필요해진다.
- 메서드를 한 문장으로 설명할 수 없거나, 설명에 서로 다른 개념적 책임이 섞인다.
- 한 private method가 다시 여러 private method를 끌고 다닌다.
- private method가 파라미터보다 클래스 필드와 숨은 상태에 더 많이 기대어 동작한다.
- 동일한 조건식, 계산식, 매핑 코드가 반복된다.
- 테스트 setup이 동작 하나에 비해 과하게 커진다.
- fake 구현을 줄이려고 운영 인터페이스에 `default` 메서드 로직을 추가하고 싶어진다.
- 로그, 예외 번역, 저장, 도메인 판단이 순서 없이 섞인다.
- 새 의존성을 넣기 위해 기존 클래스 생성자 파라미터가 계속 늘어난다.

분리 순서:

1. 먼저 domain 규칙인지 판단한다.
2. domain이 아니면 application 협력 객체인지 판단한다.
3. 기술 세부사항이면 infrastructure adapter나 config로 내린다.
4. 단순히 읽기 좋은 단계 이름만 필요한 경우 private method로 추출한다.
5. 추출 후 이름이 책임을 설명하지 못하면 추출 방향을 다시 검토한다.

### Application Implement Refactor Checklist

큰 application service를 정리할 때는 아래 순서로 판단한다.

1. public service flow를 먼저 쓴다. 읽는 사람이 유스케이스 목차를 따라갈 수 없다면 세부 구현을 내린다.
2. storage-free이고 오래 살아야 하는 제품 규칙은 `domain` 후보로 먼저 본다.
3. repository/provider/domain 조합, post-save 처리, projection, invariant validation처럼 application 실행 세부사항이면 `application/implement` 후보로 본다.
4. 단순 constructor/field mapping이거나 한 번만 쓰이고 흐름을 흐리지 않는 코드는 premature commonization을 피하고 private method 또는 factory에 남긴다.
5. 여러 유스케이스에서 반복될 수 있는 절차/계산/조합은 `application/implement`로 승격한다.
6. `application/implement`에는 하위 package를 만들지 않고, 모든 class를 바로 아래에 둔다.
7. class 이름은 `Order`, `Position`, `Account` 같은 owning domain/use-case prefix로 시작하되, prefix 뒤에는 `FillApplier`, `PlacementFactory`, `CrossMarginPreviewProjector`처럼 짧고 역할이 드러나는 이름을 쓴다.
8. `Manager`, `Helper`, `Util`, `CommonService`, generic `Processor`밖에 떠오르지 않으면 책임을 다시 쪼개거나 위치를 재검토한다.

## Verification Checklist

백엔드 변경 검증에는 기능 테스트뿐 아니라 책임 분리 확인도 포함한다.

- 변경한 public 유스케이스 메서드를 읽었을 때 흐름이 위에서 아래로 설명되는가?
- 메서드 내부의 검증, 조회, 판단, mutation, 저장, 변환 단계가 빈 줄로 자연스럽게 구분되는가?
- private method 위치가 호출 흐름을 따라 top-down으로 읽히는가?
- 새 클래스마다 한 문장으로 책임을 설명할 수 있는가?
- 테스트가 도메인 규칙, application orchestration, infrastructure adapter를 구분해서 검증하는가?
- mock/fake가 너무 많아서 클래스 책임이 과한 신호를 숨기고 있지 않은가?
- 운영 인터페이스 `default` 메서드가 남아 있지 않고, 테스트 편의 구현은 `src/test/java`의 테스트 전용 class에만 있는가?
- `application/service` 간 직접 의존이 없는가?
- `application/implement`가 새 layer처럼 쓰이지 않고, 하위 `common`/`util`/`helper` package 없이 concrete execution-detail collaborator만 담는가?
- `application/implement` class 이름이 owning domain/use-case prefix로 시작하고 concise role naming을 따르는가?
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

- [01-architecture-foundations.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/architecture/foundations.md)
- [02-package-and-wiring.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/architecture/package-and-wiring.md)
- [03-application-and-providers.md](/Users/hj.park/projects/coin-zzickmock/backend/core/docs/application-and-providers.md)
- [04-domain-modeling-rules.md](/Users/hj.park/projects/coin-zzickmock/backend/core/docs/domain-modeling-rules.md)
- [05-testing-and-lint.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/architecture/testing-and-architecture-lint.md)
