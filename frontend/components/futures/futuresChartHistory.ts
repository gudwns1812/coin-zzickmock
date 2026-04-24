const INTERVAL_TO_MS = {
  "1m": 60_000,
  "3m": 3 * 60_000,
  "5m": 5 * 60_000,
  "15m": 15 * 60_000,
  "1h": 60 * 60_000,
  "4h": 4 * 60 * 60_000,
  "12h": 12 * 60 * 60_000,
  "1D": 24 * 60 * 60_000,
  "1W": 7 * 24 * 60 * 60_000,
  "1M": 30 * 24 * 60 * 60_000,
} as const;

type FuturesCandleInterval = keyof typeof INTERVAL_TO_MS;

type CandleHistoryPoint = {
  closeTime: string;
};

export function isFreshFuturesHistory(
  candles: CandleHistoryPoint[],
  interval: FuturesCandleInterval
): boolean {
  const latestCandle = candles.at(-1);

  if (!latestCandle) {
    return false;
  }

  const latestCloseTime = Date.parse(latestCandle.closeTime);

  if (Number.isNaN(latestCloseTime)) {
    return false;
  }

  const intervalMs = INTERVAL_TO_MS[interval];
  const toleratedLag = Math.max(intervalMs * 2, 5 * 60_000);

  return Date.now() - latestCloseTime <= toleratedLag;
}
