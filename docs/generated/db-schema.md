# DB Schema

## Purpose

이 문서는 `coin-zzickmock`의 DB 스키마를 사람이 읽기 쉬운 형태로 정리하는 생성/동기화 문서다.
DDL 원문이나 migration 파일 자체를 대체하지는 않지만, 백엔드에서 DB를 읽거나 수정할 때 가장 먼저 확인하는 요약 문서로 사용한다.

이 문서는 특히 `Flyway` migration을 기준 원문으로 삼아 현재 스키마를 통합해서 설명하는 문서다.

중요:

- 이 문서는 현재 실제 DB 구조를 확정해서 적은 문서가 아니다.
- 아직 사용자 지시나 실제 migration/schema 정의가 없으면, 사실을 지어내지 않는다.
- 모르는 내용은 비워 두거나 `미정`, `아직 정의되지 않음`으로 명시한다.

## Status

- 상태: 구현 반영됨
- 마지막 스키마 동기화: 2026-04-16
- 기준 소스: Flyway migration + JPA entity + Spring Boot datasource 설정

## Source Of Truth

현재 저장소에서 실제 DB 구조의 원문으로 취급할 항목을 여기에 적는다.

- 운영 DB 기준: MySQL
- 테스트 DB 기준: H2 in-memory (`MODE=MySQL`)
- migration 기준: Flyway
- datasource 설정:
  [backend/src/main/resources/application.yml](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/application.yml)
  [backend/src/test/resources/application-test.yml](/Users/hj.park/projects/coin-zzickmock/backend/src/test/resources/application-test.yml)
- JPA entity 기준:
  [TradingAccountJpaEntity](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/account/infrastructure/persistence/TradingAccountJpaEntity.java)
  [FuturesOrderJpaEntity](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/order/infrastructure/persistence/FuturesOrderJpaEntity.java)
  [OpenPositionJpaEntity](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/position/infrastructure/persistence/OpenPositionJpaEntity.java)
  [RewardPointWalletJpaEntity](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/reward/infrastructure/persistence/RewardPointWalletJpaEntity.java)
- Query layer 기준:
  [JpaPositionRepository](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/position/infrastructure/persistence/JpaPositionRepository.java)
- migration 파일:
  [V1__initial_schema.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V1__initial_schema.sql)
- 수동 SQL 기준 여부: 없음

읽기/수정 규칙:

- DB 스키마를 읽는 작업은 이 문서를 먼저 보고 현재 테이블, 컬럼, 관계를 파악한다.
- DB 스키마를 수정하는 작업은 먼저 `backend/src/main/resources/db/migration` 아래에 새 `Flyway` 버전 파일을 추가한다.
- 기존 migration을 덮어쓰는 대신 새 버전 파일로 누적 변경을 남긴다.
- migration을 추가한 뒤 코드와 이 문서를 같은 작업에서 함께 갱신한다.

원문이 정해지면 이 문서보다 원문이 우선한다.
이 문서는 원문을 요약하고 작업자가 빠르게 파악할 수 있게 돕는 역할을 한다.

우선순위는 아래처럼 본다.

1. `Flyway` migration
2. datasource 설정
3. JPA entity / QueryDSL / `JdbcTemplate` 구현
4. 이 문서 (`db-schema.md`)

## Database Summary

- 데이터베이스 종류:
  운영 `MySQL 8.x`
  테스트 `H2 in-memory`
- 주요 도메인:
  계정, 보상 포인트, 선물 주문, 오픈 포지션
- 네이밍 규칙:
  테이블은 `snake_case`
  시간 컬럼은 `created_at`, `updated_at`
  금액/가격 계열은 `DECIMAL`
  도메인 enum 성격 값은 현재 `VARCHAR` 문자열로 저장

## Tables

### `trading_accounts`

- 목적:
  사용자 선물 계정의 현재 잔고와 사용 가능 증거금을 저장한다.
- PK:
  `member_id`
- 주요 컬럼:
  `member_email`, `member_name`, `wallet_balance`, `available_margin`, `created_at`, `updated_at`
- 관련 엔티티/모듈:
  `feature.account`
