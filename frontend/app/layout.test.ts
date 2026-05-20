import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const layoutPath = path.join(__dirname, "layout.tsx");

test("root layout does not wire the global investment survey", () => {
  const layoutSource = readFileSync(layoutPath, "utf8");

  assert.equal(
    layoutSource.includes("InvestSurveyProvider"),
    false,
    "RootLayout should not import or render InvestSurveyProvider."
  );
  assert.equal(
    layoutSource.includes("getJwtToken"),
    false,
    "RootLayout should not read JWT just to decide whether to open the survey."
  );
});
