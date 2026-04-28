import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const source = readFileSync(path.join(__dirname, "ClosePositionButton.tsx"), "utf8");

test("close position modal snapshots mark price only when opened", () => {
  assert.equal(source.includes("markPrice: number"), true);
  assert.equal(source.includes("snapshotMarkPrice"), true);
  assert.equal(source.includes("setSnapshotMarkPrice(markPrice)"), true);
  assert.equal(source.includes("setLimitPrice(markPrice.toFixed(1))"), true);
  assert.equal(source.includes("formatUsd(snapshotMarkPrice)"), true);
});
