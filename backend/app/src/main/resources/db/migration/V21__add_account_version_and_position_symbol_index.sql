ALTER TABLE trading_accounts
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX idx_open_positions_symbol_member_side_margin
    ON open_positions (symbol, member_id, position_side, margin_mode);
