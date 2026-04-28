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
- 마지막 스키마 동기화: 2026-04-20
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
  [TradingAccountEntity](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/account/infrastructure/persistence/TradingAccountEntity.java)
  [MemberCredentialEntity](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/member/infrastructure/persistence/MemberCredentialEntity.java)
  [FuturesOrderEntity](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/order/infrastructure/persistence/FuturesOrderEntity.java)
  [OpenPositionEntity](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/position/infrastructure/persistence/OpenPositionEntity.java)
  [RewardPointWalletEntity](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/reward/infrastructure/persistence/RewardPointWalletEntity.java)
- Query layer 기준:
  [PositionPersistenceRepository](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/position/infrastructure/persistence/PositionPersistenceRepository.java)
- migration 파일:
  [V1__initial_schema.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V1__initial_schema.sql)
  [V2__add_member_credentials.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V2__add_member_credentials.sql)
  [V3__add_market_history_schema.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V3__add_market_history_schema.sql)
  [V4__remove_trade_count_from_market_history.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V4__remove_trade_count_from_market_history.sql)
  [V12__add_reward_shop_foundation.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V12__add_reward_shop_foundation.sql)
  [V5__add_position_history_and_close_order_contract.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V5__add_position_history_and_close_order_contract.sql)
  [V6__add_open_position_version.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V6__add_open_position_version.sql)
  [V7__add_market_symbol_funding_schedule.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V7__add_market_symbol_funding_schedule.sql)
  [V8__add_net_pnl_position_accounting.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V8__add_net_pnl_position_accounting.sql)
  [V9__add_position_take_profit_stop_loss.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V9__add_position_take_profit_stop_loss.sql)
  [V10__add_futures_order_conditional_trigger_fields.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V10__add_futures_order_conditional_trigger_fields.sql)
  [V11__backfill_and_constrain_conditional_close_orders.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V11__backfill_and_constrain_conditional_close_orders.sql)
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
  계정, 회원 자격 증명, 보상 포인트, 선물 주문, 오픈 포지션, 시장 심볼, 1분봉 원본, 1시간봉 롤업
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
  [TradingAccountEntity](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/account/infrastructure/persistence/TradingAccountEntity.java)

### `reward_point_wallets`

- 목적:
  실현 손익 기반으로 적립되는 포인트 잔액을 저장한다.
- PK:
  `member_id`
- 주요 컬럼:
  `reward_point`, `version`, `created_at`, `updated_at`
- 포인트 타입:
  `reward_point`는 정수 포인트다. 과거 `DECIMAL(19,2)` 값은 `.00` whole-point 값을 보존하는 방향으로 migration에서 정수 컬럼으로 전환한다.
- 동시성:
  `version`은 포인트 적립/차감/환급 시 낙관적 잠금 조건으로 사용한다.
- 관련 엔티티/모듈:
  `feature.reward`
- 관련 migration 또는 schema 파일:
  [V1__initial_schema.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V1__initial_schema.sql),
  [V12__add_reward_shop_foundation.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V12__add_reward_shop_foundation.sql),
  [RewardPointWalletEntity](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/reward/infrastructure/persistence/RewardPointWalletEntity.java)

### `member_credentials`

- 목적:
  로그인 아이디 기준의 비밀번호 해시와 회원 프로필을 저장한다.
- PK:
  `member_id`
- 주요 컬럼:
  `password_hash`, `member_name`, `member_email`, `phone_number`, `zip_code`, `address`, `address_detail`, `invest_score`, `role`, `created_at`, `updated_at`
- 권한:
  `role`은 `USER`/`ADMIN` 문자열로 저장한다. 기존 `test` 계정과 fresh test-profile seed는 관리자 처리를 위해 `ADMIN`이 된다.
- 관련 엔티티/모듈:
  `feature.member`
- 관련 migration 또는 schema 파일:
  [V2__add_member_credentials.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V2__add_member_credentials.sql),
  [V12__add_reward_shop_foundation.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V12__add_reward_shop_foundation.sql),
  [MemberCredentialEntity](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/member/infrastructure/persistence/MemberCredentialEntity.java)

