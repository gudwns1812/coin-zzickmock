import assert from "node:assert/strict";
import test from "node:test";

const viewportModule: typeof import("./futuresChartViewport") = await import(
  new URL("./futuresChartViewport.ts", import.meta.url).href
);
const { getIntervalConfig, getLatestVisibleLogicalRange, INTERVAL_OPTIONS } =
  viewportModule;

test("all chart intervals expose the approved viewport presets", () => {
  assert.deepEqual(
    INTERVAL_OPTIONS.map((option) => ({
      interval: option.value,
      limit: option.limit,
      rightOffset: option.rightOffset,
      secondsVisible: option.secondsVisible,
      visibleBars: option.visibleBars,
    })),
    [
      {
        interval: "1m",
        limit: 180,
        visibleBars: 144,
        rightOffset: 6,
        secondsVisible: true,
      },
      {
        interval: "3m",
        limit: 180,
        visibleBars: 132,
        rightOffset: 6,
        secondsVisible: true,
      },
      {
        interval: "5m",
        limit: 180,
        visibleBars: 120,
        rightOffset: 6,
        secondsVisible: false,
      },
      {
        interval: "15m",
        limit: 180,
        visibleBars: 96,
        rightOffset: 6,
        secondsVisible: false,
      },
      {
        interval: "1h",
        limit: 168,
        visibleBars: 84,
        rightOffset: 4,
        secondsVisible: false,
      },
      {
        interval: "4h",
        limit: 180,
        visibleBars: 90,
        rightOffset: 4,
        secondsVisible: false,
      },
      {
        interval: "12h",
        limit: 180,
        visibleBars: 72,
        rightOffset: 3,
        secondsVisible: false,
      },
      {
        interval: "1D",
        limit: 180,
        visibleBars: 90,
        rightOffset: 3,
        secondsVisible: false,
      },
      {
        interval: "1W",
        limit: 104,
        visibleBars: 52,
        rightOffset: 2,
        secondsVisible: false,
      },
      {
        interval: "1M",
        limit: 60,
        visibleBars: 30,
        rightOffset: 2,
        secondsVisible: false,
      },
    ]
  );
});

test("viewport range uses all available bars when less than target visible bars", () => {
  const config = getIntervalConfig("1D");
  const range = getLatestVisibleLogicalRange(12, config);

  assert.deepEqual(range, {
    from: 2,
    to: 14,
  });
});

test("viewport range uses configured visible bars when enough candles are loaded", () => {
  const config = getIntervalConfig("1h");
  const range = getLatestVisibleLogicalRange(168, config);

  assert.deepEqual(range, {
    from: 87,
    to: 171,
  });
});

test("latest-range detection rejects zoomed-out views anchored near realtime", () => {
  const { isViewingLatestRange } = viewportModule;
  const config = getIntervalConfig("1h");
  const fakeChart = {
    timeScale() {
      return {
        getVisibleLogicalRange() {
          return {
            from: 40,
            to: 171,
          };
        },
        scrollPosition() {
          return 0;
        },
      };
    },
  };

  assert.equal(
    isViewingLatestRange(fakeChart as never, 168, config),
    false
  );
});
