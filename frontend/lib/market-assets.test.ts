import assert from "node:assert/strict";
import test from "node:test";

const marketsModule: typeof import("./markets") = await import(
  new URL("./markets.ts", import.meta.url).href
);

const { formatMarketRank, getMarketLogoPath, getMarketRankIconPath } =
  marketsModule;

test("maps supported futures symbols to bundled logo images", () => {
  assert.equal(getMarketLogoPath("BTCUSDT"), "/images/logo/bitcoin.png");
  assert.equal(getMarketLogoPath("ETHUSDT"), "/images/logo/ethereum.png");
});

test("maps leaderboard rank icons to bundled images", () => {
  assert.equal(getMarketRankIconPath(1), "/images/leaderboard/first.png");
  assert.equal(getMarketRankIconPath(2), "/images/leaderboard/second.png");
  assert.equal(getMarketRankIconPath(3), "/images/leaderboard/third.png");
  assert.equal(getMarketRankIconPath(4), "/images/leaderboard/4th.png");
  assert.equal(getMarketRankIconPath(5), null);
});

test("formats current user rank for compact UI", () => {
  assert.equal(formatMarketRank(7), "7위");
  assert.equal(formatMarketRank(null), "집계 중");
  assert.equal(formatMarketRank(undefined), "집계 중");
});