### `reward_shop_items`

- 목적:
  포인트 상점 상품을 DB 운영 데이터로 저장한다. MVP에서는 admin item CRUD UI/API 없이 migration/bootstrap/data-admin 경로로 관리한다.
- PK:
  `id` (auto increment)
- 유니크:
  `code`
- 주요 컬럼:
  `code`, `name`, `description`, `item_type`, `price`, `active`, `total_stock`, `sold_quantity`, `per_member_purchase_limit`, `sort_order`, `version`, `created_at`, `updated_at`
- 판매 가능성:
  별도 `sellable` 컬럼은 없다. 판매 가능 여부는 `active`, `sold_quantity`, `total_stock`, 유저별 `purchase_count`로 계산한다.
- 재고:
  `sold_quantity`는 item-level 재고 소진 수량이다. 유한 재고 상품은 `sold_quantity <= total_stock` 제약을 가진다.
- 관련 엔티티/모듈:
  `feature.reward`
- 관련 migration 또는 schema 파일:
  [V12__add_reward_shop_foundation.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V12__add_reward_shop_foundation.sql),
  [RewardShopItemEntity](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/reward/infrastructure/persistence/RewardShopItemEntity.java)

### `reward_shop_member_item_usages`

- 목적:
  회원별 상품 구매 제한 카운트를 저장한다.
- PK:
  `id` (auto increment)
- 유니크:
  `member_id`, `shop_item_id`
- 주요 컬럼:
  `member_id`, `shop_item_id`, `purchase_count`, `version`, `created_at`, `updated_at`
- 구매 제한 기준:
  `purchase_count`는 `PENDING`/`SENT` 요청만 카운트한다. `CANCELLED_REFUNDED` 전환은 guarded decrement로 카운트를 한 번만 복구한다.
- 관련 엔티티/모듈:
  `feature.reward`
- 관련 migration 또는 schema 파일:
  [V12__add_reward_shop_foundation.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V12__add_reward_shop_foundation.sql),
  [RewardShopMemberItemUsageEntity](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/reward/infrastructure/persistence/RewardShopMemberItemUsageEntity.java)

### `reward_point_histories`

- 목적:
  포인트 적립, 교환권 차감, 환급 이력을 불변 로그로 저장한다.
- PK:
  `id` (auto increment)
- 주요 컬럼:
  `member_id`, `history_type`, `amount`, `balance_after`, `source_type`, `source_reference`, `created_at`, `updated_at`
- 이력 타입:
  `GRANT`, `REDEMPTION_DEDUCT`, `REDEMPTION_REFUND`
- 관련 엔티티/모듈:
  `feature.reward`
- 관련 migration 또는 schema 파일:
  [V12__add_reward_shop_foundation.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V12__add_reward_shop_foundation.sql),
  [RewardPointHistoryEntity](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/reward/infrastructure/persistence/RewardPointHistoryEntity.java)

### `reward_redemption_requests`

- 목적:
  포인트 상점 교환권 요청과 관리자 처리 상태를 저장한다.
- PK:
  `id` (auto increment)
- 유니크:
  `request_id`
- 주요 컬럼:
  `request_id`, `member_id`, `shop_item_id`, `item_code`, `item_name`, `item_price`, `point_amount`, `submitted_phone_number`, `normalized_phone_number`, `status`, `requested_at`, `sent_at`, `cancelled_at`, `admin_member_id`, `admin_memo`, `version`, `created_at`, `updated_at`
- 스냅샷:
  `item_code`, `item_name`, `item_price`, `point_amount`는 요청 시점의 상품/가격 스냅샷이다.
- 상태:
  MVP 상태는 `PENDING`, `SENT`, `CANCELLED_REFUNDED`다.
- 관련 엔티티/모듈:
  `feature.reward`
- 관련 migration 또는 schema 파일:
  [V12__add_reward_shop_foundation.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V12__add_reward_shop_foundation.sql),
  [RewardRedemptionRequestEntity](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/reward/infrastructure/persistence/RewardRedemptionRequestEntity.java)

### `futures_orders`

- 목적:
  주문 요청과 체결 결과를 이력으로 저장한다.