- 관련 migration 또는 schema 파일:
  [V1__initial_schema.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V1__initial_schema.sql),
  [TradingAccountJpaEntity](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/account/infrastructure/persistence/TradingAccountJpaEntity.java)

### `reward_point_wallets`

- 목적:
  실현 손익 기반으로 적립되는 포인트 잔액을 저장한다.
- PK:
  `member_id`
- 주요 컬럼:
  `reward_point`, `created_at`, `updated_at`
- 관련 엔티티/모듈:
  `feature.reward`
- 관련 migration 또는 schema 파일:
  [V1__initial_schema.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V1__initial_schema.sql),
  [RewardPointWalletJpaEntity](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/reward/infrastructure/persistence/RewardPointWalletJpaEntity.java)

### `futures_orders`

- 목적:
  주문 요청과 체결 결과를 이력으로 저장한다.
- PK:
  `id` (auto increment)
- 주요 컬럼:
  `order_id`, `member_id`, `symbol`, `position_side`, `order_type`, `margin_mode`, `leverage`, `quantity`, `limit_price`, `status`, `fee_type`, `estimated_fee`, `execution_price`, `created_at`
- 관련 엔티티/모듈:
  `feature.order`
- 관련 migration 또는 schema 파일:
  [V1__initial_schema.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V1__initial_schema.sql),
  [FuturesOrderJpaEntity](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/order/infrastructure/persistence/FuturesOrderJpaEntity.java)

### `open_positions`

- 목적:
  현재 열려 있는 포지션의 집계 상태를 저장한다.
- PK:
  `id` (auto increment)
- 주요 컬럼:
  `member_id`, `symbol`, `position_side`, `margin_mode`, `leverage`, `quantity`, `entry_price`, `mark_price`, `liquidation_price`, `unrealized_pnl`, `created_at`, `updated_at`
- 관련 엔티티/모듈:
  `feature.position`
- 관련 migration 또는 schema 파일:
  [V1__initial_schema.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V1__initial_schema.sql),
  [OpenPositionJpaEntity](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/position/infrastructure/persistence/OpenPositionJpaEntity.java)

## Relationships

- `reward_point_wallets.member_id -> trading_accounts.member_id`:
  계정당 하나의 포인트 지갑을 가진다.
- `futures_orders.member_id -> trading_accounts.member_id`:
  주문 이력은 특정 계정에 속한다.
- `open_positions.member_id -> trading_accounts.member_id`:
  오픈 포지션은 특정 계정에 속한다.
- `open_positions(member_id, symbol, position_side, margin_mode)`:
  한 계정에서 동일 심볼/방향/마진 모드 조합은 하나의 집계 포지션만 가진다.

## Change Log

- 2026-04-16:
  MySQL 운영 DB + H2 테스트 DB 기준을 확정했다.
- 2026-04-16:
  `Flyway`를 DB migration 표준으로 확정하고, `db-schema.md`를 migration 통합 요약 문서로 정했다.
- 2026-04-16:
  `V1__initial_schema.sql`로 초기 스키마 migration을 추가했다.
- 2026-04-16:
  `trading_accounts`, `reward_point_wallets`, `futures_orders`, `open_positions` JPA entity를 source of truth로 연결했다.
- 2026-04-16:
  `JpaPositionRepository`에 OpenFeign 포크 `querydsl-jpa` 기반 조회를 추가했다.

## Update Rule

아래 상황에서는 이 문서를 같이 갱신한다.

- migration 파일 추가 또는 수정
- JPA entity 구조 변경
- repository가 의존하는 테이블/컬럼 구조 변경
- 수동 SQL 또는 배치 SQL 변경

갱신 시 원칙:

- 스키마 변경은 먼저 `Flyway` migration에 반영하고, 이 문서는 그 결과를 요약한다.
- `Flyway` migration은 `backend/src/main/resources/db/migration` 아래에 새 버전 파일로 추가한다.
- 버전은 기존 최신 버전보다 큰 새 번호를 사용한다. 예: `V1__initial_schema.sql` 다음 변경은 `V2__add_open_positions_index.sql`
- 모르는 내용은 지어내지 않는다.
- 확인 가능한 사실만 적는다.
- 실제 원문 경로를 함께 남긴다.
