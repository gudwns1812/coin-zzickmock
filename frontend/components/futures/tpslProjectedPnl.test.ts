import assert from "node:assert/strict";
import test from "node:test";

const { calculateTpslProjectedPnl }: typeof import("./tpslProjectedPnl") =
  await import(new URL("./tpslProjectedPnl.ts", import.meta.url).href);

test("LONG TP/SL projected PnL uses trigger price minus entry price", () => {
  const position = {
    entryPrice: 100,
    positionSide: "LONG" as const,
    quantity: 2,
  };

  assert.equal(calculateTpslProjectedPnl("125", position), 50);
  assert.equal(calculateTpslProjectedPnl("90", position), -20);
});

test("SHORT TP/SL projected PnL uses entry price minus trigger price", () => {
  const position = {
    entryPrice: 100,
    positionSide: "SHORT" as const,
    quantity: 2,
  };

  assert.equal(calculateTpslProjectedPnl("80", position), 40);
  assert.equal(calculateTpslProjectedPnl("110", position), -20);
});

test("TP/SL projected PnL omits empty or invalid input", () => {
  const position = {
    entryPrice: 100,
    positionSide: "LONG" as const,
    quantity: 2,
  };

  assert.equal(calculateTpslProjectedPnl("", position), null);
  assert.equal(calculateTpslProjectedPnl("   ", position), null);
  assert.equal(calculateTpslProjectedPnl("0", position), null);
  assert.equal(calculateTpslProjectedPnl("-1", position), null);
  assert.equal(calculateTpslProjectedPnl("not-a-price", position), null);
});
