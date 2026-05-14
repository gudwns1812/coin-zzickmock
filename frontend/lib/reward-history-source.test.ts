import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const rootDir = path.join(__dirname, "..");

function readFrontendSource(relativePath: string): string {
  return readFileSync(path.join(rootDir, relativePath), "utf8");
}

test("shop history API uses the unified purchase and redemption endpoint", () => {
  const apiSource = readFrontendSource("lib/futures-api.ts");

  assert.match(apiSource, /type RewardShopHistoryKind/);
  assert.match(apiSource, /"INSTANT_PURCHASE"/);
  assert.match(apiSource, /"REDEMPTION_REQUEST"/);
  assert.match(apiSource, /function getRewardShopHistory/);
  assert.match(apiSource, /\/api\/futures\/shop\/history/);
});

test("redemption history renders instant purchases without cancel or contact details", () => {
  const historyClientSource = readFrontendSource(
    "components/rewards/RewardRedemptionHistoryClient.tsx"
  );

  assert.match(historyClientSource, /row\.kind === "INSTANT_PURCHASE"/);
  assert.match(historyClientSource, /즉시 구매/);
  assert.match(historyClientSource, /row\.submittedPhoneNumber \?\? "-"/);
  assert.match(
    historyClientSource,
    /row\.kind === "REDEMPTION_REQUEST" && row\.status === "PENDING"/
  );
});

test("point history maps internal source references to user-facing labels", () => {
  const pointsPageSource = readFrontendSource(
    "app/(main)/mypage/points/page.tsx"
  );

  assert.doesNotMatch(
    pointsPageSource,
    /history\.sourceType\}\s*·\s*\{history\.sourceReference/
  );
  assert.match(pointsPageSource, /sourceType === "INSTANT_SHOP_PURCHASE"/);
  assert.match(pointsPageSource, /상점 즉시 구매/);
  assert.match(pointsPageSource, /상점 상품 구매/);
});
