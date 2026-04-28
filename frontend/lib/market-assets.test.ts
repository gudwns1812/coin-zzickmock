import assert from "node:assert/strict";
import test from "node:test";

const marketsModule: typeof import("./markets") = await import(
  new URL("./markets.ts", import.meta.url).href
);

const { getMarketLogoPath } = marketsModule;

test("maps supported futures symbols to bundled logo images", () => {
  assert.equal(getMarketLogoPath("BTCUSDT"), "/images/logo/bitcoin.png");
  assert.equal(getMarketLogoPath("ETHUSDT"), "/images/logo/ethereum.png");
});
