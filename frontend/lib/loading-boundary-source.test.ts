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
