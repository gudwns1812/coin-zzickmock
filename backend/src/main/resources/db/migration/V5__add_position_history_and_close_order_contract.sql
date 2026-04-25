ALTER TABLE open_positions
    ADD COLUMN opened_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

ALTER TABLE open_positions
    ADD COLUMN original_quantity DECIMAL(19, 8) NOT NULL DEFAULT 0;

ALTER TABLE open_positions
    ADD COLUMN accumulated_closed_quantity DECIMAL(19, 8) NOT NULL DEFAULT 0;

ALTER TABLE open_positions
    ADD COLUMN accumulated_exit_notional DECIMAL(19, 4) NOT NULL DEFAULT 0;

ALTER TABLE open_positions
    ADD COLUMN accumulated_realized_pnl DECIMAL(19, 4) NOT NULL DEFAULT 0;

ALTER TABLE open_positions
    ADD COLUMN accumulated_close_fee DECIMAL(19, 4) NOT NULL DEFAULT 0;

UPDATE open_positions
   SET opened_at = created_at,
       original_quantity = quantity
 WHERE original_quantity = 0;

ALTER TABLE futures_orders
    ADD COLUMN order_purpose VARCHAR(30) NOT NULL DEFAULT 'OPEN_POSITION';

CREATE TABLE position_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id VARCHAR(64) NOT NULL,
    symbol VARCHAR(30) NOT NULL,
    position_side VARCHAR(20) NOT NULL,
    margin_mode VARCHAR(20) NOT NULL,
    leverage INT NOT NULL,
    opened_at TIMESTAMP(6) NOT NULL,
    average_entry_price DECIMAL(19, 4) NOT NULL,
    average_exit_price DECIMAL(19, 4) NOT NULL,
    position_size DECIMAL(19, 8) NOT NULL,
    realized_pnl DECIMAL(19, 4) NOT NULL,
    roi DECIMAL(19, 8) NOT NULL,
    closed_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    close_reason VARCHAR(30) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_position_history PRIMARY KEY (id),
    CONSTRAINT fk_position_history_member
        FOREIGN KEY (member_id) REFERENCES trading_accounts (member_id)
);

CREATE INDEX idx_position_history_member_closed_at
    ON position_history (member_id, closed_at);

CREATE INDEX idx_position_history_member_symbol_closed_at
    ON position_history (member_id, symbol, closed_at);
