import assert from "node:assert/strict";
import test from "node:test";

const realtimeCandlesModule: typeof import("./futuresRealtimeCandles") =
  await import(new URL("./futuresRealtimeCandles.ts", import.meta.url).href);
const { mergeCandlesWithRealtimeCandle } = realtimeCandlesModule;

const baseCandle = {
  closePrice: 100,
  closeTime: "2026-04-24T02:13:00.000Z",
  highPrice: 101,
  lowPrice: 99,
  openPrice: 100,
  openTime: "2026-04-24T02:12:00.000Z",
  volume: 7,
};

test("backend realtime candle replaces the matching historical bucket", () => {
  const merged = mergeCandlesWithRealtimeCandle([baseCandle], {
    ...baseCandle,
    closePrice: 103,
    highPrice: 104,
    lowPrice: 98,
    volume: 12,
  });

  assert.equal(merged.length, 1);
  assert.equal(merged[0].openTime, "2026-04-24T02:12:00.000Z");
  assert.equal(merged[0].closePrice, 103);
  assert.equal(merged[0].highPrice, 104);
  assert.equal(merged[0].lowPrice, 98);
  assert.equal(merged[0].volume, 12);
});

test("backend realtime candle appends aggregated intervals without local bucketing", () => {
  const merged = mergeCandlesWithRealtimeCandle([baseCandle], {
    closePrice: 109,
    closeTime: "2026-04-24T02:18:00.000Z",
    highPrice: 110,
    lowPrice: 101,
    openPrice: 102,
    openTime: "2026-04-24T02:15:00.000Z",
    volume: 30,
  });

  assert.equal(merged.length, 2);
  assert.equal(merged[1].openTime, "2026-04-24T02:15:00.000Z");
  assert.equal(merged[1].closeTime, "2026-04-24T02:18:00.000Z");
  assert.equal(merged[1].openPrice, 102);
  assert.equal(merged[1].closePrice, 109);
  assert.equal(merged[1].volume, 30);
});

test("backend realtime candle is sorted with older history", () => {
  const merged = mergeCandlesWithRealtimeCandle([baseCandle], {
    closePrice: 97,
    closeTime: "2026-04-24T02:12:00.000Z",
    highPrice: 100,
    lowPrice: 95,
    openPrice: 99,
    openTime: "2026-04-24T02:11:00.000Z",
    volume: 9,
  });

  assert.deepEqual(
    merged.map((candle) => candle.openTime),
    ["2026-04-24T02:11:00.000Z", "2026-04-24T02:12:00.000Z"]
  );
});
