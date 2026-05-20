CREATE TABLE market_completed_candles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    symbol_id BIGINT NOT NULL,
    candle_interval VARCHAR(30) NOT NULL,
    open_time DATETIME(6) NOT NULL,
    close_time DATETIME(6) NOT NULL,
    open_price DECIMAL(19, 4) NOT NULL,
    high_price DECIMAL(19, 4) NOT NULL,
    low_price DECIMAL(19, 4) NOT NULL,
    close_price DECIMAL(19, 4) NOT NULL,
    volume DECIMAL(19, 8) NOT NULL,
    quote_volume DECIMAL(19, 4) NULL,
    source_interval VARCHAR(30) NOT NULL,
    source_open_time DATETIME(6) NOT NULL,
    source_close_time DATETIME(6) NOT NULL,
    source_candle_count INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_market_completed_candles PRIMARY KEY (id),
    CONSTRAINT uk_market_completed_candles_symbol_interval_open_time UNIQUE (symbol_id, candle_interval, open_time),
    CONSTRAINT fk_market_completed_candles_symbol
        FOREIGN KEY (symbol_id) REFERENCES market_symbols (id),
    CONSTRAINT chk_market_completed_candles_interval
        CHECK (candle_interval IN ('ONE_HOUR', 'ONE_DAY', 'ONE_MONTH')),
    CONSTRAINT chk_market_completed_candles_source_interval
        CHECK (source_interval IN ('ONE_MINUTE', 'ONE_HOUR')),
    CONSTRAINT chk_market_completed_candles_source_count
        CHECK (source_candle_count > 0)
);

INSERT INTO market_completed_candles (
    symbol_id,
    candle_interval,
    open_time,
    close_time,
    open_price,
    high_price,
    low_price,
    close_price,
    volume,
    quote_volume,
    source_interval,
    source_open_time,
    source_close_time,
    source_candle_count,
    created_at,
    updated_at
)
SELECT
    symbol_id,
    'ONE_HOUR',
    open_time,
    close_time,
    open_price,
    high_price,
    low_price,
    close_price,
    volume,
    quote_volume,
    'ONE_MINUTE',
    source_minute_open_time,
    source_minute_close_time,
    60,
    created_at,
    updated_at
FROM market_candles_1h;
