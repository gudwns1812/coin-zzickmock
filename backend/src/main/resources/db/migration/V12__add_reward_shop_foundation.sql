ALTER TABLE member_credentials
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

UPDATE member_credentials
   SET role = 'ADMIN'
 WHERE member_id = 'test';

ALTER TABLE reward_point_wallets
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE reward_point_wallets
   SET reward_point = 0
 WHERE reward_point IS NULL;

ALTER TABLE reward_point_wallets
    MODIFY COLUMN reward_point INT NOT NULL;

ALTER TABLE reward_point_wallets
    ADD CONSTRAINT chk_reward_point_wallets_reward_point CHECK (reward_point >= 0);

CREATE TABLE reward_shop_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL,
    item_type VARCHAR(50) NOT NULL,
    price INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    total_stock INT NULL,
    sold_quantity INT NOT NULL DEFAULT 0,
    per_member_purchase_limit INT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_reward_shop_items PRIMARY KEY (id),
    CONSTRAINT uk_reward_shop_items_code UNIQUE (code),
    CONSTRAINT chk_reward_shop_items_price CHECK (price > 0),
    CONSTRAINT chk_reward_shop_items_stock CHECK (
        sold_quantity >= 0
        AND (total_stock IS NULL OR (total_stock >= 0 AND sold_quantity <= total_stock))
    ),
    CONSTRAINT chk_reward_shop_items_limit CHECK (
        per_member_purchase_limit IS NULL OR per_member_purchase_limit > 0
    )
);

CREATE INDEX idx_reward_shop_items_active_sort
    ON reward_shop_items (active, sort_order, code);

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
    'voucher.coffee',
    '커피 교환권',
    '관리자가 휴대폰 번호로 발송하는 커피 교환권',
    'COFFEE_VOUCHER',
    100,
    TRUE,
    100,
    0,
    1,
    10
);

CREATE TABLE reward_shop_member_item_usages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id VARCHAR(64) NOT NULL,
    shop_item_id BIGINT NOT NULL,
    purchase_count INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_reward_shop_member_item_usages PRIMARY KEY (id),
    CONSTRAINT uk_reward_shop_member_item_usage UNIQUE (member_id, shop_item_id),
    CONSTRAINT fk_reward_shop_member_item_usages_member
        FOREIGN KEY (member_id) REFERENCES member_credentials (member_id),
    CONSTRAINT fk_reward_shop_member_item_usages_item
        FOREIGN KEY (shop_item_id) REFERENCES reward_shop_items (id),
    CONSTRAINT chk_reward_shop_member_item_usages_count CHECK (purchase_count >= 0)
);

CREATE INDEX idx_reward_shop_member_item_usages_member
    ON reward_shop_member_item_usages (member_id);

CREATE TABLE reward_point_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id VARCHAR(64) NOT NULL,
    history_type VARCHAR(50) NOT NULL,
    amount INT NOT NULL,
    balance_after INT NOT NULL,
    source_type VARCHAR(50) NULL,
    source_reference VARCHAR(100) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_reward_point_histories PRIMARY KEY (id),
    CONSTRAINT fk_reward_point_histories_member
        FOREIGN KEY (member_id) REFERENCES member_credentials (member_id),
    CONSTRAINT chk_reward_point_histories_amount CHECK (amount <> 0),
    CONSTRAINT chk_reward_point_histories_balance CHECK (balance_after >= 0)
);

CREATE INDEX idx_reward_point_histories_member_created_at
    ON reward_point_histories (member_id, created_at);

CREATE TABLE reward_redemption_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    request_id VARCHAR(64) NOT NULL,
    member_id VARCHAR(64) NOT NULL,
    shop_item_id BIGINT NOT NULL,
    item_code VARCHAR(100) NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    item_price INT NOT NULL,
    point_amount INT NOT NULL,
    submitted_phone_number VARCHAR(30) NOT NULL,
    normalized_phone_number VARCHAR(11) NOT NULL,
    status VARCHAR(30) NOT NULL,
    requested_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    sent_at TIMESTAMP(6) NULL,
    cancelled_at TIMESTAMP(6) NULL,
    admin_member_id VARCHAR(64) NULL,
    admin_memo VARCHAR(500) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_reward_redemption_requests PRIMARY KEY (id),
    CONSTRAINT uk_reward_redemption_requests_request_id UNIQUE (request_id),
    CONSTRAINT fk_reward_redemption_requests_member
        FOREIGN KEY (member_id) REFERENCES member_credentials (member_id),
    CONSTRAINT fk_reward_redemption_requests_item
        FOREIGN KEY (shop_item_id) REFERENCES reward_shop_items (id),
    CONSTRAINT fk_reward_redemption_requests_admin
        FOREIGN KEY (admin_member_id) REFERENCES member_credentials (member_id),
    CONSTRAINT chk_reward_redemption_requests_amount CHECK (point_amount > 0),
    CONSTRAINT chk_reward_redemption_requests_phone CHECK (LENGTH(normalized_phone_number) BETWEEN 10 AND 11)
);

CREATE INDEX idx_reward_redemption_requests_member_created_at
    ON reward_redemption_requests (member_id, created_at);

CREATE INDEX idx_reward_redemption_requests_status_requested_at
    ON reward_redemption_requests (status, requested_at);
