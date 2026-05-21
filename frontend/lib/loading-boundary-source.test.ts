import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const rootDir = path.join(__dirname, "..");

function readFrontendSource(relativePath: string): string {
  return readFileSync(path.join(rootDir, relativePath), "utf8");
}

function hasFrontendSource(relativePath: string): boolean {
  return existsSync(path.join(rootDir, relativePath));
}

test("main route transitions do not show a visual loading interstitial", () => {
  const authGateSource = readFrontendSource("components/router/BackendAuthGate.tsx");
  const marketsLandingSource = readFrontendSource(
    "components/router/(main)/markets/MarketsLandingRealtimeView.tsx"
  );
  const marketDetailSource = readFrontendSource(
    "components/futures/MarketDetailRealtimeView.tsx"
  );

  assert.equal(hasFrontendSource("app/(main)/loading.tsx"), false);
  assert.equal(
    hasFrontendSource("components/ui/shared/PageTransitionSkeleton.tsx"),
    false
  );
  assert.doesNotMatch(authGateSource, /AppLoadingScreen/);
  assert.doesNotMatch(authGateSource, /ProtectedPageSkeleton/);
  assert.doesNotMatch(authGateSource, /로그인이 필요합니다/);

  for (const source of [marketsLandingSource, marketDetailSource]) {
    assert.doesNotMatch(source, /isAuthResolved/);
    assert.doesNotMatch(source, /setIsAuthResolved\(true\)/);
    assert.doesNotMatch(source, /<AppLoadingScreen/);
    assert.match(source, /useFuturesAuthUser/);
  }
});

test("public market routes keep static identity and degrade dynamic market fields", () => {
  const marketsPageSource = readFrontendSource("app/(main)/markets/page.tsx");
  const detailPageSource = readFrontendSource("app/(main)/markets/[symbol]/page.tsx");
  const watchlistPageSource = readFrontendSource("app/(main)/watchlist/page.tsx");
  const futuresApiSource = readFrontendSource("lib/futures-api.ts");
  const marketDetailSource = readFrontendSource(
    "components/futures/MarketDetailRealtimeView.tsx"
  );

  assert.match(marketsPageSource, /getFuturesMarketsResult/);
  assert.match(detailPageSource, /getFuturesMarketResult/);
  assert.match(watchlistPageSource, /getFuturesMarketsResult/);

  assert.match(marketsPageSource, /<MarketsLandingRealtimeView/);
  assert.doesNotMatch(marketsPageSource, /hasCompleteMarketData/);
  assert.doesNotMatch(marketsPageSource, /마켓 데이터가 아직 없습니다/);
  assert.match(detailPageSource, /isInitialMarketDataDegraded={isFallback}/);
  assert.doesNotMatch(detailPageSource, /마켓 데이터가 아직 없습니다/);
  assert.doesNotMatch(watchlistPageSource, /MARKET_SNAPSHOT_LIST/);
  assert.match(watchlistPageSource, /isFallback \? "" : formatUsd/);
  assert.match(futuresApiSource, /markets: SUPPORTED_MARKET_SNAPSHOT_TUPLE/);
  assert.match(futuresApiSource, /getFuturesMarketResult/);
  assert.match(marketDetailSource, /MarketChartShell/);
  assert.doesNotMatch(marketDetailSource, /외부 시세 수집에 실패/);
  assert.doesNotMatch(marketDetailSource, /실시간 시세 수신 대기/);
});


test("protected routes mount their page shells while auth is being checked", () => {
  const authGateSource = readFrontendSource("components/router/BackendAuthGate.tsx");
  const protectedRouteEntries = [
    "app/(main)/mypage/layout.tsx",
    "app/(main)/admin/layout.tsx",
    "app/(main)/shop/page.tsx",
    "app/(main)/watchlist/page.tsx",
    "app/(main)/community/page.tsx",
    "app/(main)/community/write/page.tsx",
    "app/(main)/community/[postId]/page.tsx",
    "app/(main)/community/[postId]/edit/page.tsx",
  ] as const;

  assert.match(authGateSource, /authQuery\.isLoading/);
  assert.match(authGateSource, /if \(!isChecking && !isAllowed\)/);
  assert.match(authGateSource, /return <PageReveal>\{children\}<\/PageReveal>/);
  assert.doesNotMatch(authGateSource, /fallback\?: ReactNode/);
  assert.doesNotMatch(authGateSource, /fallback \?\?/);

  for (const routePath of protectedRouteEntries) {
    const source = readFrontendSource(routePath);

    assert.match(source, /<BackendAuthGate/);
    assert.doesNotMatch(source, /ProtectedPageSkeleton/);
    assert.doesNotMatch(source, /fallback=/);
  }
});
