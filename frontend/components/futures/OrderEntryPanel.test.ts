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

test("limit price follows live price until the user edits it", () => {
  assert.equal(source.includes("priceSnapshotSymbolRef"), true);
  assert.equal(source.includes("priceSnapshotSymbolRef.current !== symbol"), true);
  assert.equal(source.includes("if (!isLimitPriceDirty)"), true);
  assert.equal(source.includes("[currentPrice, isLimitPriceDirty, symbol]"), true);
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

test("open order max quantity uses fee-aware floored helper while close mode remains position based", () => {
  assert.equal(source.includes("calculateMaxOpenOrderQuantity"), true);
  assert.equal(source.includes("calculateMaxOpenMarketQuantity"), false);
  assert.equal(source.includes("(availableBalance * leverage) / effectivePrice"), false);
  assert.equal(source.includes("formatFlooredQuantity(value)"), true);
  assert.equal(source.includes("toFixed(3)"), false);
});

test("open limit helper and summary use conservative affordability price", () => {
  assert.equal(source.includes("resolveOpenOrderAffordabilityPrice"), true);
  assert.equal(source.includes("openOrderAffordabilityPrice"), true);
  assert.equal(
    source.includes("resolveOpenOrderAffordabilityPrice(\n    orderType,\n    currentPrice,\n    parsedLimitPrice"),
    true
  );
  assert.equal(
    source.includes(
      "calculateMaxOpenOrderQuantity(\n          availableBalance,\n          leverage,\n          openOrderAffordabilityPrice"
    ),
    true
  );
  assert.equal(
    source.includes(
      'ticketMode === "OPEN" ? openOrderAffordabilityPrice : effectivePrice'
    ),
    true
  );
  assert.equal(source.includes("parsedQuantity * orderNotionalPrice"), true);
});

test("order ticket exposes current side so leverage edits target the intended position", () => {
  assert.equal(source.includes('onClick={() => handleSubmit("LONG")}'), true);
  assert.equal(source.includes('onClick={() => handleSubmit("SHORT")}'), true);
  assert.equal(source.includes("onMouseEnter={() => setPositionSide"), false);
  assert.equal(source.includes("onFocus={() => setPositionSide"), false);
  assert.equal(source.includes("positionSide: nextSide"), true);
});

test("order ticket defaults to cross margin and 10x leverage without a stored preference", () => {
  assert.equal(source.includes("DEFAULT_MARGIN_MODE: MarginMode = \"CROSS\""), true);
  assert.equal(source.includes("const DEFAULT_LEVERAGE = 10"), true);
  assert.equal(source.includes("DEFAULT_TICKET_PREFERENCE"), true);
  assert.equal(source.includes("readTicketPreference(symbol)"), true);
});

test("order ticket persists symbol-scoped margin leverage and side preference", () => {
  assert.equal(source.includes("futures-order-ticket"), true);
  assert.equal(source.includes("window.localStorage.getItem"), true);
  assert.equal(source.includes("window.localStorage.setItem"), true);
  assert.equal(source.includes("getTicketPreferenceStorageKey(symbol)"), true);
  assert.equal(source.includes("ticketPreferenceSymbol !== symbol"), true);
  assert.equal(source.includes("writeTicketPreference(symbol, ticketPreference)"), true);
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

test("leverage can target an existing symbol position even when current side is empty", () => {
  assert.equal(source.includes("findPositionForSymbol(positions, symbol)"), true);
  assert.equal(source.includes("leverageTargetPosition.positionSide"), true);
  assert.equal(source.includes("leverageTargetPosition.marginMode"), true);
  assert.equal(source.includes("positionSide: updatedPosition.positionSide"), false);
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

test("quick limit price selection controls the ticket price and switches back to Limit", () => {
  assert.equal(
    source.includes(
      "quickLimitPriceSelection?: QuickLimitPriceSelection | null"
    ),
    true
  );
  assert.equal(source.includes("setOrderType(\"LIMIT\")"), true);
  assert.equal(
    source.includes(
      "setLimitPrice(formatLimitPriceInput(quickLimitPriceSelection.price))"
    ),
    true
  );
  assert.equal(source.includes("setIsLimitPriceDirty(true)"), true);
  assert.equal(source.includes("setInlineErrorMessage(null)"), true);
});

test("quick limit price formatting preserves sub-0.1 symbol units", () => {
  assert.equal(source.includes(".toFixed(4)"), true);
  assert.equal(source.includes('.replace(/(\\.\\d*?)0+$/, "$1")'), true);
  assert.equal(source.includes('step="any"'), true);
});

test("order ticket still builds payload through existing limit and market branches", () => {
  assert.equal(
    source.includes(
      "limitPrice: orderType === \"LIMIT\" ? parsedLimitPrice : null"
    ),
    true
  );
  assert.equal(source.includes("/proxy-futures/orders"), true);
  assert.equal(source.includes("/proxy-futures/positions/close"), true);
});

test("order account summary uses displayed positions for live mark-to-market values", () => {
  assert.equal(source.includes("deriveLiveAccountSummaryDisplay"), true);
  assert.equal(source.includes("liveAccountSummary"), true);
  assert.equal(source.includes("accountSummary.totalUnrealizedPnl"), false);
  assert.equal(source.includes("accountSummary.roi"), false);
});
