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

test("market detail opens one unified market SSE and keeps order execution SSE separate", () => {
  const source = read("components/futures/MarketDetailRealtimeView.tsx");

  assert.equal(source.includes("/api/futures/markets/stream"), true);
  assert.equal(source.includes("/api/futures/markets/${encodeURIComponent(initialMarket.symbol)}/stream"), false);
  assert.equal(source.includes("/candles/stream?"), false);
  assert.equal(source.includes("/api/futures/orders/stream"), true);
  assert.equal(source.includes("useResilientEventSource"), true);
  assert.equal(source.includes("clientKey"), true);
  assert.equal(source.includes("MARKET_SUMMARY"), true);
  assert.equal(source.includes("MARKET_CANDLE"), true);
  assert.equal(source.includes("MARKET_HISTORY_FINALIZED"), true);
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
  assert.equal(source.includes("400"), true);
  assert.equal(source.includes("createSseUpstreamHeaders"), true);
  assert.equal(source.includes("proxySseStream"), true);
  assert.equal(source.includes("/api/futures/markets/stream"), true);
  assert.equal(source.includes("response.json()"), false);
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
