import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const source = readFileSync(path.join(__dirname, "FuturesPriceChart.tsx"), "utf8");

test("volume renders in a resizable lightweight-charts pane", () => {
  assert.equal(source.includes("const VOLUME_PANE_INDEX = 1"), true);
  assert.equal(source.includes("enableResize: true"), true);
  assert.equal(source.includes("HistogramSeries"), true);
  assert.equal(source.includes("VOLUME_PANE_INDEX"), true);
  assert.equal(source.includes("setStretchFactor"), true);
  assert.equal(source.includes("visible: true"), true);
});

test("chart uses a taller canvas wrapper matching the lightweight-charts height", () => {
  assert.equal(source.includes("const CHART_HEIGHT = 620"), true);
  assert.equal(source.includes("relative h-[620px]"), true);
  assert.equal(source.includes("chart.resize(container.clientWidth, CHART_HEIGHT)"), true);
});

test("volume pane exposes a hovered candle volume legend", () => {
  assert.equal(source.includes("hoveredVolume"), true);
  assert.equal(source.includes("volume legend"), true);
  assert.equal(source.includes("formatVolume(displayedVolume)"), true);
});

test("chart header avoids instructional helper copy", () => {
  assert.equal(source.includes("getChartDescription"), false);
  assert.equal(source.includes("왼쪽으로 이동하면"), false);
  assert.equal(source.includes("최신 이동은 버튼으로만 수행합니다"), false);
});

test("chart toolbar uses latest view without past-load status copy", () => {
  assert.equal(source.includes("최신 보기"), true);
  assert.equal(source.includes("activeChartStatus"), false);
  assert.equal(source.includes("getFuturesChartStatus"), false);
  assert.equal(source.includes("과거 로드 가능"), false);
});

test("chart loading-empty mode does not render live points", () => {
  assert.equal(source.includes("chartRenderMode === \"live-fallback\""), true);
  assert.equal(source.includes("liveSeries.setData(livePoints);"), true);
  assert.equal(source.includes("chartRenderMode === \"loading-empty\""), true);
  assert.equal(
    source.includes("} else if (chartRenderMode === \"loading-empty\") {\n      candleSeries.setData([]);\n      volumeSeries.setData([]);\n      liveSeries.setData([]);\n    }"),
    true
  );
});

test("chart consumes backend candle stream instead of synthesizing candles from market price", () => {
  assert.equal(source.includes("/candles/stream?"), true);
  assert.equal(source.includes("mergeCandlesWithRealtimeCandle"), true);
  assert.equal(source.includes("mergeCandlesWithLivePrice"), false);
  assert.equal(source.includes("getLiveCandleBucket"), false);
});

test("chart reports realtime candle close price to the trading header", () => {
  assert.equal(
    source.includes("onLatestCandleClosePriceChange?: (closePrice: number, receivedAt: number) => void"),
    true
  );
  assert.equal(
    source.includes("onLatestCandleClosePriceChange?.(data.closePrice, Date.now())"),
    true
  );
});

test("chart refetches closed candle history after the realtime bucket has settled", () => {
  assert.equal(source.includes("CLOSED_CANDLE_REFETCH_DELAY_MS = 4_000"), true);
  assert.equal(source.includes("scheduleClosedCandleFinalizationRefetch"), true);
  assert.equal(source.includes("window.setTimeout"), true);
  assert.equal(source.includes("window.clearTimeout"), true);
  assert.equal(
    source.includes("if (!previousOpenTime || previousOpenTime !== data.openTime)"),
    true
  );
  assert.equal(
    source.includes("if (previousOpenTime && previousOpenTime !== data.openTime)"),
    false
  );
});

test("chart does not immediately invalidate REST history on realtime bucket changes", () => {
  assert.equal(
    source.includes("if (previousOpenTime && previousOpenTime !== data.openTime)"),
    false
  );
  assert.equal(
    source.includes("onLatestCandleClosePriceChange?.(data.closePrice, Date.now())"),
    true
  );
});

test("chart invalidates history from backend finalization notifications", () => {
  assert.equal(source.includes("type: \"historyFinalized\""), true);
  assert.equal(source.includes("isHistoryFinalizedResponse"), true);
  assert.equal(source.includes("data.affectedIntervals.includes(selectedInterval)"), true);
  assert.equal(source.includes("data.symbol === symbol"), true);
  assert.equal(source.includes("clearClosedCandleRefetchTimeout(closedCandleRefetchTimeoutRef);"), true);
});
