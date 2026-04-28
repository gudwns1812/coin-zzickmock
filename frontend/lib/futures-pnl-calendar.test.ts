import assert from "node:assert/strict";
import test from "node:test";

const calendarModule: typeof import("./futures-pnl-calendar") = await import(
  new URL("./futures-pnl-calendar.ts", import.meta.url).href
);

const { buildKstMonthCalendar, groupDailyNetRealizedPnl, toKstDateKey } =
  calendarModule;

test("groups realized pnl by KST day", () => {
  const grouped = groupDailyNetRealizedPnl([
    {
      closedAt: "2026-04-27T14:59:59Z",
      netRealizedPnl: 10,
    },
    {
      closedAt: "2026-04-27T15:00:00Z",
      netRealizedPnl: -3,
    },
    {
      closedAt: "2026-04-27T23:00:00Z",
      netRealizedPnl: 7,
    },
  ]);

  assert.deepEqual(grouped, [
    { dateKey: "2026-04-27", netRealizedPnl: 10 },
    { dateKey: "2026-04-28", netRealizedPnl: 4 },
  ]);
});

test("builds a complete KST month calendar grid", () => {
  const cells = buildKstMonthCalendar(new Date("2026-04-28T00:00:00Z"), [
    { dateKey: "2026-04-28", netRealizedPnl: 12.5 },
  ]);
  const april28 = cells.find((cell) => cell.dateKey === "2026-04-28");

  assert.equal(toKstDateKey("2026-04-27T15:00:00Z"), "2026-04-28");
  assert.equal(cells.length % 7, 0);
  assert.equal(april28?.inMonth, true);
  assert.equal(april28?.netRealizedPnl, 12.5);
});
