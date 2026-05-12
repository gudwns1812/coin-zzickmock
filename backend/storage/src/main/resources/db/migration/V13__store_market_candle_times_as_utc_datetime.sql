ALTER TABLE market_candles_1m
    MODIFY COLUMN open_time DATETIME(6) NOT NULL;

ALTER TABLE market_candles_1m
    MODIFY COLUMN close_time DATETIME(6) NOT NULL;

ALTER TABLE market_candles_1m
    MODIFY COLUMN created_at DATETIME(6) NOT NULL;

ALTER TABLE market_candles_1m
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL;

ALTER TABLE market_candles_1h
    MODIFY COLUMN open_time DATETIME(6) NOT NULL;

ALTER TABLE market_candles_1h
    MODIFY COLUMN close_time DATETIME(6) NOT NULL;

ALTER TABLE market_candles_1h
    MODIFY COLUMN source_minute_open_time DATETIME(6) NOT NULL;

ALTER TABLE market_candles_1h
    MODIFY COLUMN source_minute_close_time DATETIME(6) NOT NULL;

ALTER TABLE market_candles_1h
    MODIFY COLUMN created_at DATETIME(6) NOT NULL;

ALTER TABLE market_candles_1h
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL;
