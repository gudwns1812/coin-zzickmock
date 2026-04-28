import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const rootDir = path.join(__dirname, "..");

test("my page shell exposes admin navigation only behind the admin flag", () => {
  const shellSource = readFileSync(
    path.join(rootDir, "components/mypage/MyPageShell.tsx"),
    "utf8"
  );

  assert.match(shellSource, /isAdmin \? \(/);
  assert.match(shellSource, /href: "\/admin\/reward-redemptions"/);
  assert.match(shellSource, /href: "\/admin\/shop-items"/);
});

test("admin pages expose a return path to my page", () => {
  const redemptionsSource = readFileSync(
    path.join(rootDir, "components/rewards/AdminRewardRedemptionsClient.tsx"),
    "utf8"
  );
  const shopItemsSource = readFileSync(
    path.join(rootDir, "components/rewards/AdminShopItemsClient.tsx"),
    "utf8"
  );

  assert.match(redemptionsSource, /href="\/mypage"/);
  assert.match(shopItemsSource, /href="\/mypage"/);
});
