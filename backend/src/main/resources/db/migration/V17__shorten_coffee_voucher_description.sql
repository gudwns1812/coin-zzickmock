UPDATE reward_shop_items
   SET description = '커피 교환권'
 WHERE code = 'voucher.coffee'
   AND description = '관리자가 휴대폰 번호로 발송하는 커피 교환권';
