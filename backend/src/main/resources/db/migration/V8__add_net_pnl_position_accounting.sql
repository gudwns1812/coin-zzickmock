ALTER TABLE open_positions
    ADD COLUMN accumulated_open_fee DECIMAL(19, 4) NOT NULL DEFAULT 0;

ALTER TABLE open_positions
    ADD COLUMN accumulated_funding_cost DECIMAL(19, 4) NOT NULL DEFAULT 0;

ALTER TABLE position_history
    ADD COLUMN gross_realized_pnl DECIMAL(19, 4) NOT NULL DEFAULT 0;

ALTER TABLE position_history
    ADD COLUMN open_fee DECIMAL(19, 4) NOT NULL DEFAULT 0;

ALTER TABLE position_history
    ADD COLUMN close_fee DECIMAL(19, 4) NOT NULL DEFAULT 0;

ALTER TABLE position_history
    ADD COLUMN total_fee DECIMAL(19, 4) NOT NULL DEFAULT 0;

ALTER TABLE position_history
    ADD COLUMN funding_cost DECIMAL(19, 4) NOT NULL DEFAULT 0;

ALTER TABLE position_history
    ADD COLUMN net_realized_pnl DECIMAL(19, 4) NOT NULL DEFAULT 0;

UPDATE position_history
   SET gross_realized_pnl = realized_pnl,
       net_realized_pnl = realized_pnl;
