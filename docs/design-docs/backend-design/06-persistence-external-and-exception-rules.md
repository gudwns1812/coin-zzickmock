# Backend Persistence, External Integration, And Exception Rules

## Purpose

이 문서는 영속성, 외부 연동, 예외 번역, 기술 중심 네이밍 규칙을 모은다.
도메인 규칙 원문은 [04-domain-modeling-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/04-domain-modeling-rules.md)가 소유한다.

먼저 읽어야 하는 문서:

- [README.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/README.md)
- [01-architecture-foundations.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/01-architecture-foundations.md)

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

- 단순 조회/저장은 JPA repository 또는 QueryDSL adapter에서 우선 해결한다.
- JPA repository, gateway, connector 위에 이유 없는 중간 인터페이스를 하나 더 만들지 않는다.
- `JdbcTemplate`을 쓰더라도 SQL은 `infrastructure/persistence`에만 두고, application/domain에는 노출하지 않는다.
- 스키마 변경의 원문은 `Flyway` migration으로 남기고, `db-schema.md`는 그 결과를 통합 요약하는 문서로 유지한다.
- 테스트는 운영과 다른 DB 엔진을 쓰더라도 MySQL 동작과 의미가 어긋나지 않게 작성한다.

DB를 참고하거나 수정하는 작업에서는 반드시 [docs/generated/db-schema.md](/Users/hj.park/projects/coin-zzickmock/docs/generated/db-schema.md)를 함께 본다.

- 스키마를 읽는 작업: 먼저 `db-schema.md`를 확인하고 현재 테이블/컬럼/관계를 파악한다.
- 스키마를 바꾸는 작업: 먼저 `backend/src/main/resources/db/migration` 아래에 새 버전의 `Flyway` migration 파일을 추가하고, 코드 변경과 함께 `db-schema.md`도 최신 상태로 갱신한다.
- 엔티티, repository, migration, SQL을 바꿨는데 `db-schema.md`가 그대로면 작업이 덜 끝난 것으로 본다.

### Persistence Mutation Rule

단건 수정, 생성, 벌크 변경은 repository 계약에서 서로 다른 의도로 드러나야 한다.

- 생성은 `create`, `open`, `provision`처럼 insert 의도를 드러내고, 수동 id 엔티티에서는 generic Spring Data `save` wrapper로 insert-only를 흉내 내지 않는다. `EntityManager.persist`, 명시적 insert, 또는 검증된 `Persistable.isNew` 패턴처럼 중복 생성이 업데이트로 바뀌지 않는 구현을 사용한다.
- 단건 수정은 현재 트랜잭션에서 managed entity를 조회한 뒤 의미 있는 엔티티 메서드로 상태를 바꾸고 dirty checking에 맡긴다. `find` 후 필드 변경을 하고 다시 `save`를 호출하지 않는다.
- `saveAndFlush` 또는 수동 `flush`는 즉시 constraint 확인이나 같은 트랜잭션 내 후속 query ordering처럼 이유가 있는 경우에만 사용한다. 사용 지점에는 method name이나 짧은 주석으로 즉시 flush가 필요한 이유를 남긴다.
- 여러 행을 같은 의도로 바꾸는 주문 상태/수량 정리 같은 작업은 단건 repository 호출 반복보다 bulk/batch repository method를 우선 검토한다.
- Bulk update는 dirty checking, entity callback, 개별 엔티티 메서드를 우회한다. 따라서 이미 application/domain에서 결정된 persistence 변경을 적용할 때만 쓰고, entity 메서드가 하던 보조 컬럼 정리도 SQL 계약에 명시한다.
- 조회 query는 누락된 필수 row를 자동 생성하지 않는다. 예를 들어 회원의 `TradingAccount`가 없으면 계좌 조회에서 생성하지 않고 데이터 오류로 처리한다.

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

## Technical Naming Rule

기술 세부사항은 클래스명보다 패키지와 프레임워크 타입으로 드러내는 것을 우선한다.
동일한 기능을 여러 기술로 구현해야 한다면 기술명을 클래스명에 붙여 구분하지 않고, 먼저 기술 중립적인 계약을 둔다.
계약 이름은 application/domain 언어의 역할을 드러내고, 구현 기술 선택은 `infrastructure` 하위 패키지, Spring configuration, profile, qualifier 같은 조립 정보로 구분한다.
인터페이스는 이 경우처럼 실제 다중 구현이나 외부 경계 격리가 있을 때만 만들며, 단일 구현을 위해 기계적으로 만들지 않는다.

권장:

- `OrderPersistenceRepository`
- `TradingAccountEntity`
- `MarketDataConnector`
- `LeaderboardSnapshotStore`

금지:

- `TradingAccountJpaEntity`
- `LoadPortfolioPort`
- `RedisLeaderboardSnapshotStore`
- `JpaLeaderboardSnapshotStore`
- `OrderManager`
- `OrderHelper`
- `CommonService`

추가 규칙:

- 엔티티, repository 구현, Spring Data 인터페이스 이름에는 `Jpa`, `SpringData`, `MyBatis`, `Redis` 같은 기술명을 넣지 않는다.
- 기술 세부사항은 패키지 경로, annotation, `JpaRepository` 같은 프레임워크 타입으로만 드러내고 클래스명에는 넣지 않는다.
- 같은 역할의 구현이 둘 이상이면 `LeaderboardSnapshotStore`처럼 역할 중심 인터페이스를 두고, 구현체는 `infrastructure/persistence`, `infrastructure/store`, `providers/infrastructure/config` 같은 위치와 bean 조립으로 선택한다.
- 불리언은 반드시 술어형으로 작성한다.

예:

- `isActive`
- `hasPosition`
- `canTrade`
- `shouldLiquidate`

## Related Documents

- [04-domain-modeling-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/04-domain-modeling-rules.md)
- [05-testing-and-lint.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/05-testing-and-lint.md)
- [docs/generated/db-schema.md](/Users/hj.park/projects/coin-zzickmock/docs/generated/db-schema.md)
