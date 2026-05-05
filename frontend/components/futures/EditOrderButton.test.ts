import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const source = readFileSync(path.join(__dirname, "EditOrderButton.tsx"), "utf8");

test("edit order modal submits a positive limit price through the modify API", () => {
  assert.equal(source.includes("주문 가격 수정"), true);
  assert.equal(source.includes("modifyFuturesOrderPrice(orderId, nextLimitPrice)"), true);
  assert.equal(source.includes("!Number.isFinite(nextLimitPrice) || nextLimitPrice <= 0"), true);
  assert.equal(source.includes("router.refresh()"), true);
  assert.equal(source.includes('type="number"'), true);
  assert.equal(source.includes('step="any"'), true);
});
