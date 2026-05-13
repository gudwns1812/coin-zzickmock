# Backend Domain Modeling Rules

## Purpose

이 문서는 `coin-zzickmock` 백엔드의 도메인 관련 규칙 원문을 한 곳에 모은다.
도메인 모델, 정책, 상태 전이, 값 검증, 도메인과 application/infrastructure의 경계 판단은 이 문서를 기준으로 해석한다.

강한 규칙:

- 도메인 관련 작업을 할 때는 다른 backend 상세 설계 문서보다 먼저 이 문서를 읽는다.
- 다른 문서에 있는 domain 언급은 구조 설명이나 맥락 설명을 위한 요약으로만 본다.
- 도메인 규칙의 최종 원문 소유권은 이 문서가 가진다.

먼저 읽어야 하는 문서:

- [README.md](/Users/hj.park/projects/coin-zzickmock/backend/core/docs/README.md)
- [architecture/foundations.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/architecture/foundations.md)

## When To Read

아래 작업은 모두 이 문서를 먼저 읽는다.

- 도메인 모델을 새로 만들거나 이름을 바꿀 때
- 상태 전이 메서드나 정책 객체를 추가할 때
- 값 검증을 생성 시점에 둘지 service에 둘지 판단할 때
- 어떤 로직을 `application`에 둘지 `domain`으로 올릴지 판단할 때
- 도메인 객체를 Spring bean으로 조립해야 할 때

## Domain Ownership Rule

도메인은 "가장 오래 살아야 하는 비즈니스 의미"를 소유한다.
저장 방식, 프레임워크, 외부 SDK보다 오래 남아야 하는 규칙은 domain 후보로 본다.

도메인에 두는 대상:

- aggregate
- entity
- value object
- domain service
- domain policy
- domain event

도메인에 먼저 올릴지 검토해야 하는 로직:

- 계정, 주문, 포지션, 보상처럼 제품 개념의 의미를 바꾸는 계산
- 상태 전이와 불변식 유지
- 여러 유스케이스에서 반복되는 비즈니스 규칙
- 제품 명세에 직접 등장하는 계산식이나 판단 기준

계산식 문서화 규칙:

- 손익, ROE/ROI, 마진, 청산가, 펀딩, 포인트, 랭킹, 차트 파생값처럼 제품 의미를 가진 공식이나 판단 기준을 새로 만들거나 바꾸면 구현과 같은 변경에서 `docs/product-specs` 또는 이 설계 문서 묶음의 적절한 원문 문서를 갱신한다.
- 공식은 코드나 테스트만 보고 재추론하지 않도록 문서에 이름, 입력값, 기준 가격, 반올림/절단 기준, 예외 조건을 남긴다.
- 프론트가 표시용으로 서버 공식을 일부 복제할 때도 원문 공식은 문서에 두고, 프론트 구현은 그 문서를 따르는 파생 구현으로 취급한다.

### Futures TP/SL Order Rule

- TP/SL처럼 사용자가 주문으로 인식하고 취소/이력/체결을 기대하는 실행 의도는 포지션 필드가 아니라 `FuturesOrder`가 source of truth다.
- TP/SL 조건부 주문은 `orderPurpose == CLOSE_POSITION`에서 reduce-only 의미를 파생한다. 별도 `reduceOnly` 필드를 추가하지 않는다.
- TP/SL trigger 판단은 `triggerSource = MARK_PRICE`, execution은 latest/last price를 사용한다.
- Active pending TP/SL uniqueness는 `futures_orders.active_conditional_trigger_type` unique key로 보장한다. 이 컬럼은 pending conditional close order에만 trigger type을 보관하고, filled/cancelled/manual/open order에서는 `NULL`이어야 한다.
- V9 legacy position TP/SL 컬럼은 V11 migration에서 조건부 close order로 backfill한 뒤 호환 컬럼으로만 남긴다.
- Manual close reservation 공식은 `sum(pending non-conditional CLOSE_POSITION quantity)`이며, 포지션 summary의 `pendingCloseQuantity`와 `closeableQuantity = max(0, quantity - pendingCloseQuantity)`도 이 manual-only 공식을 따른다. TP/SL 조건부 주문은 protective lifecycle로 유지되며 manual close reservation/cap 공식에 포함하지 않는다.
- TP/SL trigger fill은 fill 직전 현재 열린 포지션 수량으로 조건부 주문 수량을 동기화한다. 포지션이 없거나 남은 수량이 0이면 stale TP/SL 조건부 주문은 fill하지 않고 취소한다.
- 포지션이 full close, pending close fill, liquidation, TP/SL trigger로 0이 되면 application close flow는 manual-only cap으로 stale manual close 주문을 취소하고, 별도 protective cleanup으로 stale TP/SL 조건부 주문을 취소한다. 부분 종료는 manual close cap만 조정하며 TP/SL을 cap 때문에 취소하지 않는다.
- `open_positions.take_profit_price`, `open_positions.stop_loss_price`는 legacy 호환 컬럼일 뿐 domain/application의 TP/SL read/write/trigger source가 아니다.

