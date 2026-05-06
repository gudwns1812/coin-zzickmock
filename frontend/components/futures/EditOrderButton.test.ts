import assert from "node:assert/strict";
import test from "node:test";
import type { ModifyFuturesOrderResult } from "../../lib/futures-api";
const orderEditModule: typeof import("../../lib/futures-order-edit") = await import(
  new URL("../../lib/futures-order-edit.ts", import.meta.url).href
);

const { submitOrderPriceEdit, toEditableLimitPrice } = orderEditModule;

const baseResult: ModifyFuturesOrderResult = {
  orderId: "order-1",
  symbol: "BTCUSDT",
  status: "PENDING",
  limitPrice: 95,
  feeType: "MAKER",
  estimatedFee: 0.001425,
  executionPrice: 95,
};

test("edit order modal submits a positive limit price through the modify API", async () => {
  const calls: Array<[string, number]> = [];
  let refreshed = false;
  let closed = false;
  const successMessages: string[] = [];
  const errorMessages: string[] = [];

  const submitted = await submitOrderPriceEdit({
    orderId: "open-order",
    limitPrice: "95.25",
    modifyOrderPrice: async (orderId, limitPrice) => {
      calls.push([orderId, limitPrice]);
      return { ...baseResult, orderId, limitPrice, executionPrice: limitPrice };
    },
    refresh: () => {
      refreshed = true;
    },
    closeModal: () => {
      closed = true;
    },
    showSuccess: (message) => successMessages.push(message),
    showError: (message) => errorMessages.push(message),
  });

  assert.equal(submitted, true);
  assert.deepEqual(calls, [["open-order", 95.25]]);
  assert.equal(refreshed, true);
  assert.equal(closed, true);
  assert.deepEqual(successMessages, ["BTCUSDT 대기 주문 가격을 수정했습니다."]);
  assert.deepEqual(errorMessages, []);
});

test("edit order modal reports immediate fill when modified price is marketable", async () => {
  const successMessages: string[] = [];

  const submitted = await submitOrderPriceEdit({
    orderId: "open-order",
    limitPrice: "101",
    modifyOrderPrice: async (orderId, limitPrice) => ({
      ...baseResult,
      orderId,
      status: "FILLED",
      limitPrice,
      feeType: "TAKER",
      estimatedFee: 0.005,
      executionPrice: 100,
    }),
    refresh: () => {},
    closeModal: () => {},
    showSuccess: (message) => successMessages.push(message),
    showError: () => {
      throw new Error("error toast should not run for a filled edit");
    },
  });

  assert.equal(submitted, true);
  assert.deepEqual(successMessages, ["BTCUSDT 주문이 즉시 체결되었습니다."]);
});

test("edit order modal blocks invalid limit prices before the modify API", async () => {
  for (const limitPrice of ["0", "-1", "Infinity", "NaN", ""] as const) {
    const calls: Array<[string, number]> = [];
    const errorMessages: string[] = [];

    const submitted = await submitOrderPriceEdit({
      orderId: "open-order",
      limitPrice,
      modifyOrderPrice: async (orderId, nextLimitPrice) => {
        calls.push([orderId, nextLimitPrice]);
        return baseResult;
      },
      refresh: () => {
        throw new Error("refresh should not run for invalid input");
      },
      closeModal: () => {
        throw new Error("close should not run for invalid input");
      },
      showSuccess: () => {
        throw new Error("success toast should not run for invalid input");
      },
      showError: (message) => errorMessages.push(message),
    });

    assert.equal(submitted, false);
    assert.deepEqual(calls, []);
    assert.deepEqual(errorMessages, ["수정할 주문 가격을 확인해주세요."]);
  }
});

test("open order limit price parser accepts only positive finite numbers", () => {
  assert.equal(toEditableLimitPrice("123.45"), 123.45);
  assert.equal(toEditableLimitPrice("0"), null);
  assert.equal(toEditableLimitPrice("NaN"), null);
  assert.equal(toEditableLimitPrice("Infinity"), null);
});
