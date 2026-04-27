import assert from "node:assert/strict";
import test from "node:test";

const countdownModule: typeof import("./funding-countdown") = await import(
  new URL("./funding-countdown.ts", import.meta.url).href
);

const { formatFundingCountdown } = countdownModule;

test("formats remaining funding time as hh:mm:ss", () => {
  assert.equal(
    formatFundingCountdown("2026-04-27T00:00:00Z", Date.parse("2026-04-26T23:59:30Z")),
    "00:00:30"
  );
});

test("clamps elapsed funding countdown to zero", () => {
  assert.equal(
    formatFundingCountdown("2026-04-27T00:00:00Z", Date.parse("2026-04-27T00:00:01Z")),
    "00:00:00"
  );
});

test("returns placeholder for missing or invalid funding timestamp", () => {
  assert.equal(formatFundingCountdown(null, 0), "--:--:--");
  assert.equal(formatFundingCountdown("not-a-date", 0), "--:--:--");
});

test("anchors countdown to server time plus elapsed client time", () => {
  assert.equal(
    formatFundingCountdown(
      "2026-04-27T08:00:00Z",
      Date.parse("2026-04-27T00:00:10Z"),
      "2026-04-27T07:59:30Z",
      Date.parse("2026-04-27T00:00:00Z")
    ),
    "00:00:20"
  );
});
