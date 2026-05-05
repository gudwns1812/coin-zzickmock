import assert from "node:assert/strict";
import test from "node:test";
import type { FuturesOpenOrder } from "./futures-api";

const openOrderActionsModule: typeof import("./futures-open-order-actions") = await import(
  new URL("./futures-open-order-actions.ts", import.meta.url).href
);

const { isEditableOpenLimitOrder } = openOrderActionsModule;

const baseOrder: FuturesOpenOrder = {
  orderId: "order-1",
  symbol: "BTCUSDT",
  positionSide: "LONG",
  orderType: "LIMIT",
  orderPurpose: "OPEN_POSITION",
  marginMode: "ISOLATED",
  leverage: 10,
  quantity: 0.1,
  limitPrice: 95,
  status: "PENDING",
  feeType: "MAKER",
  estimatedFee: 0.001425,
  executionPrice: 95,
  orderTime: "2026-05-05T00:00:00Z",
};

test("normal pending limit orders are editable", () => {
  assert.equal(isEditableOpenLimitOrder(baseOrder), true);
});

test("conditional TP/SL orders are not editable from Open Orders", () => {
  assert.equal(
    isEditableOpenLimitOrder({
      ...baseOrder,
      orderId: "tp-order",
      orderType: "MARKET",
      limitPrice: null,
      triggerPrice: 110,
      triggerType: "TAKE_PROFIT",
      triggerSource: "MARK_PRICE",
      ocoGroupId: "oco-1",
    }),
    false,
  );
});

test("non-pending and market orders are not editable", () => {
  assert.equal(isEditableOpenLimitOrder({ ...baseOrder, status: "FILLED" }), false);
  assert.equal(
    isEditableOpenLimitOrder({
      ...baseOrder,
      orderType: "MARKET",
      limitPrice: null,
    }),
    false,
  );
});
