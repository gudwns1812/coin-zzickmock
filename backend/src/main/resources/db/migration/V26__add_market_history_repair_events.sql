CREATE TABLE market_history_repair_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    symbol VARCHAR(32) NOT NULL,
    candle_interval VARCHAR(16) NOT NULL,
    open_time DATETIME(6) NOT NULL,
    close_time DATETIME(6) NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    last_error VARCHAR(1024),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_market_history_repair_events PRIMARY KEY (id),
    CONSTRAINT uk_market_history_repair_events_identity UNIQUE (symbol, candle_interval, open_time)
);

CREATE INDEX idx_market_history_repair_events_status_updated
    ON market_history_repair_events (status, updated_at);
