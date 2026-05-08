import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const source = readFileSync(
  path.join(__dirname, "QuickLimitPriceSelector.tsx"),
  "utf8"
);

test("quick limit selector presents price rows without fake depth columns", () => {
  assert.equal(source.includes("Quick Limit"), true);
  assert.equal(source.includes("onSelectPrice"), true);
  assert.equal(source.includes("Quantity"), false);
  assert.equal(source.includes("Total"), false);
  assert.equal(source.includes("Depth"), false);
});

test("quick limit rows are derived from last price with bid and ask tones", () => {
  assert.equal(source.includes("buildQuickLimitPriceRows(lastPrice)"), true);
  assert.equal(source.includes('tone: "ask"'), true);
  assert.equal(source.includes('tone: "bid"'), true);
  assert.equal(source.includes("safeLastPrice + step * index"), true);
  assert.equal(source.includes("safeLastPrice - step * index"), true);
});

test("quick limit selector does not fabricate selectable prices for invalid snapshots", () => {
  assert.equal(source.includes("lastPrice: null"), true);
  assert.equal(source.includes("return EMPTY_PRICE_ROWS"), true);
  assert.equal(source.includes("Price unavailable"), true);
  assert.equal(source.includes("return null;"), true);
});

test("quick limit price rows are buttons with accessible labels", () => {
  assert.equal(
    source.includes("aria-label={`${symbol} quick limit price selector`}"),
    true
  );
  assert.equal(source.includes("Select latest price"), true);
  assert.equal(
    source.includes("Select ${formatPriceInput(row.price)} USDT"),
    true
  );
  assert.equal(source.includes('type="button"'), true);
});
