UPDATE reward_shop_items
   SET description = '다음 KST 월요일 00:00 리셋 전까지 사용할 수 있는 지갑 리필 횟수 1회',
       updated_at = CURRENT_TIMESTAMP(6)
 WHERE code = 'account.refill-count'
   AND item_type = 'ACCOUNT_REFILL_COUNT';
