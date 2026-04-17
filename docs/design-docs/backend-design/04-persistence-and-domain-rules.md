# Backend Persistence And Domain Rules

## Purpose

이 문서는 domain 규칙, 영속성/외부 연동 기준, 예외 모델, 네이밍 규칙을 모은다.
도메인 모델을 어떤 언어로 써야 하는지, JPA/QueryDSL/JdbcTemplate을 어디까지 허용하는지, 클래스 이름에 무엇을 드러내야 하는지를 여기서 확인한다.

먼저 읽어야 하는 문서:

- [README.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/README.md)
- [01-architecture-foundations.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/01-architecture-foundations.md)

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

## Related Documents

- [02-package-and-wiring.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/02-package-and-wiring.md)
- [03-application-and-providers.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/03-application-and-providers.md)
- [05-testing-and-lint.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/05-testing-and-lint.md)
- [docs/generated/db-schema.md](/Users/hj.park/projects/coin-zzickmock/docs/generated/db-schema.md)
