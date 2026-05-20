import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import test from "node:test";

const sourceRoot = new URL("..", import.meta.url).pathname;

function read(path: string): string {
  return readFileSync(join(sourceRoot, path), "utf8");
}

test("leaderboard position peek row selection toggles the same target closed", () => {
  const landingSource = read("components/router/(main)/markets/MarketsLanding.tsx");

  assert.equal(
    landingSource.includes("currentTarget?.targetToken === target.targetToken ? null : target"),
    true
  );
  assert.equal(
    landingSource.includes("onSelect={entry.targetToken ? togglePeekTarget : undefined}"),
    true
  );
});

test("markets leaderboard does not render seeded fallback rows as live rankings", () => {
  const landingSource = read("components/router/(main)/markets/MarketsLanding.tsx");
  const realtimeSource = read("components/router/(main)/markets/MarketsLandingRealtimeView.tsx");

  assert.equal(landingSource.includes("MARKET_RANKING_FALLBACKS"), false);
  assert.equal(landingSource.includes("rankingEntries = []"), true);
  assert.equal(
    realtimeSource.includes("rankingEntries={leaderboardQuery.data?.entries ?? rankingEntries ?? []}"),
    true
  );
});

test("markets leaderboard query runs for anonymous viewers", () => {
  const realtimeSource = read("components/router/(main)/markets/MarketsLandingRealtimeView.tsx");
  const leaderboardQueryBlock = realtimeSource.slice(
    realtimeSource.indexOf("const leaderboardQuery = useQuery({"),
    realtimeSource.indexOf("const personalSummaryCards")
  );

  assert.equal(leaderboardQueryBlock.includes("enabled:"), false);
  assert.equal(
    leaderboardQueryBlock.includes("queryFn: () => getFuturesLeaderboardClient({ limit: 4 })"),
    true
  );
});
