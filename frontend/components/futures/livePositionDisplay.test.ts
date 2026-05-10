import assert from "node:assert/strict";
import test from "node:test";

const helperModule: typeof import("./livePositionDisplay") = await import(
  new URL("./livePositionDisplay.ts", import.meta.url).href
);

const {
  calculateRoe,
  calculateUnrealizedPnl,
  deriveLiveAccountSummaryDisplay,
  deriveLivePositionDisplay,
  getAccumulatedClosedQuantity,
} = helperModule;

const basePosition = {
  symbol: "BTCUSDT" as const,
  positionSide: "LONG" as const,
  marginMode: "ISOLATED" as const,
  leverage: 10,
  quantity: 2,
  entryPrice: 100,
  markPrice: 100,
  liquidationPrice: 90,
  liquidationPriceType: "EXACT" as const,
  margin: 20,
  unrealizedPnl: 0,
  roi: 0,
  pendingCloseQuantity: 0,
  closeableQuantity: 2,
  accumulatedClosedQuantity: 0,
};

test("LONG unrealized PnL uses mark minus entry times quantity", () => {
  assert.equal(calculateUnrealizedPnl("LONG", 100, 110, 2), 20);
});

test("SHORT unrealized PnL uses entry minus mark times quantity", () => {
  assert.equal(calculateUnrealizedPnl("SHORT", 100, 90, 2), 20);
});

test("ROE divides derived unrealized PnL by margin", () => {
  assert.equal(calculateRoe(20, 50), 0.4);
});

test("ROE does not return NaN for zero or non-finite margin", () => {
  assert.equal(calculateRoe(20, 0), 0);
  assert.equal(calculateRoe(20, Number.NaN), 0);
});

test("selected-symbol positions use live market mark-to-market fields", () => {
  const result = deriveLivePositionDisplay(basePosition, {
    symbol: "BTCUSDT",
    markPrice: 115,
  });

  assert.equal(result.markPrice, 115);
  assert.equal(result.unrealizedPnl, 30);
  assert.equal(result.roi, 1.5);
});

test("positions for non-selected symbols keep server-provided values", () => {
  const ethPosition = {
    ...basePosition,
    symbol: "ETHUSDT" as const,
    markPrice: 3000,
    unrealizedPnl: 12,
    roi: 0.3,
  };

  const result = deriveLivePositionDisplay(ethPosition, {
    symbol: "BTCUSDT",
    markPrice: 115,
  });

  assert.equal(result, ethPosition);
  assert.equal(result.markPrice, 3000);
  assert.equal(result.unrealizedPnl, 12);
  assert.equal(result.roi, 0.3);
});

test("Close amount uses accumulated closed quantity only", () => {
  assert.equal(getAccumulatedClosedQuantity(basePosition), 0);
  assert.equal(
    getAccumulatedClosedQuantity({
      ...basePosition,
      accumulatedClosedQuantity: 0.4,
      pendingCloseQuantity: 2,
      closeableQuantity: 0,
    }),
    0.4
  );
});

test("account summary display uses live position PnL and margin for USDT balance and ROI", () => {
  const result = deriveLiveAccountSummaryDisplay(
    {
      memberId: 1,
      account: "tester@example.com",
      memberName: "Tester",
      nickname: "tester",
      usdtBalance: 1000,
      walletBalance: 1000,
      available: 800,
      totalUnrealizedPnl: 0,
      roi: 0,
      rewardPoint: 0,
    },
    [
      {
        ...basePosition,
        unrealizedPnl: 30,
        margin: 20,
      },
      {
        ...basePosition,
        symbol: "ETHUSDT",
        positionSide: "SHORT",
        unrealizedPnl: -10,
        margin: 30,
      },
    ]
  );

  assert.equal(result?.usdtBalance, 1020);
  assert.equal(result?.walletBalance, 1000);
  assert.equal(result?.available, 800);
  assert.equal(result?.totalUnrealizedPnl, 20);
  assert.equal(result?.roi, 0.4);
});

test("account summary display returns null when account is unavailable", () => {
  assert.equal(deriveLiveAccountSummaryDisplay(null, [basePosition]), null);
});
