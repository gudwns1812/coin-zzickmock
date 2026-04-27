ALTER TABLE market_symbols
    ADD COLUMN funding_interval_hours INT NOT NULL DEFAULT 8;

ALTER TABLE market_symbols
    ADD COLUMN funding_anchor_hour_kst INT NOT NULL DEFAULT 1;

ALTER TABLE market_symbols
    ADD COLUMN funding_time_zone VARCHAR(40) NOT NULL DEFAULT 'Asia/Seoul';
