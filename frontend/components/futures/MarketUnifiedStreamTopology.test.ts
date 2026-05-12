import assert from "node:assert/strict";
import { existsSync, readFileSync, readdirSync, statSync } from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const frontendRoot = path.resolve(__dirname, "../..");

function read(relativePath: string) {
  const absolutePath = path.join(frontendRoot, relativePath);

  assert.equal(existsSync(absolutePath), true, `Expected file to exist: ${relativePath}`);

  return readFileSync(absolutePath, "utf8");
}

function collectSourceFiles(relativeDirectory: string): string[] {
  const absoluteDirectory = path.join(frontendRoot, relativeDirectory);

  if (!existsSync(absoluteDirectory)) {
    return [];
  }

  return readdirSync(absoluteDirectory).flatMap((entry) => {
    const absolutePath = path.join(absoluteDirectory, entry);
    const relativePath = path.join(relativeDirectory, entry);
    const stat = statSync(absolutePath);

    if (stat.isDirectory()) {
      if (entry === "node_modules" || entry === ".next") {
        return [];
      }

      return collectSourceFiles(relativePath);
    }

    return /\.(ts|tsx)$/.test(entry) && !/\.test\.(ts|tsx)$/.test(entry) ? [relativePath] : [];
  });
}

test("market detail opens public-compatible unified market SSE and keeps order execution SSE separate", () => {
  const source = read("components/futures/MarketDetailRealtimeView.tsx");

  assert.equal(source.includes("/api/futures/markets/stream"), true);
  assert.equal(source.includes("/api/futures/markets/${encodeURIComponent(initialMarket.symbol)}/stream"), false);
  assert.equal(source.includes("/candles/stream?"), false);
  assert.equal(source.includes("/api/futures/orders/stream"), true);
  assert.equal(source.includes("useResilientEventSource"), true);
  assert.equal(source.includes("clientKey"), true);
  assert.equal(source.includes("viewer:"), false);
  assert.equal(source.includes("symbols:"), false);
  assert.equal(source.includes("isAuthenticated, selectedInterval"), false);
  assert.equal(source.includes("MARKET_SUMMARY"), true);
  assert.equal(source.includes("MARKET_CANDLE"), true);
  assert.equal(source.includes("MARKET_HISTORY_FINALIZED"), true);
  assert.match(
    source,
    /useResilientEventSource\(\{\s*onMessage: handleMarketStreamMessage,\s*reconnectKey: isAuthenticated \? "authenticated" : "anonymous",\s*url: marketStreamUrl,/,
    "unified market stream must reopen when auth identity changes without changing the URL"
  );
  assert.equal(source.includes("handlePublicMarketSummaryMessage"), false);
  assert.equal(source.includes("handlePublicMarketCandleMessage"), false);
  assert.match(
    source,
    /useResilientEventSource\(\{\s*enabled: isAuthenticated,\s*onMessage: handleOrderStreamMessage,[\s\S]*?url: orderStreamUrl,/,
    "order execution stream must remain gated by authentication"
  );
});

test("off-screen order execution events refresh trading state without showing a symbol-scoped panel", () => {
  const source = read("components/futures/MarketDetailRealtimeView.tsx");
  const refreshIndex = source.indexOf(
    "refreshTradingStateFromOrderStream({ force: true });"
  );
  const offscreenReturnIndex = source.indexOf("if (!isCurrentSymbolExecution)");
  const displayIndex = source.indexOf("setExecutionEvents((current)");

  assert.notEqual(refreshIndex, -1);
  assert.notEqual(offscreenReturnIndex, -1);
  assert.notEqual(displayIndex, -1);
  assert.equal(refreshIndex < offscreenReturnIndex, true);
  assert.equal(offscreenReturnIndex < displayIndex, true);
  assert.equal(source.includes("if (data.symbol !== initialMarket.symbol)"), false);
});

test("futures price chart consumes parent unified candle events instead of opening its own SSE", () => {
  const source = read("components/futures/FuturesPriceChart.tsx");

  assert.equal(source.includes("/candles/stream?"), false);
  assert.equal(source.includes("useResilientEventSource"), false);
  assert.equal(source.includes("latestUnifiedCandle") || source.includes("realtimeCandle"), true);
  assert.equal(source.includes("onIntervalChange") || source.includes("selectedInterval"), true);
  assert.equal(source.includes("MARKET_HISTORY_FINALIZED") || source.includes("historyFinalized"), true);
});

test("unified market stream proxy route validates query and forwards to backend SSE", () => {
  const source = read("app/api/futures/markets/stream/route.ts");

  assert.equal(source.includes("clientKey"), true);
  assert.equal(source.includes("symbol"), true);
  assert.equal(source.includes("interval"), true);
  assert.equal(source.includes("viewer"), false);
  assert.equal(source.includes("symbols"), false);
  assert.equal(source.includes("400"), true);
  assert.equal(source.includes("createSseUpstreamHeaders"), true);
  assert.equal(source.includes("proxySseStream"), true);
  assert.equal(source.includes("/api/futures/markets/stream"), true);
  assert.equal(source.includes("response.json()"), false);
});

test("market landing summary stream proxy forwards multi-symbol SSE without interval", () => {
  const source = read("app/api/futures/markets/summary/stream/route.ts");
  const landingSource = read("components/router/(main)/markets/MarketsLandingRealtimeView.tsx");

  assert.equal(source.includes("clientKey"), true);
  assert.equal(source.includes("symbols"), true);
  assert.equal(source.includes("interval"), false);
  assert.equal(source.includes("400"), true);
  assert.equal(source.includes("createSseUpstreamHeaders"), true);
  assert.equal(source.includes("proxySseStream"), true);
  assert.equal(source.includes("/api/futures/markets/summary/stream"), true);
  assert.equal(source.includes("response.json()"), false);
  assert.equal(landingSource.includes("/api/futures/markets/summary/stream"), true);
  assert.equal(landingSource.includes("initialMarkets.map((market) => ("), false);
});

test("frontend has a parser/upsert boundary for unified market envelopes", () => {
  const candidateFiles = [
    ...collectSourceFiles("lib"),
    ...collectSourceFiles("components/futures"),
  ];
  const parserSources = candidateFiles
    .filter((file) => /market.*stream|stream.*market|envelope|realtime/i.test(file))
    .map((file) => [file, read(file)] as const)
    .filter(([, source]) =>
      source.includes("MARKET_SUMMARY") &&
      source.includes("MARKET_CANDLE") &&
      source.includes("MARKET_HISTORY_FINALIZED")
    );

  assert.ok(parserSources.length > 0, "Expected a parser/routing module for unified market envelopes");

  const combined = parserSources.map(([, source]) => source).join("\n");
  assert.equal(combined.includes("serverTime"), true);
  assert.equal(combined.includes("source"), true);
  assert.equal(combined.includes("INITIAL_SNAPSHOT"), true);
  assert.equal(combined.includes("LIVE"), true);
  assert.equal(combined.includes("openTime"), true);
  assert.equal(combined.includes("symbol"), true);
  assert.equal(combined.includes("interval"), true);
  assert.equal(combined.includes("parsePublicMarketSummaryEvent"), true);
  assert.equal(combined.includes("parsePublicMarketCandleEvent"), true);
});

test("live position display can re-mark non-selected positions from matching summary snapshots", () => {
  const detailSource = read("components/futures/MarketDetailRealtimeView.tsx");
  const displaySource = read("components/futures/livePositionDisplay.ts");

  assert.equal(detailSource.includes("marketSnapshotsBySymbol"), true);
  assert.equal(detailSource.includes("displayedPositions"), true);
  assert.equal(displaySource.includes("markPrice") || detailSource.includes("markPrice"), true);
  assert.equal(displaySource.includes("unrealizedPnl") || detailSource.includes("unrealizedPnl"), true);
  assert.equal(displaySource.includes("roi") || detailSource.includes("roi"), true);
  assert.equal(detailSource.includes("deriveLivePositionDisplayFromSnapshots"), true);
});
