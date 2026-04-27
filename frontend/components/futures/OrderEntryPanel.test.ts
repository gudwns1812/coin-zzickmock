import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const source = readFileSync(path.join(__dirname, "OrderEntryPanel.tsx"), "utf8");

test("order entry panel does not expose disabled TP/SL controls", () => {
  assert.equal(source.includes('type="checkbox"'), false);
  assert.equal(source.includes("TP/SL"), false);
});

test("limit price is snapshot-initialized and not live-overwritten", () => {
  assert.equal(source.includes("priceSnapshotSymbolRef"), true);
  assert.equal(source.includes("priceSnapshotSymbolRef.current !== symbol"), true);
  assert.equal(source.includes("[currentPrice, symbol]"), true);
});
