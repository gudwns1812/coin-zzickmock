import assert from "node:assert/strict";
import test from "node:test";

const liveCandlesModule: typeof import("./futuresLiveCandles") = await import(
  new URL("./futuresLiveCandles.ts", import.meta.url).href
);
const { getLiveCandleBucket, mergeCandlesWithLivePrice } = liveCandlesModule;

const baseCandle = {
  closePrice: 100,
  closeTime: "2026-04-24T02:13:00.000Z",
  highPrice: 101,
  lowPrice: 99,
  openPrice: 100,
  openTime: "2026-04-24T02:12:00.000Z",
  volume: 7,
};

test("live 1m price updates the current minute candle close high and low", () => {
  const merged = mergeCandlesWithLivePrice(
    [baseCandle],
    "1m",
    103,
    Date.parse("2026-04-24T02:12:40.000Z")
  );

  assert.equal(merged.length, 1);
  assert.equal(merged[0].openTime, "2026-04-24T02:12:00.000Z");
  assert.equal(merged[0].openPrice, 100);
  assert.equal(merged[0].closePrice, 103);
  assert.equal(merged[0].highPrice, 103);
  assert.equal(merged[0].lowPrice, 99);
  assert.equal(merged[0].volume, 7);
});

test("live 1m price appends a new candle after the minute boundary", () => {
  const merged = mergeCandlesWithLivePrice(
    [baseCandle],
    "1m",
    102,
    Date.parse("2026-04-24T02:13:03.000Z")
  );

  assert.equal(merged.length, 2);
  assert.equal(merged[1].openTime, "2026-04-24T02:13:00.000Z");
  assert.equal(merged[1].closeTime, "2026-04-24T02:14:00.000Z");
  assert.equal(merged[1].openPrice, 100);
  assert.equal(merged[1].closePrice, 102);
  assert.equal(merged[1].highPrice, 102);
  assert.equal(merged[1].lowPrice, 100);
});

test("live 1h price updates and appends hourly candles on hour boundaries", () => {
  const hourly = {
    ...baseCandle,
    closeTime: "2026-04-24T03:00:00.000Z",
    openTime: "2026-04-24T02:00:00.000Z",
  };
  const updated = mergeCandlesWithLivePrice(
    [hourly],
    "1h",
    96,
    Date.parse("2026-04-24T02:55:00.000Z")
  );
  const appended = mergeCandlesWithLivePrice(
    updated,
    "1h",
    104,
    Date.parse("2026-04-24T03:00:01.000Z")
  );

  assert.equal(updated[0].closePrice, 96);
  assert.equal(updated[0].lowPrice, 96);
  assert.equal(appended.length, 2);
  assert.equal(appended[1].openTime, "2026-04-24T03:00:00.000Z");
  assert.equal(appended[1].closeTime, "2026-04-24T04:00:00.000Z");
  assert.equal(appended[1].openPrice, 96);
  assert.equal(appended[1].closePrice, 104);
});

test("calendar bucket helpers use UTC week and month starts", () => {
  assert.deepEqual(getLiveCandleBucket("1W", Date.parse("2026-04-26T12:00:00.000Z")), {
    closeTimeMs: Date.parse("2026-04-27T00:00:00.000Z"),
    openTimeMs: Date.parse("2026-04-20T00:00:00.000Z"),
  });
  assert.deepEqual(getLiveCandleBucket("1M", Date.parse("2026-04-24T12:00:00.000Z")), {
    closeTimeMs: Date.parse("2026-05-01T00:00:00.000Z"),
    openTimeMs: Date.parse("2026-04-01T00:00:00.000Z"),
  });
});