도메인에 두지 않는 대상:

- HTTP 요청/응답 처리
- 트랜잭션 경계와 유스케이스 오케스트레이션
- JPA 엔티티, QueryDSL, SQL, 외부 API DTO
- 인증 컨텍스트 조회, feature flag 평가, telemetry 호출

## Modeling Rule

- 도메인은 비즈니스 언어로만 이름 짓는다.
- 도메인은 저장 방식이나 API 응답 형식을 모른다.
- 값 검증은 가능한 도메인 생성 시점에 닫는다.
- 상태 변경은 의미 있는 메서드로 노출한다.
- 하나의 도메인 타입은 자기 불변식과 자기 상태 전이에 책임을 진다.
- JPA entity에도 의미 있는 변경 메서드를 둘 수 있지만, 그것은 persistence state 반영을 위한 메서드다. 수익/마진/주문 상태 같은 비즈니스 전이의 원문은 domain 메서드 또는 domain policy에 둔다.

권장:

- `activate()`
- `changeLeverage(...)`
- `openPosition(...)`
- `closePosition(...)`
- `grantPoint(...)`

비권장:

- `update(...)`
- `process(...)`
- `handle(...)`

### Position Snapshot Modeling

`PositionSnapshot`은 아직 persistence/API 호환을 위해 record accessor를 유지하지만, 새 구현은 원시 필드 묶음을 직접 해석하기보다 이름 있는 domain view를 우선 사용한다.

강한 규칙:

- 새 포지션 생성은 `PositionSnapshot.open(...)`을 사용한다.
- persistence rehydration은 `PositionSnapshot.restore(...)`를 사용한다. infrastructure mapper/entity가 원시 생성자 인자 순서에 직접 의존하지 않도록 한다.
- 신규 domain 동작에서 `symbol + positionSide + marginMode`를 직접 다시 묶지 말고 `identity()`/`PositionIdentity`를 사용한다.
- 신규 exposure/margin/ROI 동작에서 `leverage`, `quantity`, `entryPrice`, `markPrice`, `liquidationPrice`, `unrealizedPnl` 원시 조합을 반복하지 말고 `exposure()`/`PositionExposure`를 사용한다.
- 신규 누적 실현손익, open/close/funding fee 계산은 `accounting()`/`PositionAccounting`을 먼저 확장한다.
- 호환 생성자는 legacy/test/persistence 전환 과정에서만 남긴다. 새 production 호출 경로를 추가할 때 raw constructor overload를 선택하지 않는다.

이 규칙은 `PositionSnapshot`을 한 번에 큰 aggregate hierarchy로 바꾸기보다, 의미 있는 값 객체를 먼저 만들고 public accessor 호환을 천천히 줄이기 위한 중간 단계다.

## Placement Decision Rule

같은 로직을 어디에 둘지 헷갈릴 때는 아래 순서로 판단한다.

1. 이 로직이 비즈니스 개념의 의미나 상태를 바꾸는가?
2. 이 로직이 둘 이상의 유스케이스에서 반복될 가능성이 있는가?
3. 이 로직이 저장소, HTTP, 외부 SDK를 몰라도 표현 가능한가?

위 질문에 대부분 `예`라면 domain 후보로 본다.
반대로 아래 성격이 강하면 domain이 아니라 다른 레이어에 둔다.

- 유스케이스 시작과 종료를 조합하는 흐름이면 `application`
- provider, repository, connector 호출을 조합하는 흐름이면 `application`
- DB 저장, 외부 API 호출, DTO 변환이면 `infrastructure`

강한 규칙:

- 공유 로직이 필요하면 먼저 domain 후보인지 검토한다.
- application service가 길어지는 이유가 "도메인 계산을 대신 품고 있기 때문"이라면, 먼저 domain으로 올릴 수 있는지 재검토한다.
- domain으로 올릴 수 없고 애플리케이션 메커니즘에 더 가깝다면 `application/<purpose>` 하위 패키지로 분리한다.

## Framework-Free Rule

domain은 프레임워크와 외부 기술을 모른다.

금지:

- Spring annotation
- JPA repository 의존
- `@Entity`, `@Embeddable`
- HTTP 응답 타입 의존
- 외부 API 클라이언트 의존
- feature flag SDK, telemetry SDK, `SecurityContextHolder` 직접 사용

즉, domain은 자신과 `common`에 있는 범용 타입만 의존하는 것이 기본값이다.

### Shared Futures Formula Exception

