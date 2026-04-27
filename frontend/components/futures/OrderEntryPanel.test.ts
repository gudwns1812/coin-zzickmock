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
