import assert from "node:assert/strict";
import test from "node:test";
import { TickMarkType, type UTCTimestamp } from "lightweight-charts";

const chartTimeModule: typeof import("./futuresChartTime") = await import(
  new URL("./futuresChartTime.ts", import.meta.url).href
);
const { formatChartTickInKst, formatChartTimeInKst } = chartTimeModule;

test("chart crosshair time is formatted in UTC+9", () => {
  assert.equal(
    formatChartTimeInKst((Date.parse("2026-04-24T15:01:02.000Z") / 1000) as UTCTimestamp),
    "2026-04-25 00:01:02 UTC+9"
  );
});

test("chart time-axis ticks are formatted in UTC+9", () => {
  const timestamp = (Date.parse("2026-04-24T15:01:02.000Z") / 1000) as UTCTimestamp;

  assert.equal(formatChartTickInKst(timestamp, TickMarkType.DayOfMonth), "04/25");
  assert.equal(formatChartTickInKst(timestamp, TickMarkType.Time), "00:01");
});
