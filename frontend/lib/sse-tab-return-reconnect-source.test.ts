import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const frontendRoot = path.join(__dirname, "..");

function readFrontendSource(relativePath: string) {
  return readFileSync(path.join(frontendRoot, relativePath), "utf8");
}

test("markets landing isolates per-symbol SSE hooks in a child subscription", () => {
  const source = readFrontendSource(
    "components/router/(main)/markets/MarketsLandingRealtimeView.tsx"
  );

  assert.equal(source.includes("MarketLandingStreamSubscription"), true);
  assert.equal(source.includes("useResilientEventSource({"), true);
  assert.equal(source.includes("const streams = initialMarkets.map"), false);
  assert.equal(source.includes("new EventSource("), false);
});

test("markets landing can clear initial fallback after live stream recovery", () => {
  const source = readFrontendSource(
    "components/router/(main)/markets/MarketsLandingRealtimeView.tsx"
  );

  assert.equal(source.includes("recoveredStreamSymbols"), true);
  assert.equal(source.includes("isInitialFallbackRecovering"), true);
  assert.equal(
    source.includes(
      "initialMarkets.some((market) => !recoveredStreamSymbols[market.symbol])"
    ),
    true
  );
});

test("tab return reconnect policy force-reconnects instead of health-checking", () => {
  const source = readFrontendSource("hooks/resilientEventSourcePolicy.ts");

  assert.equal(source.includes("shouldForceReconnectOnVisible"), true);
  assert.equal(source.includes("return true;"), true);
});

test("candle stream invalidates futures candle queries by prefix", () => {
  const source = readFrontendSource("components/futures/FuturesPriceChart.tsx");

  assert.equal(source.includes("invalidateCurrentCandleQueries"), true);
  assert.equal(source.includes("exact: false"), true);
  assert.equal(
    source.includes('queryKey: ["futures-candles", symbol, selectedInterval]'),
    true
  );
});

test("frontend SSE route handlers abort upstream fetches on client disconnect", () => {
  const routeSources = [
    "app/api/futures/markets/[symbol]/stream/route.ts",
    "app/api/futures/markets/[symbol]/candles/stream/route.ts",
    "app/api/futures/orders/stream/route.ts",
  ].map(readFrontendSource);

  for (const source of routeSources) {
    assert.equal(source.includes("signal: request.signal"), true);
  }
});