둘 이상의 futures feature domain이 같은 제품 공식을 사용해야 하고 한 feature domain이 다른 feature domain을
import하면 dependency rule을 위반하는 경우, `common/trading`에 순수 산식만 둘 수 있다.

허용되는 예:

- order preview와 position liquidation이 함께 사용하는 유지증거금률, 격리 청산가, 선형 청산 경계식

금지되는 예:

- 포지션 목록 해석, 교차 청산 후보 선정, repository/provider 호출, application orchestration, feature 상태 전이

이 예외를 사용할 때도 제품 의미와 후보 선정 정책은 feature domain 또는 제품 명세에 남기고, `common/trading`은
입력값만 받는 순수 arithmetic 원천으로 제한한다.

순수 산식의 기준:

- 입력값에만 의존하는 stateless deterministic 함수여야 한다.
- 파일, 네트워크, DB, repository/provider 호출, 전역 상태 변경 같은 side effect를 금지한다.
- feature domain 객체에 의존하지 않고 primitive 또는 간단한 value type만 입력/출력으로 사용한다.
- 허용 범위는 산술 계산, 공통 상수/계수표, 기본 입력 검증까지다.
- 제품 정책 판단, 후보 선정, 상태 전이는 feature domain 또는 제품 명세에 남긴다.
- 제품 산식의 숫자 계수, 기준 비율, 허용 오차, 경계값은 의미 있는 이름을 붙인다. `1`, `0`, `0.005`처럼
  수식 안에서 반복되는 값은 수학적 항등값인지, 제품 계수인지, 테스트 허용 오차인지 코드와 테스트에서 구분되어야 한다.
- domain identity나 value view가 이미 제공하는 판단은 raw string 비교로 반복하지 않는다. 예를 들어 포지션 방향과
  마진 모드 판단은 가능한 한 `identity()`/predicate 메서드를 우선 사용하고, API 또는 persistence 문자열 contract를
  전역 enum으로 바꾸는 작업은 별도 설계 변경으로 다룬다.

## Domain Bean And Wiring Rule

domain을 concrete class로 쓴다는 말은 아무 데서나 직접 생성하라는 뜻이 아니다.
정책 객체나 재사용 계산기처럼 장기 협력 객체는 Spring이 조립하되, domain 클래스 자체는 Spring을 모르게 유지한다.

강한 규칙:

- `domain`에는 Spring annotation을 두지 않는다.
- `domain policy`, `domain service`, 계산기 객체를 빈으로 쓸 때는 `feature/<name>/infrastructure/config`에서 `@Bean`으로 등록한다.
- Spring 관리 클래스 안에서 정책 객체를 필드 초기화로 직접 `new`하지 않는다.
- 값 객체, 엔티티, 결과 DTO, 컬렉션처럼 한 유스케이스 안에서 즉시 소비되는 짧은 수명 객체는 `new`를 허용한다.

예시:

- 허용: `new TradingAccount(...)`
- 허용: `new RewardPointWallet(...)`
- 금지: `@Service` 안에서 `private final RewardPointPolicy policy = new RewardPointPolicy();`

## Persistence Boundary Rule

영속성과 domain은 서로 가깝지만 같은 객체가 아니다.

강한 규칙:

- JPA 엔티티는 도메인 모델과 동일 객체로 취급하지 않는다.
- 영속성 모델과 도메인 모델 사이의 변환 책임을 명시한다.
- 외부 API 응답 DTO를 application/domain으로 직접 전파하지 않는다.
- 인터페이스가 꼭 필요하면 계약은 `application` 또는 `domain`이 소유하고, 구현은 `infrastructure`에 둔다.

## Domain Work Checklist

도메인 관련 작업을 끝내기 전 아래를 확인한다.

- 이 로직이 정말 domain이 소유해야 하는 비즈니스 의미인지 설명할 수 있다.
- 타입 이름과 메서드 이름이 비즈니스 언어로 읽힌다.
- 값 검증이 생성 시점 또는 상태 전이 메서드 안에서 닫혀 있다.
- domain 타입이 Spring/JPA/HTTP/SDK를 모르고 있다.
- 정책 객체를 bean으로 쓸 경우 `infrastructure/config`에서 조립했다.

## Related Documents

- [application-and-providers.md](/Users/hj.park/projects/coin-zzickmock/backend/core/docs/application-and-providers.md)
- [testing-and-architecture-lint.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/architecture/testing-and-architecture-lint.md)
- [persistence-rules.md](/Users/hj.park/projects/coin-zzickmock/backend/storage/docs/persistence-rules.md)
- [exception-rules.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/errors/exception-rules.md)
- [coin-futures-platform-mvp.md](/Users/hj.park/projects/coin-zzickmock/docs/product-specs/coin-futures-platform-mvp.md)
