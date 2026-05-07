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

test("tab return reconnect policy keeps recently healthy streams open", () => {
  const source = readFrontendSource("hooks/resilientEventSourcePolicy.ts");

  assert.equal(source.includes("shouldForceReconnectOnVisible"), true);
  assert.equal(
    source.includes("EVENT_SOURCE_VISIBLE_RECONNECT_AFTER_HIDDEN_MS"),
    true
  );
  assert.equal(
    source.includes("export function shouldForceReconnectOnVisible()"),
    false
  );
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

test("frontend SSE route handlers use the cancellable SSE proxy", () => {
  const routeSources = [
    "app/api/futures/markets/[symbol]/stream/route.ts",
    "app/api/futures/markets/[symbol]/candles/stream/route.ts",
    "app/api/futures/orders/stream/route.ts",
  ].map(readFrontendSource);

  for (const source of routeSources) {
    assert.equal(source.includes("proxySseStream({"), true);
    assert.equal(source.includes("createSseUpstreamHeaders(request)"), true);
  }

  const proxySource = readFrontendSource("lib/sse-proxy.ts");

  assert.equal(proxySource.includes("new AbortController()"), true);
  assert.equal(proxySource.includes("reader?.cancel()"), true);
  assert.equal(proxySource.includes("start(controller)"), true);
  assert.equal(proxySource.includes("async cancel()"), true);
});

test("frontend SSE routes require and forward clientKey", () => {
  const routeSources = [
    "app/api/futures/markets/[symbol]/stream/route.ts",
    "app/api/futures/markets/[symbol]/candles/stream/route.ts",
    "app/api/futures/orders/stream/route.ts",
  ].map(readFrontendSource);

  for (const source of routeSources) {
    assert.equal(source.includes("readRequiredSseClientKey(request.url)"), true);
    assert.equal(source.includes("Missing SSE client key"), true);
    assert.equal(source.includes("SSE_CLIENT_KEY_PARAM"), true);
  }
});

test("frontend SSE consumers append tab clientKey through the shared helper", () => {
  const sources = [
    "components/router/(main)/markets/MarketsLandingRealtimeView.tsx",
    "components/futures/MarketDetailRealtimeView.tsx",
    "components/futures/FuturesPriceChart.tsx",
  ].map(readFrontendSource);

  for (const source of sources) {
    assert.equal(source.includes("useSseClientKey"), true);
    assert.equal(source.includes("appendSseClientKey"), true);
  }
});
