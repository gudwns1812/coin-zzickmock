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

const sseRoutePaths = [
  "app/api/futures/markets/[symbol]/stream/route.ts",
  "app/api/futures/markets/stream/route.ts",
  "app/api/futures/markets/summary/stream/route.ts",
  "app/api/futures/markets/[symbol]/candles/stream/route.ts",
  "app/api/futures/orders/stream/route.ts",
] as const;

test("markets landing opens one summary SSE through a child subscription", () => {
  const source = readFrontendSource(
    "components/router/(main)/markets/MarketsLandingRealtimeView.tsx"
  );

  assert.equal(source.includes("MarketLandingStreamSubscription"), true);
  assert.equal(source.includes("useResilientEventSource({"), true);
  assert.equal(source.includes("createMarketSummarySseUrl"), true);
  assert.equal(source.includes("/api/futures/markets/${encodeURIComponent(symbol)}/stream"), false);
  assert.equal(source.includes("initialMarkets.map((market) => ("), false);
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

test("public market SSE consumers can bypass Vercel route handlers", () => {
  const urlSource = readFrontendSource("lib/futures-sse-url.ts");
  const detailSource = readFrontendSource("components/futures/MarketDetailRealtimeView.tsx");
  const landingSource = readFrontendSource(
    "components/router/(main)/markets/MarketsLandingRealtimeView.tsx"
  );

  assert.equal(urlSource.includes("NEXT_PUBLIC_FUTURES_API_BASE_URL"), true);
  assert.equal(urlSource.includes("/markets/stream"), true);
  assert.equal(urlSource.includes("/markets/summary/stream"), true);
  assert.equal(urlSource.includes("/orders/stream"), true);
  assert.equal(detailSource.includes("createUnifiedMarketSseUrl"), true);
  assert.equal(detailSource.includes("createOrderExecutionSseUrl"), true);
  assert.equal(landingSource.includes("createMarketSummarySseUrl"), true);
});

test("event sources include credentials for direct backend SSE", () => {
  const hookSource = readFrontendSource("hooks/useResilientEventSource.ts");

  assert.equal(hookSource.includes("new EventSource(url, { withCredentials: true })"), true);
});

test("client login seeds backend-domain and same-origin auth cookies", () => {
  const authSource = readFrontendSource("lib/futures-auth-client.ts");
  const pageLoginSource = readFrontendSource("app/login/LoginFormClient.tsx");
  const headerLoginSource = readFrontendSource("components/ui/shared/header/LoginForm.tsx");

  assert.equal(authSource.includes('createFuturesBackendApiUrl("/auth/login")'), true);
  assert.equal(authSource.includes('"/proxy/auth/login"'), true);
  assert.equal(authSource.includes("credentials: \"include\""), true);
  assert.equal(pageLoginSource.includes("loginToFutures"), true);
  assert.equal(headerLoginSource.includes("loginToFutures"), true);
});

test("frontend SSE routes fail missing or blank clientKey before proxying", () => {
  const routeSources = sseRoutePaths.map(readFrontendSource);

  for (const source of routeSources) {
    assert.equal(source.includes("readRequiredSseClientKey(request.url)"), true);
    assert.match(
      source,
      /if \(!clientKey\) \{\s*return new Response\("Missing SSE client key", \{\s*status: 400,?\s*\}\);\s*\}/,
      "missing or blank clientKey must resolve to the dedicated 400 response"
    );
    assert.equal(source.indexOf("Missing SSE client key") < source.indexOf("proxySseStream({"), true);
  }
});

test("frontend SSE routes forward normalized clientKey to upstream", () => {
  const routeSources = sseRoutePaths.map(readFrontendSource);

  for (const source of routeSources) {
    assert.equal(source.includes("Missing SSE client key"), true);
    assert.equal(source.includes("SSE_CLIENT_KEY_PARAM"), true);
  }
});

test("useResilientEventSource appends tab clientKey at the shared hook boundary", () => {
  const source = readFrontendSource("hooks/useResilientEventSource.ts");

  assert.equal(source.includes("useSseClientKey"), true);
  assert.equal(source.includes("appendSseClientKey"), true);
  assert.equal(source.includes("eventSourceFactory(nextUrl)"), true);
});

test("useResilientEventSource reconnectKey reopens without changing the URL", () => {
  const source = readFrontendSource("hooks/useResilientEventSource.ts");

  assert.equal(source.includes("type EventSourceReconnectKey"), true);
  assert.equal(source.includes("reconnectKey?: EventSourceReconnectKey"), true);
  assert.equal(source.includes("reconnectKey = null"), true);
  assert.match(
    source,
    /useMemo\(\s*\(\) => \(url \? appendSseClientKey\(url, sseClientKey\) : null\),\s*\[sseClientKey, url\]\s*\)/
  );
  assert.match(
    source,
    /reconnectKey,\s*updateStatus,\s*eventSourceUrl,/
  );
});

test("frontend SSE consumers keep plain stream URLs and rely on the hook boundary", () => {
  const sources = [
    "components/router/(main)/markets/MarketsLandingRealtimeView.tsx",
    "components/futures/MarketDetailRealtimeView.tsx",
    "components/futures/FuturesPriceChart.tsx",
  ].map(readFrontendSource);

  for (const source of sources) {
    assert.equal(source.includes("useSseClientKey"), false);
    assert.equal(source.includes("appendSseClientKey"), false);
  }

  const detailSource = readFrontendSource("components/futures/MarketDetailRealtimeView.tsx");
  assert.equal(detailSource.includes("createUnifiedMarketSseUrl"), true);
  assert.equal(detailSource.includes("selectedInterval"), true);
  assert.equal(detailSource.includes("viewer:"), false);
  assert.equal(detailSource.includes("symbols:"), false);
});
