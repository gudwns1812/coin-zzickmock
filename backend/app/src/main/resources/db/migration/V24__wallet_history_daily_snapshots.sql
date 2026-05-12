-- Intentional destructive migration:
-- existing wallet_history rows are event-style mutation records, not valid KST daily snapshots.
-- They are discarded instead of collapsed so the account page does not show misleading daily history.
DROP TABLE IF EXISTS wallet_history;

CREATE TABLE wallet_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    snapshot_date DATE NOT NULL,
    baseline_wallet_balance DECIMAL(19, 4) NOT NULL,
    wallet_balance DECIMAL(19, 4) NOT NULL,
    daily_wallet_change DECIMAL(19, 4) NOT NULL DEFAULT 0,
    account_version BIGINT NOT NULL DEFAULT 0,
    recorded_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_wallet_history PRIMARY KEY (id),
    CONSTRAINT uk_wallet_history_member_snapshot_date UNIQUE (member_id, snapshot_date),
    CONSTRAINT fk_wallet_history_member
        FOREIGN KEY (member_id) REFERENCES trading_accounts (member_id)
);

CREATE INDEX idx_wallet_history_member_snapshot_date
    ON wallet_history (member_id, snapshot_date);
