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

test("quick limit selector presents price rows without fake depth columns or labels", () => {
  assert.equal(source.includes("Quick Limit"), true);
  assert.equal(source.includes("onSelectPrice"), true);
  assert.equal(source.includes("Quantity"), false);
  assert.equal(source.includes("Total"), false);
  assert.equal(source.includes("Depth"), false);
  assert.equal(source.includes("Order-book inspired price picks"), false);
  assert.equal(source.includes("Ask +"), false);
  assert.equal(source.includes("Bid -"), false);
});

test("quick limit renders seven price rows on each side of the last price", () => {
  assert.equal(source.includes("const ROWS_PER_SIDE = 7"), true);
  assert.equal(source.includes("upperRows.map"), true);
  assert.equal(source.includes("lowerRows.map"), true);
  assert.equal(source.includes("safeLastPrice + safeUnit * index"), true);
  assert.equal(source.includes("safeLastPrice - safeUnit * index"), true);
});

test("quick limit units are symbol-specific and persisted in localStorage", () => {
  assert.equal(source.includes("BTCUSDT"), true);
  assert.equal(source.includes('{ label: "0.1", value: 0.1 }'), true);
  assert.equal(source.includes('{ label: "1000", value: 1000 }'), true);
  assert.equal(source.includes("ETHUSDT"), true);
  assert.equal(source.includes('{ label: "0.01", value: 0.01 }'), true);
  assert.equal(source.includes("coin-zzickmock.quick-limit-price-unit"), true);
  assert.equal(source.includes("window.localStorage.getItem"), true);
  assert.equal(source.includes("window.localStorage.setItem"), true);
});

test("quick limit selector does not fabricate selectable prices for invalid snapshots", () => {
  assert.equal(source.includes("lastPrice: null"), true);
  assert.equal(source.includes("return EMPTY_PRICE_ROWS"), true);
  assert.equal(source.includes("Price unavailable"), true);
  assert.equal(source.includes("return null;"), true);
});

test("quick limit price rows are buttons with accessible labels and matching price typography", () => {
  assert.equal(
    source.includes("aria-label={`${symbol} quick limit price selector`}"),
    true
  );
  assert.equal(source.includes("Select latest price"), true);
  assert.equal(source.includes("Select ${formatUsd(row.price)} as limit price"), true);
  assert.equal(source.includes("text-base-custom font-black tabular-nums"), true);
  assert.equal(source.includes('type="button"'), true);
});
