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

test("main routes render a loading boundary instead of flashing default content", () => {
  const loadingSource = readFrontendSource("app/(main)/loading.tsx");
  const authGateSource = readFrontendSource("components/router/BackendAuthGate.tsx");
  const marketsLandingSource = readFrontendSource(
    "components/router/(main)/markets/MarketsLandingRealtimeView.tsx"
  );
  const marketDetailSource = readFrontendSource(
    "components/futures/MarketDetailRealtimeView.tsx"
  );

  assert.match(loadingSource, /AppLoadingScreen/);
  assert.match(authGateSource, /<AppLoadingScreen/);

  for (const source of [marketsLandingSource, marketDetailSource]) {
    assert.match(source, /isAuthResolved/);
    assert.match(source, /setIsAuthResolved\(true\)/);
    assert.match(source, /<AppLoadingScreen/);
  }
});


test("protected routes provide route-specific auth fallbacks without mounting children", () => {
  const authGateSource = readFrontendSource("components/router/BackendAuthGate.tsx");
  const skeletonSource = readFrontendSource("components/ui/shared/ProtectedPageSkeleton.tsx");
  const protectedRouteFallbacks = [
    ["app/(main)/mypage/layout.tsx", "mypage"],
    ["app/(main)/admin/layout.tsx", "admin"],
    ["app/(main)/shop/page.tsx", "shop"],
    ["app/(main)/watchlist/page.tsx", "watchlist"],
    ["app/(main)/community/page.tsx", "community-list"],
    ["app/(main)/community/write/page.tsx", "community-editor"],
    ["app/(main)/community/[postId]/page.tsx", "community-detail"],
    ["app/(main)/community/[postId]/edit/page.tsx", "community-editor"],
  ] as const;

  assert.match(authGateSource, /fallback\?: ReactNode/);
  assert.match(authGateSource, /state === "checking"/);
  assert.match(authGateSource, /fallback \?\?/);
  assert.match(skeletonSource, /role="status"/);
  assert.match(skeletonSource, /aria-busy="true"/);

  for (const [routePath, variant] of protectedRouteFallbacks) {
    const source = readFrontendSource(routePath);

    assert.match(source, /ProtectedPageSkeleton/);
    assert.match(source, new RegExp(String.raw`fallback=\{[\s\S]*variant="${variant}"`));
  }
});
