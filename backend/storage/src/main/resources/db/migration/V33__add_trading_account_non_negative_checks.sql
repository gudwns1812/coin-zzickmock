DROP TABLE IF EXISTS trading_account_non_negative_preflight;

CREATE TABLE trading_account_non_negative_preflight (
    negative_count BIGINT NOT NULL,
    CONSTRAINT chk_trading_account_non_negative_preflight CHECK (negative_count = 0)
);

INSERT INTO trading_account_non_negative_preflight (negative_count)
SELECT COUNT(*)
  FROM trading_accounts
 WHERE wallet_balance < 0
    OR available_margin < 0;

DROP TABLE trading_account_non_negative_preflight;

ALTER TABLE trading_accounts
    ADD CHECK (wallet_balance >= 0 AND available_margin >= 0);
