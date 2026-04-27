import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const source = readFileSync(path.join(__dirname, "MarketDetailRealtimeView.tsx"), "utf8");

test("symbol selector renders supported BTCUSDT and ETHUSDT markets", () => {
  assert.equal(source.includes("SUPPORTED_MARKET_SYMBOLS.map"), true);
  assert.equal(source.includes("router.push(`/markets/${symbol}`)"), true);
});

test("close position button uses held quantity instead of closeable quantity gating", () => {
  assert.equal(source.includes("quantity={position.quantity}"), true);
  assert.equal(source.includes("disabled={closeableQuantity <= 0}"), false);
  assert.equal(source.includes("getCloseableQuantity"), false);
});

test("Close amount displays accumulated closed quantity, not closeable quantity", () => {
  assert.equal(source.includes("getAccumulatedClosedQuantity(position)"), true);
  assert.equal(source.includes('label="Close amount"'), true);
  assert.equal(source.includes("value={`${formatPlainNumber(closeableQuantity)"), false);
});

test("TP/SL editor is opened from an edit affordance", () => {
  assert.equal(source.includes('aria-label="Edit Position TP/SL"'), true);
  assert.equal(source.includes("setIsOpen(true)"), true);
  assert.equal(source.includes("isOpen={isOpen}"), true);
});
