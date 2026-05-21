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
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_market_completed_candles PRIMARY KEY (id),
    CONSTRAINT uk_market_completed_candles_symbol_interval_open_time UNIQUE (symbol_id, candle_interval, open_time),
    CONSTRAINT fk_market_completed_candles_symbol
        FOREIGN KEY (symbol_id) REFERENCES market_symbols (id),
    CONSTRAINT chk_market_completed_candles_interval
        CHECK (candle_interval IN ('ONE_HOUR', 'ONE_DAY', 'ONE_MONTH'))
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
    created_at,
    updated_at
FROM market_candles_1h;

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
    created_at,
    updated_at
)
SELECT
    aggregated.symbol_id,
    'ONE_DAY',
    aggregated.open_time,
    aggregated.close_time,
    opening.open_price,
    aggregated.high_price,
    aggregated.low_price,
    closing.close_price,
    aggregated.volume,
    aggregated.quote_volume,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
FROM (
    SELECT
        symbol_id,
        DATE(open_time) AS open_time,
        TIMESTAMPADD(DAY, 1, DATE(open_time)) AS close_time,
        MIN(open_time) AS opening_open_time,
        MAX(open_time) AS closing_open_time,
        MAX(high_price) AS high_price,
        MIN(low_price) AS low_price,
        SUM(volume) AS volume,
        SUM(COALESCE(quote_volume, 0)) AS quote_volume
    FROM market_completed_candles
    WHERE candle_interval = 'ONE_HOUR'
    GROUP BY symbol_id, DATE(open_time), TIMESTAMPADD(DAY, 1, DATE(open_time))
    HAVING close_time <= CURRENT_TIMESTAMP(6)
) aggregated
JOIN market_completed_candles opening
    ON opening.symbol_id = aggregated.symbol_id
    AND opening.candle_interval = 'ONE_HOUR'
    AND opening.open_time = aggregated.opening_open_time
JOIN market_completed_candles closing
    ON closing.symbol_id = aggregated.symbol_id
    AND closing.candle_interval = 'ONE_HOUR'
    AND closing.open_time = aggregated.closing_open_time;

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
    created_at,
    updated_at
)
SELECT
    aggregated.symbol_id,
    'ONE_MONTH',
    aggregated.open_time,
    aggregated.close_time,
    opening.open_price,
    aggregated.high_price,
    aggregated.low_price,
    closing.close_price,
    aggregated.volume,
    aggregated.quote_volume,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
FROM (
    SELECT
        symbol_id,
        TIMESTAMPADD(MONTH, TIMESTAMPDIFF(MONTH, '1970-01-01 00:00:00', open_time), '1970-01-01 00:00:00') AS open_time,
        TIMESTAMPADD(MONTH, 1, TIMESTAMPADD(MONTH, TIMESTAMPDIFF(MONTH, '1970-01-01 00:00:00', open_time), '1970-01-01 00:00:00')) AS close_time,
        MIN(open_time) AS opening_open_time,
        MAX(open_time) AS closing_open_time,
        MAX(high_price) AS high_price,
        MIN(low_price) AS low_price,
        SUM(volume) AS volume,
        SUM(COALESCE(quote_volume, 0)) AS quote_volume
    FROM market_completed_candles
    WHERE candle_interval = 'ONE_HOUR'
    GROUP BY
        symbol_id,
        TIMESTAMPADD(MONTH, TIMESTAMPDIFF(MONTH, '1970-01-01 00:00:00', open_time), '1970-01-01 00:00:00'),
        TIMESTAMPADD(MONTH, 1, TIMESTAMPADD(MONTH, TIMESTAMPDIFF(MONTH, '1970-01-01 00:00:00', open_time), '1970-01-01 00:00:00'))
    HAVING close_time <= CURRENT_TIMESTAMP(6)
) aggregated
JOIN market_completed_candles opening
    ON opening.symbol_id = aggregated.symbol_id
    AND opening.candle_interval = 'ONE_HOUR'
    AND opening.open_time = aggregated.opening_open_time
JOIN market_completed_candles closing
    ON closing.symbol_id = aggregated.symbol_id
    AND closing.candle_interval = 'ONE_HOUR'
    AND closing.open_time = aggregated.closing_open_time;
