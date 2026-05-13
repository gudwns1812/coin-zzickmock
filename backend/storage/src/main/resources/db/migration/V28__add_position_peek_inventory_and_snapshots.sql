INSERT INTO reward_shop_items (
    code,
    name,
    description,
    item_type,
    price,
    active,
    total_stock,
    sold_quantity,
    per_member_purchase_limit,
    sort_order
)
VALUES (
    'position.peek',
    '포지션 엿보기권',
    '리더보드에서 선택한 사용자 1명의 현재 포지션 공개 요약을 1회 스냅샷으로 저장합니다.',
    'POSITION_PEEK',
    30,
    TRUE,
    NULL,
    0,
    NULL,
    30
);

CREATE TABLE reward_item_balances (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    shop_item_id BIGINT NOT NULL,
    remaining_quantity INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_reward_item_balances PRIMARY KEY (id),
    CONSTRAINT uk_reward_item_balances_member_item UNIQUE (member_id, shop_item_id),
    CONSTRAINT fk_reward_item_balances_member
        FOREIGN KEY (member_id) REFERENCES member_credentials (id),
    CONSTRAINT fk_reward_item_balances_item
        FOREIGN KEY (shop_item_id) REFERENCES reward_shop_items (id),
    CONSTRAINT chk_reward_item_balances_quantity CHECK (remaining_quantity >= 0)
);

CREATE INDEX idx_reward_item_balances_member
    ON reward_item_balances (member_id);

CREATE TABLE position_peek_snapshots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    peek_id VARCHAR(36) NOT NULL,
    viewer_member_id BIGINT NOT NULL,
    target_member_id BIGINT NOT NULL,
    target_token_fingerprint VARCHAR(128) NOT NULL,
    target_display_name_snapshot VARCHAR(100) NOT NULL,
    discovery_source VARCHAR(50) NULL,
    rank_at_use INT NULL,
    leaderboard_mode_at_use VARCHAR(30) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_position_peek_snapshots PRIMARY KEY (id),
    CONSTRAINT uk_position_peek_snapshots_peek_id UNIQUE (peek_id),
    CONSTRAINT fk_position_peek_snapshots_viewer
        FOREIGN KEY (viewer_member_id) REFERENCES member_credentials (id),
    CONSTRAINT fk_position_peek_snapshots_target
        FOREIGN KEY (target_member_id) REFERENCES member_credentials (id),
    CONSTRAINT chk_position_peek_snapshots_rank CHECK (rank_at_use IS NULL OR rank_at_use > 0)
);

CREATE INDEX idx_position_peek_snapshots_viewer_created_at
    ON position_peek_snapshots (viewer_member_id, created_at);

CREATE INDEX idx_position_peek_snapshots_viewer_target_created_at
    ON position_peek_snapshots (viewer_member_id, target_member_id, created_at);

CREATE TABLE position_peek_snapshot_positions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    peek_snapshot_id BIGINT NOT NULL,
    symbol VARCHAR(30) NOT NULL,
    position_side VARCHAR(20) NOT NULL,
    leverage INT NOT NULL,
    position_size DOUBLE NOT NULL,
    notional_value DOUBLE NOT NULL,
    unrealized_pnl DOUBLE NOT NULL,
    roi DOUBLE NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_position_peek_snapshot_positions PRIMARY KEY (id),
    CONSTRAINT fk_position_peek_snapshot_positions_snapshot
        FOREIGN KEY (peek_snapshot_id) REFERENCES position_peek_snapshots (id),
    CONSTRAINT chk_position_peek_snapshot_positions_leverage CHECK (leverage > 0),
    CONSTRAINT chk_position_peek_snapshot_positions_size CHECK (position_size > 0),
    CONSTRAINT chk_position_peek_snapshot_positions_notional CHECK (notional_value >= 0)
);

CREATE INDEX idx_position_peek_snapshot_positions_snapshot
    ON position_peek_snapshot_positions (peek_snapshot_id);
