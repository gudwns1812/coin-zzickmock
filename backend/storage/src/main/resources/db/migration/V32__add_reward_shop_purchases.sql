CREATE TABLE reward_shop_purchases (
    id BIGINT NOT NULL AUTO_INCREMENT,
    purchase_id VARCHAR(64) NOT NULL,
    member_id BIGINT NOT NULL,
    shop_item_id BIGINT NOT NULL,
    item_code VARCHAR(100) NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    item_type VARCHAR(50) NOT NULL,
    item_price INT NOT NULL,
    point_amount INT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    purchased_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_reward_shop_purchases PRIMARY KEY (id),
    CONSTRAINT uk_reward_shop_purchases_purchase_id UNIQUE (purchase_id),
    CONSTRAINT fk_reward_shop_purchases_member
        FOREIGN KEY (member_id) REFERENCES member_credentials (id),
    CONSTRAINT fk_reward_shop_purchases_item
        FOREIGN KEY (shop_item_id) REFERENCES reward_shop_items (id),
    CONSTRAINT chk_reward_shop_purchases_amount CHECK (item_price > 0 AND point_amount > 0),
    CONSTRAINT chk_reward_shop_purchases_quantity CHECK (quantity = 1),
    CONSTRAINT chk_reward_shop_purchases_item_type CHECK (
        item_type IN ('ACCOUNT_REFILL_COUNT', 'POSITION_PEEK')
    )
);

CREATE INDEX idx_reward_shop_purchases_member_purchased
    ON reward_shop_purchases (member_id, purchased_at, id);
