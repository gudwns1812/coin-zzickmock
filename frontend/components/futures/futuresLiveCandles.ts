import type { FuturesCandleInterval } from "./futuresChartViewport";

export type FuturesLiveCandle = {
  openTime: string;
  closeTime: string;
  openPrice: number;
  highPrice: number;
  lowPrice: number;
  closePrice: number;
  volume: number;
};

export type LiveCandleBucket = {
  closeTimeMs: number;
  openTimeMs: number;
};

const MINUTE_MS = 60_000;
const HOUR_MS = 60 * MINUTE_MS;
const DAY_MS = 24 * HOUR_MS;

export function getLiveCandleBucket(
  interval: FuturesCandleInterval,
  observedAtMs: number
): LiveCandleBucket {
  if (!Number.isFinite(observedAtMs)) {
    throw new Error("observedAtMs must be finite");
  }

  if (interval === "1W") {
    return weeklyBucket(observedAtMs);
  }

  if (interval === "1M") {
    return monthlyBucket(observedAtMs);
  }

  const durationMs = fixedIntervalDurationMs(interval);
  const openTimeMs = alignToFixedBucket(observedAtMs, durationMs);

  return {
    closeTimeMs: openTimeMs + durationMs,
    openTimeMs,
  };
}

function alignToFixedBucket(observedAtMs: number, durationMs: number): number {
  return Math.floor(observedAtMs / durationMs) * durationMs;
}

export function mergeCandlesWithLivePrice(
  candles: FuturesLiveCandle[],
  interval: FuturesCandleInterval,
  livePrice: number,
  observedAtMs: number,
  liveVolume?: number
): FuturesLiveCandle[] {
  if (!Number.isFinite(livePrice)) {
    return candles;
  }

  const bucket = getLiveCandleBucket(interval, observedAtMs);
  const sortedCandles = [...candles].sort(
    (left, right) => Date.parse(left.openTime) - Date.parse(right.openTime)
  );
  const bucketOpenTime = new Date(bucket.openTimeMs).toISOString();
  const existingIndex = sortedCandles.findIndex(
    (candle) => Date.parse(candle.openTime) === bucket.openTimeMs
  );

  if (existingIndex >= 0) {
    const existing = sortedCandles[existingIndex];
    sortedCandles[existingIndex] = {
      ...existing,
      closePrice: livePrice,
      closeTime: new Date(bucket.closeTimeMs).toISOString(),
      highPrice: Math.max(existing.highPrice, livePrice),
      lowPrice: Math.min(existing.lowPrice, livePrice),
      volume: normalizeLiveVolume(liveVolume, existing.volume),
    };
    return sortedCandles;
  }

  const previousCandle = [...sortedCandles]
    .reverse()
    .find((candle) => Date.parse(candle.openTime) < bucket.openTimeMs);
  const openPrice = previousCandle?.closePrice ?? livePrice;
  const liveCandle: FuturesLiveCandle = {
    closePrice: livePrice,
    closeTime: new Date(bucket.closeTimeMs).toISOString(),
    highPrice: Math.max(openPrice, livePrice),
    lowPrice: Math.min(openPrice, livePrice),
    openPrice,
    openTime: bucketOpenTime,
    volume: normalizeLiveVolume(liveVolume, 0),
  };

  return [...sortedCandles, liveCandle].sort(
    (left, right) => Date.parse(left.openTime) - Date.parse(right.openTime)
  );
}

function normalizeLiveVolume(liveVolume: number | undefined, fallback: number): number {
  if (!Number.isFinite(liveVolume) || liveVolume === undefined) {
    return fallback;
  }

  return Math.max(liveVolume, 0);
}

function fixedIntervalDurationMs(interval: FuturesCandleInterval): number {
  switch (interval) {
    case "1m":
      return MINUTE_MS;
    case "3m":
      return 3 * MINUTE_MS;
    case "5m":
      return 5 * MINUTE_MS;
    case "15m":
      return 15 * MINUTE_MS;
    case "1h":
      return HOUR_MS;
    case "4h":
      return 4 * HOUR_MS;
    case "12h":
      return 12 * HOUR_MS;
    case "1D":
      return DAY_MS;
    case "1W":
    case "1M":
      throw new Error("Calendar intervals use dedicated bucket helpers");
  }
}

function weeklyBucket(observedAtMs: number): LiveCandleBucket {
  const observedAt = new Date(observedAtMs);
  const dayStartMs = Date.UTC(
    observedAt.getUTCFullYear(),
    observedAt.getUTCMonth(),
    observedAt.getUTCDate()
  );
  const dayOfWeek = observedAt.getUTCDay();
  const daysSinceMonday = dayOfWeek === 0 ? 6 : dayOfWeek - 1;
  const openTimeMs = dayStartMs - daysSinceMonday * DAY_MS;

  return {
    closeTimeMs: openTimeMs + 7 * DAY_MS,
    openTimeMs,
  };
}

function monthlyBucket(observedAtMs: number): LiveCandleBucket {
  const observedAt = new Date(observedAtMs);
  const openTimeMs = Date.UTC(
    observedAt.getUTCFullYear(),
    observedAt.getUTCMonth(),
    1
  );
  const closeTimeMs = Date.UTC(
    observedAt.getUTCFullYear(),
    observedAt.getUTCMonth() + 1,
    1
  );

  return {
    closeTimeMs,
    openTimeMs,
  };
}
