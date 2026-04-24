import type { HorzScaleOptions, IChartApi } from "lightweight-charts";

export type FuturesCandleInterval =
  | "1m"
  | "3m"
  | "5m"
  | "15m"
  | "1h"
  | "4h"
  | "12h"
  | "1D"
  | "1W"
  | "1M";

export type FuturesChartIntervalConfig = {
  value: FuturesCandleInterval;
  label: string;
  limit: number;
  visibleBars: number;
  rightOffset: number;
  secondsVisible: boolean;
};

export const INTERVAL_OPTIONS: FuturesChartIntervalConfig[] = [
  {
    value: "1m",
    label: "1m",
    limit: 180,
    visibleBars: 144,
    rightOffset: 6,
    secondsVisible: true,
  },
  {
    value: "3m",
    label: "3m",
    limit: 180,
    visibleBars: 132,
    rightOffset: 6,
    secondsVisible: true,
  },
  {
    value: "5m",
    label: "5m",
    limit: 180,
    visibleBars: 120,
    rightOffset: 6,
    secondsVisible: false,
  },
  {
    value: "15m",
    label: "15m",
    limit: 180,
    visibleBars: 96,
    rightOffset: 6,
    secondsVisible: false,
  },
  {
    value: "1h",
    label: "1h",
    limit: 168,
    visibleBars: 84,
    rightOffset: 4,
    secondsVisible: false,
  },
  {
    value: "4h",
    label: "4h",
    limit: 180,
    visibleBars: 90,
    rightOffset: 4,
    secondsVisible: false,
  },
  {
    value: "12h",
    label: "12h",
    limit: 180,
    visibleBars: 72,
    rightOffset: 3,
    secondsVisible: false,
  },
  {
    value: "1D",
    label: "1D",
    limit: 180,
    visibleBars: 90,
    rightOffset: 3,
    secondsVisible: false,
  },
  {
    value: "1W",
    label: "1W",
    limit: 104,
    visibleBars: 52,
    rightOffset: 2,
    secondsVisible: false,
  },
  {
    value: "1M",
    label: "1M",
    limit: 60,
    visibleBars: 30,
    rightOffset: 2,
    secondsVisible: false,
  },
];

export type LogicalRange = {
  from: number;
  to: number;
};

export type ViewportScaleOptions = Partial<Pick<
  HorzScaleOptions,
  "barSpacing" | "maxBarSpacing"
>>;

const SPARSE_HISTORY_BAR_SPACING = 6;

export function getIntervalConfig(
  interval: FuturesCandleInterval
): FuturesChartIntervalConfig {
  return INTERVAL_OPTIONS.find((option) => option.value === interval)!;
}

export function getLatestVisibleLogicalRange(
  totalBars: number,
  config: Pick<FuturesChartIntervalConfig, "visibleBars" | "rightOffset">
): LogicalRange | null {
  if (totalBars <= 0) {
    return null;
  }

  const to = totalBars - 1 + config.rightOffset;
  const from = to - config.visibleBars;

  return { from, to };
}

export function getViewportScaleOptions(
  totalBars: number,
  config: Pick<FuturesChartIntervalConfig, "visibleBars">
): ViewportScaleOptions {
  if (totalBars > 0 && totalBars < config.visibleBars) {
    return {
      barSpacing: SPARSE_HISTORY_BAR_SPACING,
      maxBarSpacing: SPARSE_HISTORY_BAR_SPACING,
    };
  }

  return {
    maxBarSpacing: 0,
  };
}

export function focusLatestCandles(
  chart: IChartApi,
  totalBars: number,
  config: Pick<FuturesChartIntervalConfig, "visibleBars" | "rightOffset">
): void {
  const range = getLatestVisibleLogicalRange(totalBars, config);

  if (!range) {
    chart.timeScale().scrollToRealTime();
    return;
  }

  chart.timeScale().setVisibleLogicalRange(range);
}

export function isViewingLatestRange(
  chart: IChartApi,
  totalBars: number,
  config: Pick<FuturesChartIntervalConfig, "rightOffset" | "visibleBars">
): boolean {
  if (totalBars <= 0) {
    return chart.timeScale().scrollPosition() < 1;
  }

  const visibleRange = chart.timeScale().getVisibleLogicalRange();
  if (!visibleRange) {
    return true;
  }

  const expectedRange = getLatestVisibleLogicalRange(totalBars, config);
  if (!expectedRange) {
    return true;
  }

  return (
    Math.abs(visibleRange.from - expectedRange.from) < 1 &&
    Math.abs(visibleRange.to - expectedRange.to) < 1
  );
}
