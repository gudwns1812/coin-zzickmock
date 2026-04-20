"use client";

import type {
  DashboardSummaryCard,
  PriceFlashRenderState,
} from "@/components/router/(main)/markets/MarketsLanding";
import MarketsLanding from "@/components/router/(main)/markets/MarketsLanding";
import type { MarketApiResponse } from "@/lib/futures-api";
import {
  isSupportedMarketSymbol,
  type MarketSnapshot,
  type MarketSymbol,
} from "@/lib/markets";
import { startTransition, useEffect, useRef, useState } from "react";

type PriceFlashTone = "rise" | "fall";
type PriceFlashMetadata = {
  tone: PriceFlashTone;
  peakUntilMs: number;
  endsAtMs: number;
};

type MarketsLandingRealtimeViewProps = {
  initialMarkets: [MarketSnapshot, MarketSnapshot];
  isMarketDataDegraded: boolean;
  summaryCards: DashboardSummaryCard[];
};

type MarketSnapshotMap = Record<MarketSymbol, MarketSnapshot>;
type PriceFlashMetadataMap = Partial<Record<MarketSymbol, PriceFlashMetadata>>;
type PriceFlashRenderMap = Partial<Record<MarketSymbol, PriceFlashRenderState>>;

const FLASH_PEAK_WINDOW_MS = 110;
const FLASH_DECAY_WINDOW_MS = 520;
const FLASH_VISIBILITY_EPSILON = 0.01;

function toMarketMap(markets: readonly MarketSnapshot[]): MarketSnapshotMap {
  return markets.reduce(
    (acc, market) => {
      acc[market.symbol] = market;
      return acc;
    },
    {} as MarketSnapshotMap
  );
}

function mergeSnapshot(
  current: MarketSnapshot,
  realtime: MarketApiResponse
): MarketSnapshot {
  return {
    ...current,
    displayName: realtime.displayName,
    lastPrice: realtime.lastPrice,
    markPrice: realtime.markPrice,
    indexPrice: realtime.indexPrice,
    fundingRate: realtime.fundingRate,
    change24h: realtime.change24h,
  };
}

function buildFlashMetadata(
  current: PriceFlashMetadata | undefined,
  tone: PriceFlashTone,
  nowMs: number
): PriceFlashMetadata {
  if (!current) {
    return {
      tone,
      peakUntilMs: nowMs + FLASH_PEAK_WINDOW_MS,
      endsAtMs: nowMs + FLASH_PEAK_WINDOW_MS + FLASH_DECAY_WINDOW_MS,
    };
  }

  if (current.tone === tone) {
    const peakUntilMs = nowMs + FLASH_PEAK_WINDOW_MS;

    return {
      tone,
      peakUntilMs: Math.max(current.peakUntilMs, peakUntilMs),
      endsAtMs: Math.max(
        current.endsAtMs,
        peakUntilMs + FLASH_DECAY_WINDOW_MS
      ),
    };
  }

  return {
    tone,
    peakUntilMs: nowMs + FLASH_PEAK_WINDOW_MS,
    endsAtMs: nowMs + FLASH_PEAK_WINDOW_MS + FLASH_DECAY_WINDOW_MS,
  };
}

function computeFlashIntensity(nowMs: number, metadata: PriceFlashMetadata) {
  if (nowMs <= metadata.peakUntilMs) {
    return 1;
  }

  if (nowMs >= metadata.endsAtMs) {
    return 0;
  }

  const decayDuration = metadata.endsAtMs - metadata.peakUntilMs;
  const remaining = 1 - (nowMs - metadata.peakUntilMs) / decayDuration;

  return remaining * remaining;
}

