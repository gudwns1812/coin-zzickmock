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

test("trading title renders logo, asset, and perpetual on one row", () => {
  assert.equal(source.includes("getMarketLogoPath(market.symbol)"), true);
  assert.equal(source.includes("{market.assetName}"), true);
  assert.equal(source.includes("Perpetual"), true);
  assert.equal(source.includes("items-baseline gap-3"), true);
  assert.equal(source.includes("flex-col leading-none"), false);
  assert.equal(source.includes("{market.displayName}"), false);
});

test("trading title does not render explanatory page copy", () => {
  assert.equal(source.includes("메인"), false);
  assert.equal(source.includes("트레이딩 화면입니다"), false);
});

test("close position button uses held quantity instead of closeable quantity gating", () => {
  assert.equal(source.includes("quantity={position.quantity}"), true);
  assert.equal(source.includes("disabled={closeableQuantity <= 0}"), false);
  assert.equal(source.includes("getCloseableQuantity"), false);
});

test("order entry panel receives displayed positions for close orders", () => {
  assert.equal(source.includes("positions={displayedPositions}"), true);
});

test("top latest trade price displays the realtime candle close when available", () => {
  assert.equal(source.includes("latestCandleClosePrice"), true);
  assert.equal(source.includes("formatUsd(latestCandleClosePrice ?? market.lastPrice)"), true);
  assert.equal(source.includes("onLatestCandleClosePriceChange={handleLatestCandleClosePriceChange}"), true);
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

test("position card header includes margin mode next to side", () => {
  assert.equal(source.includes("{position.marginMode} · {position.positionSide}"), true);
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

test("position card labels estimated and unavailable liquidation prices", () => {
  assert.equal(source.includes("function formatPositionLiquidationPrice"), true);
  assert.equal(source.includes('position.liquidationPriceType === "UNAVAILABLE"'), true);
  assert.equal(source.includes('position.liquidationPriceType === "ESTIMATED"'), true);
  assert.equal(source.includes("(Est.)"), true);
});

test("Open Orders table renders edit and cancel actions for editable limit orders", () => {
  assert.equal(source.includes("import EditOrderButton"), true);
  assert.equal(source.includes("<EditOrderButton"), true);
  assert.equal(source.includes("currentLimitPrice={order.limitPrice}"), true);
  assert.equal(source.includes("isEditableOpenLimitOrder(order)"), true);
  assert.equal(source.includes("<CancelOrderButton orderId={order.orderId} />"), true);
});

test("Open Orders table uses stable display sorting and fixed action layout", () => {
  assert.equal(source.includes("[...orders].sort(compareOpenOrdersForDisplay)"), true);
  assert.equal(source.includes("min-w-[1120px] table-fixed"), true);
  assert.equal(source.includes("flex min-w-[190px] justify-start gap-2"), true);
  assert.equal(source.includes("{formatOrderPurpose(order)} · {order.orderType}"), true);
});

test("market detail renders quick limit selector between chart and order panel", () => {
  assert.equal(source.includes("import QuickLimitPriceSelector"), true);
  assert.equal(source.includes("<FuturesPriceChart"), true);
  assert.equal(source.includes("<QuickLimitPriceSelector"), true);
  assert.equal(source.includes("<OrderEntryPanel"), true);
  assert.equal(
    source.indexOf("<FuturesPriceChart") <
      source.indexOf("<QuickLimitPriceSelector"),
    true
  );
  assert.equal(
    source.indexOf("<QuickLimitPriceSelector") <
      source.indexOf("<OrderEntryPanel"),
    true
  );
});

test("market detail wires quick limit price locally without global UI state", () => {
  assert.equal(source.includes("useState<QuickLimitPriceSelection | null>"), true);
  assert.equal(source.includes("handleQuickLimitPriceSelect"), true);
  assert.equal(source.includes("quickLimitPriceSelection={quickLimitPriceSelection}"), true);
  assert.equal(source.includes("useOrderBook"), false);
  assert.equal(source.includes("zustand"), false);
});