- PK:
  `id` (auto increment)
- 주요 컬럼:
  `order_id`, `member_id`, `symbol`, `position_side`, `order_type`, `order_purpose`, `margin_mode`, `leverage`, `quantity`, `limit_price`, `status`, `fee_type`, `estimated_fee`, `execution_price`, `trigger_price`, `trigger_type`, `trigger_source`, `oco_group_id`, `active_conditional_trigger_type`, `created_at`
- 조건부 주문:
  TP/SL은 pending `CLOSE_POSITION` 주문으로 저장한다. `trigger_source`는 `MARK_PRICE`, `trigger_type`은 `TAKE_PROFIT` 또는 `STOP_LOSS`다. TP/SL sibling은 같은 `oco_group_id`를 공유할 수 있다.
- 조건부 주문 유일성:
  `active_conditional_trigger_type`은 pending conditional close order일 때만 `trigger_type`을 복사하고, 그 외 주문/상태에서는 `NULL`이다. `uk_futures_orders_active_conditional_close`는 같은 `member_id + symbol + position_side + margin_mode` 안에서 active pending TP와 SL을 trigger type별 하나씩만 허용한다.
- 관련 엔티티/모듈:
  `feature.order`
- 관련 migration 또는 schema 파일:
  [V1__initial_schema.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V1__initial_schema.sql),
  [V10__add_futures_order_conditional_trigger_fields.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V10__add_futures_order_conditional_trigger_fields.sql),
  [V11__backfill_and_constrain_conditional_close_orders.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V11__backfill_and_constrain_conditional_close_orders.sql),
  [FuturesOrderEntity](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/order/infrastructure/persistence/FuturesOrderEntity.java)

### `open_positions`

- 목적:
  현재 열려 있는 포지션의 집계 상태를 저장한다.
- PK:
  `id` (auto increment)
- 주요 컬럼:
  `member_id`, `symbol`, `position_side`, `margin_mode`, `leverage`, `quantity`, `entry_price`, `mark_price`, `liquidation_price`, `unrealized_pnl`, `opened_at`, `original_quantity`, `accumulated_closed_quantity`, `accumulated_exit_notional`, `accumulated_realized_pnl`, `accumulated_open_fee`, `accumulated_close_fee`, `accumulated_funding_cost`, `take_profit_price`, `stop_loss_price`, `version`, `created_at`, `updated_at`
- legacy 컬럼:
  `take_profit_price`, `stop_loss_price`는 V9 호환 컬럼으로 남아 있지만 TP/SL의 source of truth가 아니다. 현재 TP/SL read/write/trigger path는 `futures_orders`의 조건부 close order를 사용한다.
- 동시성:
  `version`은 포지션 종료/청산/종료 주문 체결 시 낙관적 잠금 조건으로 사용한다. 버전 불일치 시 계정 정산, 포지션 이력, 리워드, SSE 이벤트를 수행하지 않고 재조회가 필요한 충돌로 처리한다.
- 관련 엔티티/모듈:
  `feature.position`
- 관련 migration 또는 schema 파일:
  [V1__initial_schema.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V1__initial_schema.sql),
  [V8__add_net_pnl_position_accounting.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V8__add_net_pnl_position_accounting.sql),
  [V9__add_position_take_profit_stop_loss.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V9__add_position_take_profit_stop_loss.sql),
  [OpenPositionEntity](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/position/infrastructure/persistence/OpenPositionEntity.java)

### `position_history`

- 목적:
  완전히 종료된 포지션의 요약 이력을 저장한다. 부분 종료는 열린 포지션의 누적 필드에 보존하고, 포지션이 0이 되는 순간에만 이 테이블에 기록한다.
- PK:
  `id` (auto increment)
- 주요 컬럼:
  `member_id`, `symbol`, `position_side`, `margin_mode`, `leverage`, `opened_at`, `average_entry_price`, `average_exit_price`, `position_size`, `realized_pnl`, `gross_realized_pnl`, `open_fee`, `close_fee`, `total_fee`, `funding_cost`, `net_realized_pnl`, `roi`, `closed_at`, `close_reason`, `created_at`
