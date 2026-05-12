# Backend Persistence Rules

## Purpose

이 문서는 DB, repository, QueryDSL, migration 같은 영속성 규칙만 소유한다.
외부 연동 원문은 [08-external-integration-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/08-external-integration-rules.md)가,
예외 모델 원문은 [09-exception-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/09-exception-rules.md)가,
기술 중심 네이밍 원문은 [10-technical-naming-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/10-technical-naming-rules.md)가 소유한다.
도메인 규칙 원문은 [04-domain-modeling-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/04-domain-modeling-rules.md)가 소유한다.

먼저 읽어야 하는 문서:

- [README.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/README.md)
- [01-architecture-foundations.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/01-architecture-foundations.md)

## Persistence Boundary

영속성 구현은 `infrastructure`에 둔다.

권장 구조:

- 영속성: `feature/<feature>/infrastructure/persistence`
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
- JPA repository 위에 이유 없는 중간 인터페이스를 하나 더 만들지 않는다.
- `JdbcTemplate`을 쓰더라도 SQL은 `infrastructure/persistence`에만 두고, application/domain에는 노출하지 않는다.
- 스키마 변경의 원문은 `Flyway` migration으로 남기고, `db-schema.md`는 그 결과를 통합 요약하는 문서로 유지한다.
- 테스트는 운영과 다른 DB 엔진을 쓰더라도 MySQL 동작과 의미가 어긋나지 않게 작성한다.

DB를 참고하거나 수정하는 작업에서는 반드시 [docs/generated/db-schema.md](/Users/hj.park/projects/coin-zzickmock/docs/generated/db-schema.md)를 함께 본다.

- 스키마를 읽는 작업: 먼저 `db-schema.md`를 확인하고 현재 테이블/컬럼/관계를 파악한다.
- 스키마를 바꾸는 작업: 먼저 현재 `backend/storage/src/main/resources/db/migration` 아래에 새 버전의 `Flyway` migration 파일을 추가하고, 코드 변경과 함께 `db-schema.md`도 최신 상태로 갱신한다.
- 엔티티, repository, migration, SQL을 바꿨는데 `db-schema.md`가 그대로면 작업이 덜 끝난 것으로 본다.

## Persistence Mutation Rule

단건 수정, 생성, 벌크 변경은 repository 계약에서 서로 다른 의도로 드러나야 한다.

- 생성은 `create`, `open`, `provision`처럼 insert 의도를 드러내고, 수동 id 엔티티에서는 generic Spring Data `save` wrapper로 insert-only를 흉내 내지 않는다. `EntityManager.persist`, 명시적 insert, 또는 검증된 `Persistable.isNew` 패턴처럼 중복 생성이 업데이트로 바뀌지 않는 구현을 사용한다.
- 단건 수정은 현재 트랜잭션에서 managed entity를 조회한 뒤 의미 있는 엔티티 메서드로 상태를 바꾸고 dirty checking에 맡긴다. `find` 후 필드 변경을 하고 다시 `save`를 호출하지 않는다.
- `saveAndFlush` 또는 수동 `flush`는 즉시 constraint 확인이나 같은 트랜잭션 내 후속 query ordering처럼 이유가 있는 경우에만 사용한다. 사용 지점에는 method name이나 짧은 주석으로 즉시 flush가 필요한 이유를 남긴다.
- 여러 행을 같은 의도로 바꾸는 주문 상태/수량 정리 같은 작업은 단건 repository 호출 반복보다 bulk/batch repository method를 우선 검토한다.
- Bulk update는 dirty checking, entity callback, 개별 엔티티 메서드를 우회한다. 따라서 이미 application/domain에서 결정된 persistence 변경을 적용할 때만 쓰고, entity 메서드가 하던 보조 컬럼 정리도 SQL 계약에 명시한다.
- 조회 query는 누락된 필수 row를 자동 생성하지 않는다. 예를 들어 회원의 `TradingAccount`가 없으면 계좌 조회에서 생성하지 않고 데이터 오류로 처리한다.

## Related Documents

- [04-domain-modeling-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/04-domain-modeling-rules.md)
- [05-testing-and-lint.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/05-testing-and-lint.md)
- [08-external-integration-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/08-external-integration-rules.md)
- [09-exception-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/09-exception-rules.md)
- [10-technical-naming-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/10-technical-naming-rules.md)
- [docs/generated/db-schema.md](/Users/hj.park/projects/coin-zzickmock/docs/generated/db-schema.md)
