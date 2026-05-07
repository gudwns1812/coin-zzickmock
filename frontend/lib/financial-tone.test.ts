import assert from "node:assert/strict";
import test from "node:test";

const toneModule: typeof import("./financial-tone") = await import(
  new URL("./financial-tone.ts", import.meta.url).href
);

const {
  getSignedFinancialBadgeClassName,
  getSignedFinancialTextClassName,
  getSignedFinancialTone,
} = toneModule;

test("signed financial tone maps profit to green and loss to red", () => {
  assert.equal(getSignedFinancialTone(1), "positive");
  assert.equal(getSignedFinancialTextClassName(1), "text-emerald-600");

  assert.equal(getSignedFinancialTone(-1), "negative");
  assert.equal(getSignedFinancialTextClassName(-1), "text-main-red");

  assert.equal(getSignedFinancialTone(0), "neutral");
  assert.equal(getSignedFinancialTextClassName(0), "text-main-dark-gray");
});

test("signed financial badge colors follow the same sign semantics", () => {
  assert.match(getSignedFinancialBadgeClassName(1), /emerald/);
  assert.match(getSignedFinancialBadgeClassName(-1), /red/);
  assert.match(getSignedFinancialBadgeClassName(0), /main-dark-gray/);
});
