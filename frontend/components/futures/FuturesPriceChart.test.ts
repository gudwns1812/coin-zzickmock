import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const source = readFileSync(path.join(__dirname, "FuturesPriceChart.tsx"), "utf8");

test("volume renders in a resizable lightweight-charts pane", () => {
  assert.equal(source.includes("const VOLUME_PANE_INDEX = 1"), true);
  assert.equal(source.includes("enableResize: true"), true);
  assert.equal(source.includes("HistogramSeries"), true);
  assert.equal(source.includes("VOLUME_PANE_INDEX"), true);
  assert.equal(source.includes("setStretchFactor"), true);
});

test("volume pane exposes a hovered candle volume legend", () => {
  assert.equal(source.includes("hoveredVolume"), true);
  assert.equal(source.includes("volume legend"), true);
  assert.equal(source.includes("formatVolume(displayedVolume)"), true);
});
