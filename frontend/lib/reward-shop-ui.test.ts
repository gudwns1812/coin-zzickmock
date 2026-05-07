import assert from "node:assert/strict";
import test from "node:test";

const rewardShopModule: typeof import("./reward-shop-ui") = await import(
  new URL("./reward-shop-ui.ts", import.meta.url).href
);

const {
  getShopItemImagePath,
  getShopItemAvailabilityLabel,
  isAccountRefillShopItem,
  isShopItemLimitReached,
  isShopItemSoldOut,
  normalizeVoucherPhoneNumber,
  validateVoucherPhoneNumber,
} = rewardShopModule;

test("validates and normalizes voucher phone numbers", () => {
  assert.equal(validateVoucherPhoneNumber("010-1234-5678"), null);
  assert.equal(normalizeVoucherPhoneNumber("010-1234-5678"), "01012345678");
  assert.equal(validateVoucherPhoneNumber("0101234567"), null);
  assert.equal(validateVoucherPhoneNumber("010 1234 5678"), "숫자와 하이픈만 입력할 수 있습니다.");
  assert.equal(validateVoucherPhoneNumber("010-123"), "휴대폰 번호는 숫자 10~11자리여야 합니다.");
});

test("derives sold-out and per-member limit item states", () => {
  const baseItem = {
    code: "voucher.coffee",
    name: "커피 교환권",
    description: "커피",
    itemType: "COFFEE_VOUCHER",
    price: 50,
    active: true,
    totalStock: 10,
    soldQuantity: 10,
    remainingStock: 0,
    perMemberPurchaseLimit: 1,
    remainingPurchaseLimit: 1,
  };

  assert.equal(isShopItemSoldOut(baseItem), true);
  assert.equal(getShopItemAvailabilityLabel(baseItem), "품절");

  const limitedItem = {
    ...baseItem,
    soldQuantity: 1,
    remainingStock: 9,
    remainingPurchaseLimit: 0,
  };

  assert.equal(isShopItemLimitReached(limitedItem), true);
  assert.equal(getShopItemAvailabilityLabel(limitedItem), "구매 제한 도달");
});

test("identifies account refill count shop items", () => {
  assert.equal(isAccountRefillShopItem({ itemType: "ACCOUNT_REFILL_COUNT" }), true);
  assert.equal(isAccountRefillShopItem({ itemType: "COFFEE_VOUCHER" }), false);
});

test("maps coffee shop items to the bundled coffee image", () => {
  assert.equal(
    getShopItemImagePath({
      code: "voucher.coffee",
      name: "커피 교환권",
    }),
    "/images/IceAmericano.webp"
  );
});
