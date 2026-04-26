import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";
import type { UTCTimestamp } from "lightweight-charts";

const indicatorModule: typeof import("./futuresChartIndicators") = await import(
  new URL("./futuresChartIndicators.ts", import.meta.url).href
);
const {
  calculateIndicatorValueRows,
  canEditIndicators,
  DEFAULT_INDICATOR_CONFIGS,
  getIndicatorFallbackMessage,
  MAX_ACTIVE_INDICATORS,
  resetIndicators,
  setIndicatorEnabled,
  updateBollingerStdDev,
  updateIndicatorPeriod,
} = indicatorModule;

const __dirname = path.dirname(fileURLToPath(import.meta.url));

test("indicators are off by default", () => {
  assert.equal(DEFAULT_INDICATOR_CONFIGS.EMA.enabled, false);
  assert.equal(DEFAULT_INDICATOR_CONFIGS.SMA.enabled, false);
  assert.equal(DEFAULT_INDICATOR_CONFIGS.BOLLINGER.enabled, false);
});

test("indicator config enforces the max active rule without duplicate types", () => {
  const withEma = setIndicatorEnabled(DEFAULT_INDICATOR_CONFIGS, "EMA", true);
  const withSma = setIndicatorEnabled(withEma, "SMA", true);
  const withBollinger = setIndicatorEnabled(withSma, "BOLLINGER", true);
  const duplicateEma = setIndicatorEnabled(withBollinger, "EMA", true);

  assert.equal(
    [duplicateEma.EMA, duplicateEma.SMA, duplicateEma.BOLLINGER].filter(
      (indicator) => indicator.enabled
    ).length,
    MAX_ACTIVE_INDICATORS
  );
  assert.equal(duplicateEma.EMA.enabled, true);
  assert.equal(duplicateEma.SMA.enabled, true);
  assert.equal(duplicateEma.BOLLINGER.enabled, true);
});

test("indicator parameter updates are clamped to the approved bounds", () => {
  const updatedPeriod = updateIndicatorPeriod(
    DEFAULT_INDICATOR_CONFIGS,
    "EMA",
    999
  );
  const updatedBollinger = updateBollingerStdDev(updatedPeriod, 0.2);

  assert.equal(updatedBollinger.EMA.period, 200);
  assert.equal(updatedBollinger.BOLLINGER.stdDev, 1);
});

test("resetIndicators keeps parameter presets while disabling every indicator", () => {
  const configured = updateBollingerStdDev(
    updateIndicatorPeriod(
      setIndicatorEnabled(DEFAULT_INDICATOR_CONFIGS, "BOLLINGER", true),
      "BOLLINGER",
      44
    ),
    3.5
  );
  const reset = resetIndicators(configured);

  assert.equal(reset.BOLLINGER.enabled, false);
  assert.equal(reset.BOLLINGER.period, 44);
  assert.equal(reset.BOLLINGER.stdDev, 3.5);
});

test("stale history keeps config but disables indicator editing", () => {
  const configured = setIndicatorEnabled(DEFAULT_INDICATOR_CONFIGS, "EMA", true);

  assert.equal(configured.EMA.enabled, true);
  assert.equal(canEditIndicators(false), false);
  assert.equal(getIndicatorFallbackMessage(false), "히스토리 준비 후 indicator 표시");
});

test("indicator value rows expose active prices at the hovered candle", () => {
  const configs = setIndicatorEnabled(
    setIndicatorEnabled(DEFAULT_INDICATOR_CONFIGS, "SMA", true),
    "BOLLINGER",
    true
  );
  const candles = Array.from({ length: 20 }, (_, index) => ({
    close: 100 + index,
    time: (index + 1) as UTCTimestamp,
  }));

  const rows = calculateIndicatorValueRows(candles, configs, 20 as UTCTimestamp);

  assert.deepEqual(
    rows.map((row) => row.label),
    ["SMA20", "BB M20", "BB U20", "BB L20"]
  );
  assert.equal(rows[0].value, 109.5);
  assert.ok(rows[2].value > rows[1].value);
  assert.ok(rows[3].value < rows[1].value);
});

test("indicator value rows stay empty before a configured period has enough candles", () => {
  const configs = setIndicatorEnabled(DEFAULT_INDICATOR_CONFIGS, "EMA", true);
  const candles = Array.from({ length: 10 }, (_, index) => ({
    close: 100 + index,
    time: (index + 1) as UTCTimestamp,
  }));

  const rows = calculateIndicatorValueRows(candles, configs, 10 as UTCTimestamp);

  assert.deepEqual(rows, []);
});

test("indicator state remains local to FuturesPriceChart", () => {
  const source = readFileSync(
    path.join(__dirname, "FuturesPriceChart.tsx"),
    "utf8"
  );

  assert.equal(source.includes("useSearchParams"), false);
  assert.equal(source.includes("zustand"), false);
  assert.equal(source.includes("createJSONStorage"), false);
});
