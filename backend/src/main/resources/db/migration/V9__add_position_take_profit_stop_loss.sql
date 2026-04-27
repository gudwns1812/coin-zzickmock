ALTER TABLE open_positions
    ADD COLUMN take_profit_price DECIMAL(19, 4);

ALTER TABLE open_positions
    ADD COLUMN stop_loss_price DECIMAL(19, 4);
