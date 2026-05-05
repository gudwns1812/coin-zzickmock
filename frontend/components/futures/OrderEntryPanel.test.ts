import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const source = readFileSync(path.join(__dirname, "OrderEntryPanel.tsx"), "utf8");

test("order entry panel does not expose disabled TP/SL controls", () => {
  assert.equal(source.includes('type="checkbox"'), false);
  assert.equal(source.includes("TP/SL"), false);
});

test("limit price is snapshot-initialized and not live-overwritten", () => {
  assert.equal(source.includes("priceSnapshotSymbolRef"), true);
  assert.equal(source.includes("priceSnapshotSymbolRef.current !== symbol"), true);
  assert.equal(source.includes("[currentPrice, symbol]"), true);
});

test("close mode submits through the position close endpoint", () => {
  assert.equal(source.includes('{ label: "Close", value: "CLOSE" }'), true);
  assert.equal(source.includes('/proxy-futures/positions/close'), true);
  assert.equal(source.includes('ticketMode !== "OPEN"'), true);
});

test("close mode handles no-position failures inline without console noise", () => {
  assert.equal(source.includes('code === "POSITION_NOT_FOUND"'), true);
  assert.equal(source.includes("종료할 포지션이 없습니다."), true);
  assert.equal(source.includes("console.error"), false);
  assert.equal(source.includes("console.warn"), false);
  assert.equal(source.includes("console.log"), false);
});

test("close quantity controls use held position quantity", () => {
  assert.equal(source.includes("matchingPosition?.quantity ?? 0"), true);
  assert.equal(source.includes('ticketMode === "CLOSE" ? maxCloseQuantity : maxOpenQuantity'), true);
  assert.equal(source.includes("closeableQuantity"), false);
});

test("side control lets close helpers target long or short positions", () => {
  assert.equal(source.includes("function SideToggle"), true);
  assert.equal(source.includes('aria-label="Position side"'), true);
  assert.equal(source.includes("setPositionSide(side)"), true);
});

test("order ticket defaults to cross margin and 10x leverage without a selected-side position", () => {
  assert.equal(source.includes("DEFAULT_MARGIN_MODE: MarginMode = \"CROSS\""), true);
  assert.equal(source.includes("const DEFAULT_LEVERAGE = 10"), true);
  assert.equal(source.includes("setMarginMode(DEFAULT_MARGIN_MODE)"), true);
  assert.equal(source.includes("setLeverage(DEFAULT_LEVERAGE)"), true);
});

test("existing selected-side position locks margin mode and drives leverage", () => {
  assert.equal(source.includes("findPositionForSide"), true);
  assert.equal(source.includes("selectedSidePosition.marginMode"), true);
  assert.equal(source.includes("selectedSidePosition.leverage"), true);
  assert.equal(source.includes("disabled={isMarginModeLocked}"), true);
});

test("leverage modal applies existing-position leverage through the position endpoint", () => {
  assert.equal(source.includes('/proxy-futures/positions/leverage'), true);
  assert.equal(source.includes("onApply={handleApplyLeverage}"), true);
  assert.equal(source.includes("draftLeverage"), true);
});

test("order help tooltip explains margin leverage and order concepts", () => {
  assert.equal(source.includes("function OrderHelpTooltip"), true);
  assert.equal(source.includes("주문 도움말"), true);
  assert.equal(source.includes("Cross"), true);
  assert.equal(source.includes("Limit"), true);
  assert.equal(source.includes("Long"), true);
});

test("order preview labels estimated and unavailable liquidation prices", () => {
  assert.equal(source.includes("function formatLiquidationPrice"), true);
  assert.equal(source.includes('type === "UNAVAILABLE"'), true);
  assert.equal(source.includes('type === "ESTIMATED"'), true);
  assert.equal(source.includes("(Est.)"), true);
  assert.equal(source.includes("현재 다른 심볼 가격을 고정한 추정값"), true);
});
