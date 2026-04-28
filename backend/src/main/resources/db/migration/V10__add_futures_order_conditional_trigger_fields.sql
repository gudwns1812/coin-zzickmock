ALTER TABLE futures_orders
    ADD COLUMN trigger_price DECIMAL(19, 4);

ALTER TABLE futures_orders
    ADD COLUMN trigger_type VARCHAR(30);

ALTER TABLE futures_orders
    ADD COLUMN trigger_source VARCHAR(30);

ALTER TABLE futures_orders
    ADD COLUMN oco_group_id VARCHAR(64);

CREATE INDEX idx_futures_orders_pending_conditional_symbol
    ON futures_orders (symbol, status, trigger_type);

CREATE INDEX idx_futures_orders_oco_group
    ON futures_orders (member_id, symbol, position_side, margin_mode, oco_group_id);
