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

const protectedServerRoutes = [
  "app/(main)/markets/page.tsx",
  "app/(main)/markets/[symbol]/page.tsx",
  "app/(main)/mypage/page.tsx",
  "app/(main)/mypage/assets/page.tsx",
  "app/(main)/mypage/points/page.tsx",
  "app/(main)/mypage/redemptions/page.tsx",
  "app/(main)/shop/page.tsx",
  "app/(main)/community/page.tsx",
  "app/(main)/community/[postId]/page.tsx",
  "app/(main)/community/[postId]/edit/page.tsx",
  "app/(main)/admin/reward-redemptions/page.tsx",
  "app/(main)/admin/shop-items/page.tsx",
];

const personalizedServerHelpers = [
  "getFuturesAccountSummary",
  "getAccountRefillStatus",
  "getFuturesWalletHistory",
  "getFuturesPositions",
  "getFuturesOpenOrders",
  "getFuturesOrderHistory",
  "getFuturesPositionHistory",
  "getFuturesReward",
  "getRewardPointHistory",
  "getRewardRedemptions",
  "getRewardShopHistory",
  "getAdminRewardRedemptions",
  "getAdminShopItems",
  "getCommunityPosts",
  "getCommunityPost",
  "getCommunityPostForEdit",
  "getCommunityComments",
];

test("protected server routes do not import personalized futures-api runtime helpers", () => {
  for (const routePath of protectedServerRoutes) {
    const source = readFrontendSource(routePath);
    for (const helper of personalizedServerHelpers) {
      assert.doesNotMatch(
        source,
        new RegExp(`\\b${helper}\\b`),
        `${routePath} must not import or call ${helper}`
      );
    }
  }
});

test("personalized reads live in the client API boundary with credentialed requests", () => {
  const clientApiSource = readFrontendSource("lib/futures-client-api.ts");
  const requiredClientHelpers = [
    "getFuturesAccountSummaryClient",
    "getFuturesPositionsClient",
    "getFuturesOpenOrdersClient",
    "getFuturesRewardClient",
    "getShopItemsClient",
    "getAdminRewardRedemptionsClient",
    "getCommunityPostsClient",
  ];

  for (const helper of requiredClientHelpers) {
    assert.match(clientApiSource, new RegExp(`function ${helper}`));
  }
  assert.match(clientApiSource, /credentials: "include"/);
  assert.match(clientApiSource, /readClientApiData\(response, payload\)/);
});

test("auth change events clear personalized react-query caches", () => {
  const providerSource = readFrontendSource("components/router/QueryClientProvider.tsx");
  const queryKeysSource = readFrontendSource("lib/futures-query-keys.ts");

  assert.match(providerSource, /FUTURES_AUTH_CHANGED_EVENT/);
  assert.match(providerSource, /client\.removeQueries\(\{ queryKey \}\)/);
  assert.match(queryKeysSource, /personalizedQueryKeyPrefixes/);
  assert.match(queryKeysSource, /futuresQueryKeys\.admin/);
  assert.match(queryKeysSource, /futuresQueryKeys\.community/);
});
