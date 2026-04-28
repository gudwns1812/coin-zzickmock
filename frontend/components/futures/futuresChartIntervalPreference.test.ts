import assert from "node:assert/strict";
import test from "node:test";

const preferenceModule: typeof import("./futuresChartIntervalPreference") =
  await import(new URL("./futuresChartIntervalPreference.ts", import.meta.url).href);

const {
  DEFAULT_FUTURES_CHART_INTERVAL,
  FUTURES_CHART_INTERVAL_STORAGE_KEY,
  isFuturesCandleInterval,
  readStoredFuturesChartInterval,
  writeStoredFuturesChartInterval,
} = preferenceModule;

test("futures chart interval preference validates supported intervals", () => {
  assert.equal(DEFAULT_FUTURES_CHART_INTERVAL, "1m");
  assert.equal(isFuturesCandleInterval("1h"), true);
  assert.equal(isFuturesCandleInterval("2h"), false);
  assert.equal(isFuturesCandleInterval(null), false);
});

test("futures chart interval preference reads only valid stored values", () => {
  assert.equal(
    readStoredFuturesChartInterval(storageWithValue("4h")),
    "4h"
  );
  assert.equal(readStoredFuturesChartInterval(storageWithValue("2h")), null);
  assert.equal(readStoredFuturesChartInterval(null), null);
});

test("futures chart interval preference writes selected interval", () => {
  const writes: Array<[string, string]> = [];

  writeStoredFuturesChartInterval("12h", {
    setItem(key, value) {
      writes.push([key, value]);
    },
  });

  assert.deepEqual(writes, [[FUTURES_CHART_INTERVAL_STORAGE_KEY, "12h"]]);
});

function storageWithValue(value: string) {
  return {
    getItem(key: string) {
      assert.equal(key, FUTURES_CHART_INTERVAL_STORAGE_KEY);
      return value;
    },
  };
}
