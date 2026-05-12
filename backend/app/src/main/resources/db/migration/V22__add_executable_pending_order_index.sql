CREATE INDEX idx_futures_orders_pending_limit_symbol_price
    ON futures_orders (symbol, status, limit_price, order_purpose, position_side);
