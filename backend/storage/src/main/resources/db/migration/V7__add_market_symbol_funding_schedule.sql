ALTER TABLE market_symbols
    ADD COLUMN funding_interval_hours INT NOT NULL DEFAULT 8 CHECK (funding_interval_hours > 0 AND funding_interval_hours <= 24);

ALTER TABLE market_symbols
    ADD COLUMN funding_anchor_hour INT NOT NULL DEFAULT 1 CHECK (funding_anchor_hour >= 0 AND funding_anchor_hour < 24);

ALTER TABLE market_symbols
    ADD COLUMN funding_time_zone VARCHAR(40) NOT NULL DEFAULT 'Asia/Seoul';
