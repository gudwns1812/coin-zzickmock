CREATE TABLE market_symbols (
    id BIGINT NOT NULL AUTO_INCREMENT,
    symbol VARCHAR(30) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    base_asset VARCHAR(20) NOT NULL,
    quote_asset VARCHAR(20) NOT NULL DEFAULT 'USDT',
    price_scale TINYINT NOT NULL,
    quantity_scale TINYINT NOT NULL,
    price_step DECIMAL(19, 8) NOT NULL,
    quantity_step DECIMAL(19, 8) NOT NULL,
    max_leverage INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_market_symbols PRIMARY KEY (id),
    CONSTRAINT uk_market_symbols_symbol UNIQUE (symbol)
);

CREATE TABLE market_candles_1m (
    id BIGINT NOT NULL AUTO_INCREMENT,
    symbol_id BIGINT NOT NULL,
    open_time TIMESTAMP(6) NOT NULL,
    close_time TIMESTAMP(6) NOT NULL,
    open_price DECIMAL(19, 4) NOT NULL,
    high_price DECIMAL(19, 4) NOT NULL,
    low_price DECIMAL(19, 4) NOT NULL,
    close_price DECIMAL(19, 4) NOT NULL,
    volume DECIMAL(19, 8) NOT NULL,
    quote_volume DECIMAL(19, 4) NULL,
    trade_count INT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_market_candles_1m PRIMARY KEY (id),
    CONSTRAINT uk_market_candles_1m_symbol_open_time UNIQUE (symbol_id, open_time),
    CONSTRAINT fk_market_candles_1m_symbol
        FOREIGN KEY (symbol_id) REFERENCES market_symbols (id)
);

CREATE INDEX idx_market_candles_1m_open_time_symbol
    ON market_candles_1m (open_time, symbol_id);

CREATE TABLE market_candles_1h (
    id BIGINT NOT NULL AUTO_INCREMENT,
    symbol_id BIGINT NOT NULL,
    open_time TIMESTAMP(6) NOT NULL,
    close_time TIMESTAMP(6) NOT NULL,
    open_price DECIMAL(19, 4) NOT NULL,
    high_price DECIMAL(19, 4) NOT NULL,
    low_price DECIMAL(19, 4) NOT NULL,
    close_price DECIMAL(19, 4) NOT NULL,
    volume DECIMAL(19, 8) NOT NULL,
    quote_volume DECIMAL(19, 4) NULL,
    trade_count INT NULL,
    source_minute_open_time TIMESTAMP(6) NOT NULL,
    source_minute_close_time TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_market_candles_1h PRIMARY KEY (id),
    CONSTRAINT uk_market_candles_1h_symbol_open_time UNIQUE (symbol_id, open_time),
    CONSTRAINT fk_market_candles_1h_symbol
        FOREIGN KEY (symbol_id) REFERENCES market_symbols (id)
);

CREATE INDEX idx_market_candles_1h_open_time_symbol
    ON market_candles_1h (open_time, symbol_id);

INSERT INTO market_symbols (
    symbol,
    display_name,
    base_asset,
    quote_asset,
    price_scale,
    quantity_scale,
    price_step,
    quantity_step,
    max_leverage,
    active
)
VALUES
    ('BTCUSDT', 'Bitcoin Perpetual', 'BTC', 'USDT', 1, 4, 0.1, 0.0001, 50, TRUE),
    ('ETHUSDT', 'Ethereum Perpetual', 'ETH', 'USDT', 2, 3, 0.01, 0.001, 50, TRUE);