export default function MarketsLandingRealtimeView({
  initialMarkets,
  isMarketDataDegraded,
  summaryCards,
}: MarketsLandingRealtimeViewProps) {
  const [marketMap, setMarketMap] = useState<MarketSnapshotMap>(() =>
    toMarketMap(initialMarkets)
  );
  const [priceFlashBySymbol, setPriceFlashBySymbol] =
    useState<PriceFlashRenderMap>({});
  const [isStreamDegraded, setIsStreamDegraded] = useState(isMarketDataDegraded);
  const [lastUpdatedAt, setLastUpdatedAt] = useState(() => new Date());
  const marketMapRef = useRef(marketMap);
  const isStreamDegradedRef = useRef(isMarketDataDegraded);
  const flashMetadataRef = useRef<PriceFlashMetadataMap>({});
  const animationFrameRef = useRef<number | null>(null);

  useEffect(() => {
    const nextMap = toMarketMap(initialMarkets);
    marketMapRef.current = nextMap;
    setMarketMap(nextMap);
  }, [initialMarkets]);

  useEffect(() => {
    flashMetadataRef.current = {};
    setPriceFlashBySymbol({});
  }, [initialMarkets]);

  useEffect(() => {
    setIsStreamDegraded(isMarketDataDegraded);
    isStreamDegradedRef.current = isMarketDataDegraded;
  }, [isMarketDataDegraded]);

  useEffect(() => {
    const clearFlashState = () => {
      flashMetadataRef.current = {};
      setPriceFlashBySymbol({});

      if (animationFrameRef.current !== null) {
        cancelAnimationFrame(animationFrameRef.current);
        animationFrameRef.current = null;
      }
    };

    const syncFlashRenderState = (nowMs: number) => {
      const nextMetadata: PriceFlashMetadataMap = {};
      const nextRenderState: PriceFlashRenderMap = {};

      for (const symbol of Object.keys(flashMetadataRef.current) as MarketSymbol[]) {
        const metadata = flashMetadataRef.current[symbol];

        if (!metadata || nowMs >= metadata.endsAtMs) {
          continue;
        }

        nextMetadata[symbol] = metadata;

        const intensity = computeFlashIntensity(nowMs, metadata);

        if (intensity > FLASH_VISIBILITY_EPSILON) {
          nextRenderState[symbol] = {
            tone: metadata.tone,
            intensity,
          };
        }
      }

      flashMetadataRef.current = nextMetadata;
      setPriceFlashBySymbol(nextRenderState);

      if (Object.keys(nextMetadata).length === 0) {
        animationFrameRef.current = null;
        return;
      }

      animationFrameRef.current = requestAnimationFrame(syncFlashRenderState);
    };

    const ensureFlashClock = () => {
      if (animationFrameRef.current !== null || isStreamDegraded) {
        return;
      }

      animationFrameRef.current = requestAnimationFrame(syncFlashRenderState);
    };

    if (isStreamDegraded) {
      clearFlashState();
      return;
    }

    const streams = initialMarkets.map((market) => {
      const symbol = market.symbol;
      const stream = new EventSource(
        `/api/futures/markets/${encodeURIComponent(symbol)}/stream`
      );

      stream.onmessage = (event) => {
        try {
          if (isStreamDegradedRef.current) {
            return;
          }

          const data = JSON.parse(event.data) as MarketApiResponse;

          if (!isSupportedMarketSymbol(data.symbol)) {
            return;
          }

          const nextSymbol: MarketSymbol = data.symbol;
          const currentSnapshot = marketMapRef.current[nextSymbol];
          const nextSnapshot = mergeSnapshot(currentSnapshot, data);
          const priceFlashTone =
            nextSnapshot.lastPrice > currentSnapshot.lastPrice
              ? "rise"
              : nextSnapshot.lastPrice < currentSnapshot.lastPrice
                ? "fall"
                : null;

          startTransition(() => {
            setMarketMap((current) => {
              const updated = {
                ...current,
                [nextSymbol]: nextSnapshot,
              };
              marketMapRef.current = updated;
              return updated;
            });
            setLastUpdatedAt(new Date());

            if (!priceFlashTone || isStreamDegradedRef.current) {
              return;
            }

            const nowMs = performance.now();
            flashMetadataRef.current = {
              ...flashMetadataRef.current,
              [nextSymbol]: buildFlashMetadata(
                flashMetadataRef.current[nextSymbol],
                priceFlashTone,
                nowMs
              ),
            };
            ensureFlashClock();
          });
        } catch {
          // Ignore malformed events and keep the last known snapshot.
        }
      };
      stream.onerror = () => {
        clearFlashState();
        isStreamDegradedRef.current = true;
        setIsStreamDegraded(true);
        stream.close();
      };

      return stream;
    });

    return () => {
      streams.forEach((stream) => stream.close());
      clearFlashState();
    };
  }, [initialMarkets, isStreamDegraded]);

  return (
    <MarketsLanding
      isMarketDataDegraded={isStreamDegraded}
      markets={[marketMap.BTCUSDT, marketMap.ETHUSDT]}
      summaryCards={summaryCards}
      priceFlashBySymbol={priceFlashBySymbol}
      lastUpdatedLabel={
        isStreamDegraded
          ? "데이터 복구 중"
          : new Intl.DateTimeFormat("ko-KR", {
              hour: "numeric",
              minute: "2-digit",
              second: "2-digit",
            }).format(lastUpdatedAt)
      }
    />
  );
}
