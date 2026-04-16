CREATE TABLE trading_accounts (
    member_id VARCHAR(64) NOT NULL,
    member_email VARCHAR(255) NOT NULL,
    member_name VARCHAR(100) NOT NULL,
    wallet_balance DECIMAL(19, 4) NOT NULL,
    available_margin DECIMAL(19, 4) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_trading_accounts PRIMARY KEY (member_id)
);

CREATE TABLE reward_point_wallets (
    member_id VARCHAR(64) NOT NULL,
    reward_point DECIMAL(19, 2) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_reward_point_wallets PRIMARY KEY (member_id),
    CONSTRAINT fk_reward_point_wallets_member
        FOREIGN KEY (member_id) REFERENCES trading_accounts (member_id)
);

CREATE TABLE futures_orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id VARCHAR(64) NOT NULL,
    member_id VARCHAR(64) NOT NULL,
    symbol VARCHAR(30) NOT NULL,
    position_side VARCHAR(20) NOT NULL,
    order_type VARCHAR(20) NOT NULL,
    margin_mode VARCHAR(20) NOT NULL,
    leverage INT NOT NULL,
    quantity DECIMAL(19, 8) NOT NULL,
    limit_price DECIMAL(19, 4) NULL,
    status VARCHAR(20) NOT NULL,
    fee_type VARCHAR(20) NOT NULL,
    estimated_fee DECIMAL(19, 4) NOT NULL,
    execution_price DECIMAL(19, 4) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_futures_orders PRIMARY KEY (id),
    CONSTRAINT uk_futures_orders_order_id UNIQUE (order_id),
    CONSTRAINT fk_futures_orders_member
        FOREIGN KEY (member_id) REFERENCES trading_accounts (member_id)
);

CREATE INDEX idx_futures_orders_member_created_at
    ON futures_orders (member_id, created_at);

CREATE TABLE open_positions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id VARCHAR(64) NOT NULL,
    symbol VARCHAR(30) NOT NULL,
    position_side VARCHAR(20) NOT NULL,
    margin_mode VARCHAR(20) NOT NULL,
    leverage INT NOT NULL,
    quantity DECIMAL(19, 8) NOT NULL,
    entry_price DECIMAL(19, 4) NOT NULL,
    mark_price DECIMAL(19, 4) NOT NULL,
    liquidation_price DECIMAL(19, 4) NULL,
    unrealized_pnl DECIMAL(19, 4) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_open_positions PRIMARY KEY (id),
    CONSTRAINT uk_open_position_member_symbol_side_mode
        UNIQUE (member_id, symbol, position_side, margin_mode),
    CONSTRAINT fk_open_positions_member
        FOREIGN KEY (member_id) REFERENCES trading_accounts (member_id)
);
