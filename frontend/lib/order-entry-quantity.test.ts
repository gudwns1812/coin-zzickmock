import assert from "node:assert/strict";
import test from "node:test";

const quantityModule: typeof import("./order-entry-quantity") = await import(
  new URL("./order-entry-quantity.ts", import.meta.url).href
);

const {
  calculateMaxOpenOrderQuantity,
  resolveOpenOrderAffordabilityPrice,
  floorQuantityToPrecision,
  formatFlooredQuantity,
  OPEN_ORDER_TAKER_FEE_RATE,
} = quantityModule;

test("open order max quantity includes taker fee in affordability denominator", () => {
  const quantity = calculateMaxOpenOrderQuantity(1000, 10, 100);

  assert.equal(quantity, 1000 / (100 * (1 / 10 + OPEN_ORDER_TAKER_FEE_RATE)));
});

test("open market affordability price uses the current market price", () => {
  assert.equal(resolveOpenOrderAffordabilityPrice("MARKET", 100, 95), 100);
});

test("open limit affordability price uses current price when limit is below market", () => {
  const affordabilityPrice = resolveOpenOrderAffordabilityPrice(
    "LIMIT",
    100,
    95
  );
  const quantity = calculateMaxOpenOrderQuantity(1000, 10, affordabilityPrice);

  assert.equal(affordabilityPrice, 100);
  assert.equal(quantity, 1000 / (100 * (1 / 10 + OPEN_ORDER_TAKER_FEE_RATE)));
});

test("open limit affordability price uses entered limit price when limit is above market", () => {
  const affordabilityPrice = resolveOpenOrderAffordabilityPrice(
    "LIMIT",
    100,
    105
  );
  const quantity = calculateMaxOpenOrderQuantity(1000, 10, affordabilityPrice);

  assert.equal(affordabilityPrice, 105);
  assert.equal(quantity, 1000 / (105 * (1 / 10 + OPEN_ORDER_TAKER_FEE_RATE)));
});

test("quantity precision floors without rounding up at the affordability boundary", () => {
  assert.equal(floorQuantityToPrecision(99.5024875621), 99.502);
  assert.equal(formatFlooredQuantity(99.5029875621), "99.502");
});

test("invalid inputs resolve to zero quantity", () => {
  assert.equal(calculateMaxOpenOrderQuantity(1000, 0, 100), 0);
  assert.equal(calculateMaxOpenOrderQuantity(1000, 10, 0), 0);
  assert.equal(calculateMaxOpenOrderQuantity(1000, 10, 100, -0.01), 0);
  assert.equal(resolveOpenOrderAffordabilityPrice("MARKET", 0), 0);
  assert.equal(resolveOpenOrderAffordabilityPrice("MARKET", Number.NaN), 0);
  assert.equal(resolveOpenOrderAffordabilityPrice("LIMIT", 100), 0);
  assert.equal(resolveOpenOrderAffordabilityPrice("LIMIT", 100, 0), 0);
  assert.equal(
    resolveOpenOrderAffordabilityPrice("LIMIT", 100, Number.POSITIVE_INFINITY),
    0
  );
  assert.equal(floorQuantityToPrecision(Number.NaN), 0);
});
