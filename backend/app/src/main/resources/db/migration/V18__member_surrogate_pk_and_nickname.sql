CREATE TABLE member_reference_orphan_preflight (
    table_name VARCHAR(100) NOT NULL,
    column_name VARCHAR(100) NOT NULL,
    orphan_count BIGINT NOT NULL,
    CONSTRAINT chk_member_reference_orphan_preflight CHECK (orphan_count = 0)
);

INSERT INTO member_reference_orphan_preflight (table_name, column_name, orphan_count)
SELECT 'trading_accounts', 'member_id', COUNT(*)
  FROM trading_accounts ref
  LEFT JOIN member_credentials member ON member.member_id = ref.member_id
 WHERE member.member_id IS NULL;

INSERT INTO member_reference_orphan_preflight (table_name, column_name, orphan_count)
SELECT 'reward_point_wallets', 'member_id', COUNT(*)
  FROM reward_point_wallets ref
  LEFT JOIN member_credentials member ON member.member_id = ref.member_id
 WHERE member.member_id IS NULL;

INSERT INTO member_reference_orphan_preflight (table_name, column_name, orphan_count)
SELECT 'futures_orders', 'member_id', COUNT(*)
  FROM futures_orders ref
  LEFT JOIN member_credentials member ON member.member_id = ref.member_id
 WHERE member.member_id IS NULL;

INSERT INTO member_reference_orphan_preflight (table_name, column_name, orphan_count)
SELECT 'open_positions', 'member_id', COUNT(*)
  FROM open_positions ref
  LEFT JOIN member_credentials member ON member.member_id = ref.member_id
 WHERE member.member_id IS NULL;

INSERT INTO member_reference_orphan_preflight (table_name, column_name, orphan_count)
SELECT 'position_history', 'member_id', COUNT(*)
  FROM position_history ref
  LEFT JOIN member_credentials member ON member.member_id = ref.member_id
 WHERE member.member_id IS NULL;

INSERT INTO member_reference_orphan_preflight (table_name, column_name, orphan_count)
SELECT 'reward_shop_member_item_usages', 'member_id', COUNT(*)
  FROM reward_shop_member_item_usages ref
  LEFT JOIN member_credentials member ON member.member_id = ref.member_id
 WHERE member.member_id IS NULL;

INSERT INTO member_reference_orphan_preflight (table_name, column_name, orphan_count)
SELECT 'reward_point_histories', 'member_id', COUNT(*)
  FROM reward_point_histories ref
  LEFT JOIN member_credentials member ON member.member_id = ref.member_id
 WHERE member.member_id IS NULL;

INSERT INTO member_reference_orphan_preflight (table_name, column_name, orphan_count)
SELECT 'reward_redemption_requests', 'member_id', COUNT(*)
  FROM reward_redemption_requests ref
  LEFT JOIN member_credentials member ON member.member_id = ref.member_id
 WHERE member.member_id IS NULL;

INSERT INTO member_reference_orphan_preflight (table_name, column_name, orphan_count)
SELECT 'reward_redemption_requests', 'admin_member_id', COUNT(*)
  FROM reward_redemption_requests ref
  LEFT JOIN member_credentials member ON member.member_id = ref.admin_member_id
 WHERE ref.admin_member_id IS NOT NULL
   AND member.member_id IS NULL;

INSERT INTO member_reference_orphan_preflight (table_name, column_name, orphan_count)
SELECT 'wallet_history', 'member_id', COUNT(*)
  FROM wallet_history ref
  LEFT JOIN member_credentials member ON member.member_id = ref.member_id
 WHERE member.member_id IS NULL;

DROP TABLE member_reference_orphan_preflight;

CREATE TABLE member_surrogate_id_map AS
SELECT member_id AS account,
       ROW_NUMBER() OVER (ORDER BY member_id) AS id
  FROM member_credentials;

ALTER TABLE member_credentials DROP FOREIGN KEY fk_member_credentials_account;
ALTER TABLE reward_point_wallets DROP FOREIGN KEY fk_reward_point_wallets_member;
ALTER TABLE futures_orders DROP FOREIGN KEY fk_futures_orders_member;
ALTER TABLE open_positions DROP FOREIGN KEY fk_open_positions_member;
ALTER TABLE position_history DROP FOREIGN KEY fk_position_history_member;
ALTER TABLE reward_shop_member_item_usages DROP FOREIGN KEY fk_reward_shop_member_item_usages_member;
ALTER TABLE reward_point_histories DROP FOREIGN KEY fk_reward_point_histories_member;
ALTER TABLE reward_redemption_requests DROP FOREIGN KEY fk_reward_redemption_requests_member;
ALTER TABLE reward_redemption_requests DROP FOREIGN KEY fk_reward_redemption_requests_admin;
ALTER TABLE wallet_history DROP FOREIGN KEY fk_wallet_history_member;

UPDATE trading_accounts
   SET member_id = (SELECT id FROM member_surrogate_id_map WHERE account = trading_accounts.member_id);

UPDATE reward_point_wallets
   SET member_id = (SELECT id FROM member_surrogate_id_map WHERE account = reward_point_wallets.member_id);

