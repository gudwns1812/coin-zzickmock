export type FuturesRealtimeCandle = {
  openTime: string;
  closeTime: string;
  openPrice: number;
  highPrice: number;
  lowPrice: number;
  closePrice: number;
  volume: number;
};

export function mergeCandlesWithRealtimeCandle(
  candles: FuturesRealtimeCandle[],
  realtimeCandle: FuturesRealtimeCandle | null
): FuturesRealtimeCandle[] {
  if (!realtimeCandle) {
    return candles;
  }

  const byOpenTime = new Map<string, FuturesRealtimeCandle>();

  for (const candle of candles) {
    byOpenTime.set(candle.openTime, candle);
  }

  byOpenTime.set(realtimeCandle.openTime, realtimeCandle);

  return Array.from(byOpenTime.values()).sort(
    (left, right) => Date.parse(left.openTime) - Date.parse(right.openTime)
  );
}
