import assert from "node:assert/strict";
import test from "node:test";

const renderModeModule: typeof import("./futuresChartRenderMode") =
  await import(new URL("./futuresChartRenderMode.ts", import.meta.url).href);

const { getFuturesChartRenderMode, getFuturesChartStatus } = renderModeModule;

test("futures chart render mode keeps initial interval loading empty", () => {
  assert.equal(
    getFuturesChartRenderMode({
      hasCandleData: false,
      hasQueryData: false,
      historyStatus: "missing",
      isError: false,
      isLoading: true,
    }),
    "loading-empty"
  );
});

test("futures chart render mode prefers candles when candle data is available", () => {
  assert.equal(
    getFuturesChartRenderMode({
      hasCandleData: true,
      hasQueryData: true,
      historyStatus: "ready",
      isError: false,
      isLoading: false,
    }),
    "candles"
  );
});

test("futures chart render mode preserves live fallback after resolved gaps", () => {
  const resolvedStates = [
    {
      hasQueryData: true,
      historyStatus: "missing" as const,
      isError: false,
      isLoading: false,
    },
    {
      hasQueryData: true,
      historyStatus: "stale" as const,
      isError: false,
      isLoading: false,
    },
    {
      hasQueryData: false,
      historyStatus: "missing" as const,
      isError: true,
      isLoading: false,
    },
  ];

  for (const state of resolvedStates) {
    assert.equal(
      getFuturesChartRenderMode({
        hasCandleData: false,
        ...state,
      }),
      "live-fallback"
    );
  }
});

test("futures chart status labels loading-empty as loading, not live shell", () => {
  assert.equal(
    getFuturesChartStatus({
      hasNextPage: undefined,
      historyStatus: "missing",
      renderMode: "loading-empty",
    }),
    "히스토리 로딩 중"
  );
  assert.notEqual(
    getFuturesChartStatus({
      hasNextPage: undefined,
      historyStatus: "missing",
      renderMode: "loading-empty",
    }),
    "실시간 라인 셸"
  );
});
