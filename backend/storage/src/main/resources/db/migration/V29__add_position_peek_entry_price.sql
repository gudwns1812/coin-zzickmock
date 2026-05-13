ALTER TABLE position_peek_snapshot_positions
    ADD COLUMN entry_price DOUBLE NULL AFTER position_size;
