import type { FuturesCandleInterval } from "./futuresChartViewport";

export const DEFAULT_FUTURES_CHART_INTERVAL: FuturesCandleInterval = "1m";
export const FUTURES_CHART_INTERVAL_STORAGE_KEY =
  "coinzzickmock:futures-chart-interval";
const FUTURES_CHART_INTERVAL_VALUES = [
  "1m",
  "3m",
  "5m",
  "15m",
  "1h",
  "4h",
  "12h",
  "1D",
  "1W",
  "1M",
] as const satisfies readonly FuturesCandleInterval[];

type ReadableStorage = Pick<Storage, "getItem">;
type WritableStorage = Pick<Storage, "setItem">;

export function isFuturesCandleInterval(
  value: string | null
): value is FuturesCandleInterval {
  return FUTURES_CHART_INTERVAL_VALUES.some((interval) => interval === value);
}

export function readStoredFuturesChartInterval(
  storage: ReadableStorage | null = browserStorage()
): FuturesCandleInterval | null {
  if (!storage) {
    return null;
  }

  try {
    const value = storage.getItem(FUTURES_CHART_INTERVAL_STORAGE_KEY);
    return isFuturesCandleInterval(value) ? value : null;
  } catch {
    return null;
  }
}

export function writeStoredFuturesChartInterval(
  interval: FuturesCandleInterval,
  storage: WritableStorage | null = browserStorage()
): void {
  if (!storage) {
    return;
  }

  try {
    storage.setItem(FUTURES_CHART_INTERVAL_STORAGE_KEY, interval);
  } catch {
    // Ignore unavailable storage; the in-memory chart state still updates.
  }
}

function browserStorage(): (ReadableStorage & WritableStorage) | null {
  if (typeof window === "undefined") {
    return null;
  }

  return window.localStorage;
}
