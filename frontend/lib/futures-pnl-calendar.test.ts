import assert from "node:assert/strict";
import test from "node:test";

const calendarModule: typeof import("./futures-pnl-calendar") = await import(
  new URL("./futures-pnl-calendar.ts", import.meta.url).href
);

const { buildDailyWalletChanges, buildKstMonthCalendar, toKstDateKey } =
  calendarModule;

test("builds daily wallet changes from wallet history snapshots", () => {
  const grouped = buildDailyWalletChanges([
    {
      snapshotDate: "2026-04-28",
      dailyWalletChange: -3,
    },
    {
      snapshotDate: "2026-04-27",
      dailyWalletChange: 10,
    },
  ]);

  assert.deepEqual(grouped, [
    { dateKey: "2026-04-27", dailyWalletChange: 10 },
    { dateKey: "2026-04-28", dailyWalletChange: -3 },
  ]);
});

test("builds a complete KST month calendar grid", () => {
  const cells = buildKstMonthCalendar(new Date("2026-04-28T00:00:00Z"), [
    { dateKey: "2026-04-28", dailyWalletChange: 12.5 },
  ]);
  const april28 = cells.find((cell) => cell.dateKey === "2026-04-28");

  assert.equal(toKstDateKey("2026-04-27T15:00:00Z"), "2026-04-28");
  assert.equal(cells.length % 7, 0);
  assert.equal(april28?.inMonth, true);
  assert.equal(april28?.dailyWalletChange, 12.5);
});
