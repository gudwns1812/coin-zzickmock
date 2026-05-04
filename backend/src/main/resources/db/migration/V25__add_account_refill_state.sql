CREATE TABLE account_refill_states (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    refill_date DATE NOT NULL,
    remaining_count INT NOT NULL DEFAULT 1,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_account_refill_states PRIMARY KEY (id),
    CONSTRAINT uk_account_refill_states_member_date UNIQUE (member_id, refill_date),
    CONSTRAINT fk_account_refill_states_member
        FOREIGN KEY (member_id) REFERENCES member_credentials (id),
    CONSTRAINT chk_account_refill_states_remaining CHECK (remaining_count >= 0)
);

CREATE INDEX idx_account_refill_states_member_date
    ON account_refill_states (member_id, refill_date);

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
    'account.refill-count',
    '리필 횟수 추가권',
    '오늘 자정 전까지 사용할 수 있는 지갑 리필 횟수 1회',
    'ACCOUNT_REFILL_COUNT',
    20,
    TRUE,
    NULL,
    0,
    NULL,
    20
);
