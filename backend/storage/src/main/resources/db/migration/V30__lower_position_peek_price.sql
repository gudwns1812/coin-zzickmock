UPDATE reward_shop_items
SET price = 10,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE code = 'position.peek';
