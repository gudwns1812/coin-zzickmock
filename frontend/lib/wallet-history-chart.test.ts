import assert from "node:assert/strict";
import test from "node:test";

const chartModule: typeof import("./wallet-history-chart") = await import(
  new URL("./wallet-history-chart.ts", import.meta.url).href
);

const { buildWalletBalanceChartPoints } = chartModule;

test("wallet history chart points are sorted by snapshot date", () => {
  const points = buildWalletBalanceChartPoints([
    {
      snapshotDate: "2026-04-28",
      walletBalance: 101000,
      dailyWalletChange: 1500,
      recordedAt: "2026-04-28T00:00:00Z",
    },
    {
      snapshotDate: "2026-04-27",
      walletBalance: 99500,
      dailyWalletChange: -500,
      recordedAt: "2026-04-27T00:00:00Z",
    },
  ]);

  assert.equal(points[0].walletBalance, 99500);
  assert.equal(points[1].walletBalance, 101000);
});

test("wallet history chart labels use KST date labels", () => {
  const points = buildWalletBalanceChartPoints([
    {
      snapshotDate: "2026-04-28",
      walletBalance: 100000,
      dailyWalletChange: 0,
      recordedAt: "2026-04-27T15:30:00Z",
    },
  ]);

  assert.equal(points[0].label, "04. 28.");
});