- 손익 의미:
  `realized_pnl`은 API 호환용 net PnL이며 `net_realized_pnl`과 같은 값을 저장한다. `gross_realized_pnl`은 수수료/funding 차감 전 종료 손익이다.
- 관련 엔티티/모듈:
  `feature.position`
- 관련 migration 또는 schema 파일:
  [V5__add_position_history_and_close_order_contract.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V5__add_position_history_and_close_order_contract.sql),
  [V8__add_net_pnl_position_accounting.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V8__add_net_pnl_position_accounting.sql),
  [PositionHistoryEntity](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/position/infrastructure/persistence/PositionHistoryEntity.java)

### `market_symbols`

- 목적:
  선물 차트와 시세 수집이 참조하는 거래 심볼 기준 정보를 저장한다.
- PK:
  `id` (auto increment)
- 주요 컬럼:
  `symbol`, `display_name`, `base_asset`, `quote_asset`, `price_scale`, `quantity_scale`, `price_step`, `quantity_step`, `max_leverage`, `active`, `funding_interval_hours`, `funding_anchor_hour`, `funding_time_zone`, `created_at`, `updated_at`
- 관련 엔티티/모듈:
  `feature.market.infrastructure.persistence.MarketSymbolEntity`가 심볼과 funding schedule metadata를 읽는다.
- 관련 migration 또는 schema 파일:
  [V3__add_market_history_schema.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V3__add_market_history_schema.sql),
  [V7__add_market_symbol_funding_schedule.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V7__add_market_symbol_funding_schedule.sql)
- 초기 시드:
  `BTCUSDT`, `ETHUSDT`

### `market_candles_1m`

- 목적:
  차트와 롤업의 기준 원본이 되는 1분봉 시계열 데이터를 저장한다.
- PK:
  `id` (auto increment)
- 주요 컬럼:
  `symbol_id`, `open_time`, `close_time`, `open_price`, `high_price`, `low_price`, `close_price`, `volume`, `quote_volume`, `created_at`, `updated_at`
- 관련 엔티티/모듈:
  현재는 전용 JPA entity가 없고 `feature.market`의 향후 시계열 영속성 기준 테이블로 예약되어 있다.
- 관련 migration 또는 schema 파일:
  [V3__add_market_history_schema.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V3__add_market_history_schema.sql),
  [V4__remove_trade_count_from_market_history.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V4__remove_trade_count_from_market_history.sql)
- 인덱스:
  `uk_market_candles_1m_symbol_open_time`로 심볼별 시각 중복을 막고,
  `idx_market_candles_1m_open_time_symbol`로 시간 구간 기준 롤업 조회를 빠르게 한다.

### `market_candles_1h`

- 목적:
  1분봉 원본에서 만들어진 1시간봉 롤업 데이터를 저장해 차트 조회 비용을 줄인다.
- PK:
  `id` (auto increment)
- 주요 컬럼:
  `symbol_id`, `open_time`, `close_time`, `open_price`, `high_price`, `low_price`, `close_price`, `volume`, `quote_volume`, `source_minute_open_time`, `source_minute_close_time`, `created_at`, `updated_at`
- 관련 엔티티/모듈:
  현재는 전용 JPA entity가 없고 `feature.market`의 향후 롤업 영속성 기준 테이블로 예약되어 있다.
- 관련 migration 또는 schema 파일:
  [V3__add_market_history_schema.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V3__add_market_history_schema.sql),
  [V4__remove_trade_count_from_market_history.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V4__remove_trade_count_from_market_history.sql)
- 인덱스:
  `uk_market_candles_1h_symbol_open_time`로 심볼별 시각 중복을 막고,
  `idx_market_candles_1h_open_time_symbol`로 시간 구간 기준 조회와 재롤업 범위 탐색을 빠르게 한다.

## Relationships

- `reward_point_wallets.member_id -> trading_accounts.member_id`:
  계정당 하나의 포인트 지갑을 가진다.
- `member_credentials.member_id -> trading_accounts.member_id`:
  인증에 필요한 회원 자격 증명은 선물 계정과 같은 `member_id`를 공유한다.
