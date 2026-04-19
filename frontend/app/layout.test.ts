const assert = require("node:assert/strict");
const { readFileSync } = require("node:fs");
const path = require("node:path");
const test = require("node:test");

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
