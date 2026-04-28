import assert from "node:assert/strict";
import test from "node:test";

const adminShopItemModule: typeof import("./admin-shop-item-ui") =
  await import(new URL("./admin-shop-item-ui.ts", import.meta.url).href);

const { EMPTY_ADMIN_SHOP_ITEM_FORM, toAdminShopItemInput } =
  adminShopItemModule;

test("builds admin shop item input from form values", () => {
  const result = toAdminShopItemInput(
    {
      ...EMPTY_ADMIN_SHOP_ITEM_FORM,
      code: " voucher.coffee ",
      name: " 커피 교환권 ",
      description: " 휴대폰으로 발송 ",
      price: "100",
      totalStock: "",
      perMemberPurchaseLimit: "1",
      sortOrder: "10",
    },
    "create"
  );

  assert.equal(result.error, null);
  assert.deepEqual(result.input, {
    code: "voucher.coffee",
    name: "커피 교환권",
    description: "휴대폰으로 발송",
    itemType: "COFFEE_VOUCHER",
    price: 100,
    active: true,
    totalStock: null,
    perMemberPurchaseLimit: 1,
    sortOrder: 10,
  });
});

test("rejects invalid admin shop item numbers", () => {
  const result = toAdminShopItemInput(
    {
      ...EMPTY_ADMIN_SHOP_ITEM_FORM,
      code: "voucher.bad",
      name: "Bad",
      description: "Bad",
      price: "0",
      sortOrder: "1",
    },
    "create"
  );

  assert.equal(result.input, null);
  assert.equal(result.error, "가격은 0보다 커야 합니다.");
});