- `futures_orders.member_id -> trading_accounts.member_id`:
  주문 이력은 특정 계정에 속한다.
- `open_positions.member_id -> trading_accounts.member_id`:
  오픈 포지션은 특정 계정에 속한다.
- `open_positions(member_id, symbol, position_side, margin_mode)`:
  한 계정에서 동일 심볼/방향/마진 모드 조합은 하나의 집계 포지션만 가진다.
- `market_candles_1m.symbol_id -> market_symbols.id`:
  1분봉은 반드시 등록된 거래 심볼에 속한다.
- `market_candles_1h.symbol_id -> market_symbols.id`:
  1시간봉 롤업도 반드시 등록된 거래 심볼에 속한다.
- `market_candles_1m(symbol_id, open_time)`:
  동일 심볼에서 같은 시작 시각의 1분봉은 하나만 존재한다.
- `market_candles_1h(symbol_id, open_time)`:
  동일 심볼에서 같은 시작 시각의 1시간봉은 하나만 존재한다.

## Change Log

- 2026-04-16:
  MySQL 운영 DB + H2 테스트 DB 기준을 확정했다.
- 2026-04-16:
  `Flyway`를 DB migration 표준으로 확정하고, `db-schema.md`를 migration 통합 요약 문서로 정했다.
- 2026-04-16:
  `V1__initial_schema.sql`로 초기 스키마 migration을 추가했다.
- 2026-04-16:
  `V2__add_member_credentials.sql`로 로그인/회원가입 동기화를 위한 `member_credentials` 테이블을 추가했다.
- 2026-04-16:
  `V3__add_market_history_schema.sql`로 `market_symbols`, `market_candles_1m`, `market_candles_1h`와 시간축 인덱스를 추가했다.
- 2026-04-16:
  `trading_accounts`, `reward_point_wallets`, `futures_orders`, `open_positions` entity를 source of truth로 연결했다.
- 2026-04-16:
  `MemberCredentialEntity`를 source of truth에 추가하고, 로컬 회원 자격 증명 저장 구조를 문서화했다.
- 2026-04-16:
  `PositionPersistenceRepository`에 OpenFeign 포크 `querydsl-jpa` 기반 조회를 추가했다.
- 2026-04-26:
  `V6__add_open_position_version.sql`로 `open_positions.version`을 추가하고 포지션 종료 계열 mutation의 낙관적 잠금 기준으로 문서화했다.
- 2026-04-27:
  `V9__add_position_take_profit_stop_loss.sql`로 `open_positions.take_profit_price`, `open_positions.stop_loss_price`를 추가하고 포지션 단위 TP/SL 트리거 저장 위치를 문서화했다.
- 2026-04-28:
  `V10__add_futures_order_conditional_trigger_fields.sql`로 `futures_orders.trigger_price`, `trigger_type`, `trigger_source`, `oco_group_id`를 추가했다. TP/SL source of truth는 open position 컬럼에서 pending conditional close order로 이동했다.
- 2026-04-28:
  `V11__backfill_and_constrain_conditional_close_orders.sql`로 V9 legacy `open_positions.take_profit_price`, `stop_loss_price`를 pending conditional close order로 backfill하고, active conditional TP/SL 중복을 막는 unique index를 추가했다.

## Update Rule

아래 상황에서는 이 문서를 같이 갱신한다.

- migration 파일 추가 또는 수정
- JPA entity 구조 변경
- repository가 의존하는 테이블/컬럼 구조 변경
- 수동 SQL 또는 배치 SQL 변경

갱신 시 원칙:

- 스키마 변경은 먼저 `Flyway` migration에 반영하고, 이 문서는 그 결과를 요약한다.
- `Flyway` migration은 `backend/src/main/resources/db/migration` 아래에 새 버전 파일로 추가한다.
- 버전은 기존 최신 버전보다 큰 새 번호를 사용한다. 예: `V3__add_market_history_schema.sql` 다음 변경은 `V4__add_market_history_rollup_job_state.sql`
- 모르는 내용은 지어내지 않는다.
- 확인 가능한 사실만 적는다.
- 실제 원문 경로를 함께 남긴다.
