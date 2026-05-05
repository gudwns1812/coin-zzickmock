import assert from "node:assert/strict";
import test from "node:test";

const quantityModule: typeof import("./order-entry-quantity") = await import(
  new URL("./order-entry-quantity.ts", import.meta.url).href
);

const {
  calculateMaxOpenMarketQuantity,
  floorQuantityToPrecision,
  formatFlooredQuantity,
  OPEN_MARKET_TAKER_FEE_RATE,
} = quantityModule;

test("open market max quantity includes taker fee in affordability denominator", () => {
  const quantity = calculateMaxOpenMarketQuantity(1000, 10, 100);

  assert.equal(quantity, 1000 / (100 * (1 / 10 + OPEN_MARKET_TAKER_FEE_RATE)));
});

test("quantity precision floors without rounding up at the affordability boundary", () => {
  assert.equal(floorQuantityToPrecision(99.5024875621), 99.502);
  assert.equal(formatFlooredQuantity(99.5029875621), "99.502");
});

test("invalid inputs resolve to zero quantity", () => {
  assert.equal(calculateMaxOpenMarketQuantity(1000, 0, 100), 0);
  assert.equal(calculateMaxOpenMarketQuantity(1000, 10, 0), 0);
  assert.equal(floorQuantityToPrecision(Number.NaN), 0);
});
