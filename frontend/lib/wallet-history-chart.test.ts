import assert from "node:assert/strict";
import test from "node:test";

const chartModule: typeof import("./wallet-history-chart") = await import(
  new URL("./wallet-history-chart.ts", import.meta.url).href
);

const { buildWalletBalanceChartPoints } = chartModule;

test("wallet history chart points are sorted by recorded time", () => {
  const points = buildWalletBalanceChartPoints([
    {
      walletBalance: 101000,
      availableMargin: 99000,
      sourceType: "POSITION_CLOSE",
      sourceReference: "order:2:close-fill",
      recordedAt: "2026-04-28T00:00:00Z",
    },
    {
      walletBalance: 99500,
      availableMargin: 94500,
      sourceType: "ORDER_FILL",
      sourceReference: "order:1:fill",
      recordedAt: "2026-04-27T00:00:00Z",
    },
  ]);

  assert.equal(points[0].walletBalance, 99500);
  assert.equal(points[1].walletBalance, 101000);
});

test("wallet history chart labels use KST date labels", () => {
  const points = buildWalletBalanceChartPoints([
    {
      walletBalance: 100000,
      availableMargin: 100000,
      sourceType: "CURRENT_SNAPSHOT",
      sourceReference: "account:demo:current",
      recordedAt: "2026-04-27T15:30:00Z",
    },
  ]);

  assert.equal(points[0].label, "04. 28.");
});