UPDATE futures_orders
   SET member_id = (SELECT id FROM member_surrogate_id_map WHERE account = futures_orders.member_id);

UPDATE open_positions
   SET member_id = (SELECT id FROM member_surrogate_id_map WHERE account = open_positions.member_id);

UPDATE position_history
   SET member_id = (SELECT id FROM member_surrogate_id_map WHERE account = position_history.member_id);

UPDATE reward_shop_member_item_usages
   SET member_id = (SELECT id FROM member_surrogate_id_map WHERE account = reward_shop_member_item_usages.member_id);

UPDATE reward_point_histories
   SET member_id = (SELECT id FROM member_surrogate_id_map WHERE account = reward_point_histories.member_id);

UPDATE reward_redemption_requests
   SET member_id = (SELECT id FROM member_surrogate_id_map WHERE account = reward_redemption_requests.member_id);

UPDATE reward_redemption_requests
   SET admin_member_id = (SELECT id FROM member_surrogate_id_map WHERE account = reward_redemption_requests.admin_member_id)
 WHERE admin_member_id IS NOT NULL;

UPDATE wallet_history
   SET member_id = (SELECT id FROM member_surrogate_id_map WHERE account = wallet_history.member_id);

ALTER TABLE member_credentials DROP PRIMARY KEY;
ALTER TABLE member_credentials RENAME COLUMN member_id TO account;
ALTER TABLE member_credentials ADD COLUMN id BIGINT;
UPDATE member_credentials
   SET id = (SELECT id FROM member_surrogate_id_map WHERE account = member_credentials.account);
ALTER TABLE member_credentials MODIFY COLUMN id BIGINT NOT NULL;
ALTER TABLE member_credentials ADD CONSTRAINT pk_member_credentials PRIMARY KEY (id);
ALTER TABLE member_credentials MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE member_credentials ADD CONSTRAINT uk_member_credentials_account UNIQUE (account);
ALTER TABLE member_credentials ADD COLUMN nickname VARCHAR(100);
UPDATE member_credentials
   SET nickname = member_name;
ALTER TABLE member_credentials MODIFY COLUMN nickname VARCHAR(100) NOT NULL;

ALTER TABLE trading_accounts MODIFY COLUMN member_id BIGINT NOT NULL;
ALTER TABLE reward_point_wallets MODIFY COLUMN member_id BIGINT NOT NULL;
ALTER TABLE futures_orders MODIFY COLUMN member_id BIGINT NOT NULL;
ALTER TABLE open_positions MODIFY COLUMN member_id BIGINT NOT NULL;
ALTER TABLE position_history MODIFY COLUMN member_id BIGINT NOT NULL;
ALTER TABLE reward_shop_member_item_usages MODIFY COLUMN member_id BIGINT NOT NULL;
ALTER TABLE reward_point_histories MODIFY COLUMN member_id BIGINT NOT NULL;
ALTER TABLE reward_redemption_requests MODIFY COLUMN member_id BIGINT NOT NULL;
ALTER TABLE reward_redemption_requests MODIFY COLUMN admin_member_id BIGINT NULL;
ALTER TABLE wallet_history MODIFY COLUMN member_id BIGINT NOT NULL;

ALTER TABLE trading_accounts
    ADD CONSTRAINT fk_trading_accounts_member
        FOREIGN KEY (member_id) REFERENCES member_credentials (id);

ALTER TABLE reward_point_wallets
    ADD CONSTRAINT fk_reward_point_wallets_member
        FOREIGN KEY (member_id) REFERENCES member_credentials (id);

ALTER TABLE futures_orders
    ADD CONSTRAINT fk_futures_orders_member
        FOREIGN KEY (member_id) REFERENCES trading_accounts (member_id);

ALTER TABLE open_positions
    ADD CONSTRAINT fk_open_positions_member
        FOREIGN KEY (member_id) REFERENCES trading_accounts (member_id);

ALTER TABLE position_history
    ADD CONSTRAINT fk_position_history_member
        FOREIGN KEY (member_id) REFERENCES trading_accounts (member_id);

ALTER TABLE reward_shop_member_item_usages
    ADD CONSTRAINT fk_reward_shop_member_item_usages_member
        FOREIGN KEY (member_id) REFERENCES member_credentials (id);

ALTER TABLE reward_point_histories
    ADD CONSTRAINT fk_reward_point_histories_member
        FOREIGN KEY (member_id) REFERENCES member_credentials (id);

ALTER TABLE reward_redemption_requests
    ADD CONSTRAINT fk_reward_redemption_requests_member
        FOREIGN KEY (member_id) REFERENCES member_credentials (id);

ALTER TABLE reward_redemption_requests
    ADD CONSTRAINT fk_reward_redemption_requests_admin
        FOREIGN KEY (admin_member_id) REFERENCES member_credentials (id);

ALTER TABLE wallet_history
    ADD CONSTRAINT fk_wallet_history_member
        FOREIGN KEY (member_id) REFERENCES trading_accounts (member_id);

DROP TABLE member_surrogate_id_map;
