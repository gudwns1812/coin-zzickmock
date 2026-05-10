import type { FuturesCandleInterval } from "@/components/futures/futuresChartViewport";
import type { MarketApiResponse } from "@/lib/futures-api";

export type MarketStreamSource = "INITIAL_SNAPSHOT" | "LIVE";
export type MarketStreamKind =
  | "MARKET_SUMMARY"
  | "MARKET_CANDLE"
  | "MARKET_HISTORY_FINALIZED";

export type MarketStreamCandle = {
  openTime: string;
  closeTime: string;
  openPrice: number;
  highPrice: number;
  lowPrice: number;
  closePrice: number;
  volume: number;
};

export type MarketStreamHistoryFinalized = {
  type: "historyFinalized";
  symbol: string;
  openTime: string;
  closeTime: string;
  affectedIntervals: FuturesCandleInterval[];
};

export type MarketSummaryEnvelope = {
  kind: "MARKET_SUMMARY";
  type?: "MARKET_SUMMARY";
  symbol: string;
  serverTime: string;
  source: MarketStreamSource;
  data: MarketApiResponse;
};

export type MarketCandleEnvelope = {
  kind: "MARKET_CANDLE";
  type?: "MARKET_CANDLE";
  symbol: string;
  interval: FuturesCandleInterval;
  serverTime: string;
  source: MarketStreamSource;
  data: MarketStreamCandle;
};

export type MarketHistoryFinalizedEnvelope = {
  kind: "MARKET_HISTORY_FINALIZED";
  type?: "MARKET_HISTORY_FINALIZED";
  symbol: string;
  interval: FuturesCandleInterval;
  serverTime: string;
  source: MarketStreamSource;
  data: MarketStreamHistoryFinalized;
};

export type MarketStreamEnvelope =
  | MarketSummaryEnvelope
  | MarketCandleEnvelope
  | MarketHistoryFinalizedEnvelope;

export type PublicMarketCandleStreamEvent =
  | MarketStreamCandle
  | MarketStreamHistoryFinalized;

export function parseMarketStreamEnvelope(raw: string): MarketStreamEnvelope | null {
  try {
    const value = JSON.parse(raw) as unknown;
    return isMarketStreamEnvelope(value) ? value : null;
  } catch {
    return null;
  }
}

export function parsePublicMarketSummaryEvent(raw: string): MarketApiResponse | null {
  try {
    const value = JSON.parse(raw) as unknown;
    return isMarketSummary(value) ? value : null;
  } catch {
    return null;
  }
}

export function parsePublicMarketCandleEvent(
  raw: string
): PublicMarketCandleStreamEvent | null {
  try {
    const value = JSON.parse(raw) as unknown;

    if (isMarketCandle(value)) {
      return value;
    }

    return isHistoryFinalized(value) ? value : null;
  } catch {
    return null;
  }
}

function isMarketStreamEnvelope(value: unknown): value is MarketStreamEnvelope {
  if (!value || typeof value !== "object") {
    return false;
  }

  const candidate = value as Partial<MarketStreamEnvelope> & {
    type?: unknown;
    data?: unknown;
  };
  const kind = candidate.kind ?? candidate.type;

  if (
    !isMarketStreamKind(kind) ||
    typeof candidate.symbol !== "string" ||
    typeof candidate.serverTime !== "string" ||
    !isMarketStreamSource(candidate.source)
  ) {
    return false;
  }

  if (kind === "MARKET_SUMMARY") {
    return isMarketSummary(candidate.data);
  }

  if (!isFuturesCandleInterval((candidate as { interval?: unknown }).interval)) {
    return false;
  }

  if (kind === "MARKET_CANDLE") {
    return isMarketCandle(candidate.data);
  }

  return isHistoryFinalized(candidate.data);
}

function isMarketStreamKind(value: unknown): value is MarketStreamKind {
  return (
    value === "MARKET_SUMMARY" ||
    value === "MARKET_CANDLE" ||
    value === "MARKET_HISTORY_FINALIZED"
  );
}

function isMarketStreamSource(value: unknown): value is MarketStreamSource {
  return value === "INITIAL_SNAPSHOT" || value === "LIVE";
}

function isMarketSummary(value: unknown): value is MarketApiResponse {
  if (!value || typeof value !== "object") {
    return false;
  }

  const candidate = value as Partial<MarketApiResponse>;
  return (
    typeof candidate.symbol === "string" &&
    typeof candidate.displayName === "string" &&
    Number.isFinite(candidate.lastPrice) &&
    Number.isFinite(candidate.markPrice) &&
    Number.isFinite(candidate.indexPrice) &&
    Number.isFinite(candidate.fundingRate) &&
    Number.isFinite(candidate.change24h)
  );
}

function isMarketCandle(value: unknown): value is MarketStreamCandle {
  if (!value || typeof value !== "object") {
    return false;
  }

  const candidate = value as Partial<MarketStreamCandle>;
  return (
    typeof candidate.openTime === "string" &&
    typeof candidate.closeTime === "string" &&
    Number.isFinite(candidate.openPrice) &&
    Number.isFinite(candidate.highPrice) &&
    Number.isFinite(candidate.lowPrice) &&
    Number.isFinite(candidate.closePrice) &&
    Number.isFinite(candidate.volume)
  );
}

function isHistoryFinalized(value: unknown): value is MarketStreamHistoryFinalized {
  if (!value || typeof value !== "object") {
    return false;
  }

  const candidate = value as Partial<MarketStreamHistoryFinalized>;
  return (
    candidate.type === "historyFinalized" &&
    typeof candidate.symbol === "string" &&
    typeof candidate.openTime === "string" &&
    typeof candidate.closeTime === "string" &&
    Array.isArray(candidate.affectedIntervals) &&
    candidate.affectedIntervals.every(isFuturesCandleInterval)
  );
}

function isFuturesCandleInterval(value: unknown): value is FuturesCandleInterval {
  return (
    value === "1m" ||
    value === "3m" ||
    value === "5m" ||
    value === "15m" ||
    value === "1h" ||
    value === "4h" ||
    value === "12h" ||
    value === "1D" ||
    value === "1W" ||
    value === "1M"
  );
}
