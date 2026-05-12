ALTER TABLE futures_orders
    ADD COLUMN active_conditional_trigger_type VARCHAR(30);

UPDATE futures_orders
   SET active_conditional_trigger_type = trigger_type
 WHERE status = 'PENDING'
   AND order_purpose = 'CLOSE_POSITION'
   AND trigger_type IS NOT NULL;

INSERT INTO futures_orders (
    order_id,
    member_id,
    symbol,
    position_side,
    order_type,
    order_purpose,
    margin_mode,
    leverage,
    quantity,
    limit_price,
    status,
    fee_type,
    estimated_fee,
    execution_price,
    trigger_price,
    trigger_type,
    trigger_source,
    oco_group_id,
    active_conditional_trigger_type
)
SELECT CONCAT('legacy-tp-', id),
       member_id,
       symbol,
       position_side,
       'MARKET',
       'CLOSE_POSITION',
       margin_mode,
       leverage,
       quantity,
       NULL,
       'PENDING',
       'TAKER',
       0,
       0,
       take_profit_price,
       'TAKE_PROFIT',
       'MARK_PRICE',
       CASE
           WHEN stop_loss_price IS NOT NULL THEN CONCAT('legacy-tpsl-', id)
           ELSE NULL
       END,
       'TAKE_PROFIT'
  FROM open_positions
 WHERE take_profit_price IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
         FROM futures_orders existing_order
        WHERE existing_order.member_id = open_positions.member_id
          AND existing_order.symbol = open_positions.symbol
          AND existing_order.position_side = open_positions.position_side
          AND existing_order.margin_mode = open_positions.margin_mode
          AND existing_order.status = 'PENDING'
          AND existing_order.order_purpose = 'CLOSE_POSITION'
          AND existing_order.trigger_type = 'TAKE_PROFIT'
   );

INSERT INTO futures_orders (
    order_id,
    member_id,
    symbol,
    position_side,
    order_type,
    order_purpose,
    margin_mode,
    leverage,
    quantity,
    limit_price,
    status,
    fee_type,
    estimated_fee,
    execution_price,
    trigger_price,
    trigger_type,
    trigger_source,
    oco_group_id,
    active_conditional_trigger_type
)
SELECT CONCAT('legacy-sl-', id),
       member_id,
       symbol,
       position_side,
       'MARKET',
       'CLOSE_POSITION',
       margin_mode,
       leverage,
       quantity,
       NULL,
       'PENDING',
       'TAKER',
       0,
       0,
       stop_loss_price,
       'STOP_LOSS',
       'MARK_PRICE',
       CASE
           WHEN take_profit_price IS NOT NULL THEN CONCAT('legacy-tpsl-', id)
           ELSE NULL
       END,
       'STOP_LOSS'
  FROM open_positions
 WHERE stop_loss_price IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
         FROM futures_orders existing_order
        WHERE existing_order.member_id = open_positions.member_id
          AND existing_order.symbol = open_positions.symbol
          AND existing_order.position_side = open_positions.position_side
          AND existing_order.margin_mode = open_positions.margin_mode
          AND existing_order.status = 'PENDING'
          AND existing_order.order_purpose = 'CLOSE_POSITION'
          AND existing_order.trigger_type = 'STOP_LOSS'
   );

CREATE UNIQUE INDEX uk_futures_orders_active_conditional_close
    ON futures_orders (
        member_id,
        symbol,
        position_side,
        margin_mode,
        active_conditional_trigger_type
    );
