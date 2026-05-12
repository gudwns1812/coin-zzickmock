CREATE TABLE wallet_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id VARCHAR(64) NOT NULL,
    wallet_balance DECIMAL(19, 4) NOT NULL,
    available_margin DECIMAL(19, 4) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    source_reference VARCHAR(255) NOT NULL,
    recorded_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_wallet_history PRIMARY KEY (id),
    CONSTRAINT uk_wallet_history_source UNIQUE (source_type, source_reference),
    CONSTRAINT fk_wallet_history_member
        FOREIGN KEY (member_id) REFERENCES trading_accounts (member_id)
);

CREATE INDEX idx_wallet_history_member_recorded_at
    ON wallet_history (member_id, recorded_at);
