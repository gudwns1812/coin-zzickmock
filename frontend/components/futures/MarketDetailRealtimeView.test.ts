import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const source = readFileSync(path.join(__dirname, "MarketDetailRealtimeView.tsx"), "utf8");
const detailPageSource = readFileSync(
  path.join(__dirname, "../../app/(main)/markets/[symbol]/page.tsx"),
  "utf8"
);

test("symbol selector renders supported BTCUSDT and ETHUSDT markets", () => {
  assert.equal(source.includes("SUPPORTED_MARKET_SYMBOLS.map"), true);
  assert.equal(source.includes("router.push(`/markets/${symbol}`)"), true);
});

test("market detail remounts when the route symbol changes", () => {
  assert.equal(source.includes("prevSymbol"), false);
  assert.equal(detailPageSource.includes("key={market.symbol}"), true);
});

test("trading title renders asset and perpetual on separate lines with logo", () => {
  assert.equal(source.includes("getMarketLogoPath(market.symbol)"), true);
  assert.equal(source.includes("{market.assetName}"), true);
  assert.equal(source.includes("Perpetual"), true);
  assert.equal(source.includes("{market.displayName}"), false);
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

test("position cards display held position size", () => {
  assert.equal(source.includes('label="Size"'), true);
  assert.equal(
    source.includes(
      "value={`${formatPlainNumber(position.quantity)} ${getBaseAsset(position.symbol)}`}"
    ),
    true
  );
});

test("TP/SL editor is opened from an edit affordance", () => {
  assert.equal(source.includes('aria-label="Edit Position TP/SL"'), true);
  assert.equal(source.includes("setIsOpen(true)"), true);
  assert.equal(source.includes("isOpen={isOpen}"), true);
});

test("TP/SL editor and order tables support conditional order semantics", () => {
  assert.equal(source.includes("wasOpenRef"), true);
  assert.equal(source.includes('triggerType === "TAKE_PROFIT"'), true);
  assert.equal(source.includes('return "TP Close"'), true);
  assert.equal(source.includes('return "SL Close"'), true);
  assert.equal(source.includes("order.triggerPrice ?? order.limitPrice"), true);
});

test("TP/SL editor does not default empty fields to mark price", () => {
  assert.equal(source.includes("setTakeProfitPrice(formatEditablePrice(position.takeProfitPrice))"), true);
  assert.equal(source.includes("setStopLossPrice(formatEditablePrice(position.stopLossPrice))"), true);
  assert.equal(source.includes("snapshotPrice"), false);
});
