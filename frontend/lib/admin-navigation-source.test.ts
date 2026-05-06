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

test("my page shell exposes only one admin entry behind the admin flag", () => {
  const shellSource = readFrontendSource("components/mypage/MyPageShell.tsx");

  assert.match(shellSource, /isAdmin \? \(/);
  assert.match(shellSource, /href="\/admin"/);
  assert.doesNotMatch(shellSource, /href: "\/admin\/reward-redemptions"/);
  assert.doesNotMatch(shellSource, /href: "\/admin\/shop-items"/);
  assert.match(shellSource, /교환 내역/);
});

test("admin pages navigate through the admin hub", () => {
  const redemptionsSource = readFrontendSource(
    "components/rewards/AdminRewardRedemptionsClient.tsx"
  );
  const shopItemsSource = readFrontendSource(
    "components/rewards/AdminShopItemsClient.tsx"
  );
  const adminHubSource = readFrontendSource("app/(main)/admin/page.tsx");

  assert.match(redemptionsSource, /href="\/admin"/);
  assert.match(shopItemsSource, /href="\/admin"/);
  assert.match(adminHubSource, /href="\/admin\/reward-redemptions"/);
  assert.match(adminHubSource, /href="\/admin\/shop-items"/);
  assert.match(adminHubSource, /const token = await getJwtToken\(\);/);
  assert.match(adminHubSource, /redirect\("\/login"\);/);
  assert.match(adminHubSource, /const authUser = await getAuthUser\(\);/);
  assert.match(
    adminHubSource,
    /authUser\?\.admin \?\? token\?\.admin \?\? token\?\.role === "ADMIN"/
  );
});

test("shop page no longer fetches redemption history", () => {
  const shopPageSource = readFrontendSource("app/(main)/shop/page.tsx");

  assert.doesNotMatch(shopPageSource, /getRewardRedemptions/);
});

test("exchange history uses user-facing labels and routes", () => {
  const shopClientSource = readFrontendSource(
    "components/rewards/ShopRedemptionClient.tsx"
  );
  const redemptionsPageSource = readFrontendSource(
    "app/(main)/mypage/redemptions/page.tsx"
  );

  assert.match(shopClientSource, /href="\/mypage\/redemptions"/);
  assert.match(shopClientSource, /교환 내역/);
  assert.match(redemptionsPageSource, /교환 내역/);
  assert.match(redemptionsPageSource, /구매\/교환 내역/);
});
