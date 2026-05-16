"use client";

import type {
  DashboardSummaryCard,
  PriceFlashRenderState,
} from "@/components/router/(main)/markets/MarketsLanding";
import MarketsLanding from "@/components/router/(main)/markets/MarketsLanding";
import { useResilientEventSource } from "@/hooks/useResilientEventSource";
import type { EventSourceReconnectStatus } from "@/hooks/resilientEventSourcePolicy";
import type { MarketApiResponse } from "@/lib/futures-api";
import { createMarketSummarySseUrl } from "@/lib/futures-sse-url";
import {
  isSupportedMarketSymbol,
  type MarketRankingEntry,
  type MarketSnapshot,
  type MarketSymbol,
} from "@/lib/markets";
import {
  startTransition,
  useCallback,
  useEffect,
  useRef,
  useState,
} from "react";

type PriceFlashTone = "rise" | "fall";
type PriceFlashMetadata = {
  tone: PriceFlashTone;
  peakUntilMs: number;
  endsAtMs: number;
};

type MarketsLandingRealtimeViewProps = {
  initialMarkets: [MarketSnapshot, MarketSnapshot];
  isMarketDataDegraded: boolean;
  isAuthenticated: boolean;
  rankingEntries: MarketRankingEntry[];
  summaryCards: DashboardSummaryCard[];
};

type MarketSnapshotMap = Record<MarketSymbol, MarketSnapshot>;
type PriceFlashMetadataMap = Partial<Record<MarketSymbol, PriceFlashMetadata>>;
type PriceFlashRenderMap = Partial<Record<MarketSymbol, PriceFlashRenderState>>;
type RecoveredSymbolMap = Partial<Record<MarketSymbol, true>>;

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
    turnover24hUsdt:
      realtime.turnover24hUsdt ?? realtime.volume24h ?? current.turnover24hUsdt,
    volume24h: realtime.volume24h ?? realtime.turnover24hUsdt ?? current.volume24h,
    nextFundingAt: realtime.nextFundingAt ?? current.nextFundingAt,
    fundingIntervalHours:
      realtime.fundingIntervalHours ?? current.fundingIntervalHours,
    serverTime: realtime.serverTime ?? current.serverTime,
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

function isRecoveringStatus(status: EventSourceReconnectStatus | undefined) {
  return status === "degraded" || status === "reconnecting";
}

export default function MarketsLandingRealtimeView({
  initialMarkets,
  isMarketDataDegraded,
  isAuthenticated,
  rankingEntries,
  summaryCards,
}: MarketsLandingRealtimeViewProps) {
  const [marketMap, setMarketMap] = useState<MarketSnapshotMap>(() =>
    toMarketMap(initialMarkets)
  );
  const [priceFlashBySymbol, setPriceFlashBySymbol] =
    useState<PriceFlashRenderMap>({});
  const [recoveredStreamSymbols, setRecoveredStreamSymbols] =
    useState<RecoveredSymbolMap>({});
  const [streamStatus, setStreamStatus] =
    useState<EventSourceReconnectStatus>("idle");
  const [lastUpdatedAt, setLastUpdatedAt] = useState(() => new Date());
  const marketMapRef = useRef(marketMap);
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
    setRecoveredStreamSymbols({});
    setStreamStatus("idle");
  }, [initialMarkets]);

  const clearFlashState = useCallback(() => {
    flashMetadataRef.current = {};
    setPriceFlashBySymbol({});

    if (animationFrameRef.current !== null) {
      cancelAnimationFrame(animationFrameRef.current);
      animationFrameRef.current = null;
    }
  }, []);

  const syncFlashRenderState = useCallback((nowMs: number) => {
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
  }, []);

  const ensureFlashClock = useCallback(() => {
    if (animationFrameRef.current !== null) {
      return;
    }

    animationFrameRef.current = requestAnimationFrame(syncFlashRenderState);
  }, [syncFlashRenderState]);

  useEffect(() => {
    return () => {
      clearFlashState();
    };
  }, [clearFlashState]);

  const handleStreamRecovering = useCallback(() => {
    clearFlashState();
  }, [clearFlashState]);

  const handleMarketMessage = useCallback(
    (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data) as MarketApiResponse;

        if (!isSupportedMarketSymbol(data.symbol)) {
          return;
        }

        const nextSymbol: MarketSymbol = data.symbol;
        const currentSnapshot = marketMapRef.current[nextSymbol];

        if (!currentSnapshot) {
          return;
        }

        setRecoveredStreamSymbols((current) => {
          if (current[nextSymbol]) {
            return current;
          }

          return {
            ...current,
            [nextSymbol]: true,
          };
        });

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

          if (!priceFlashTone) {
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
    },
    [ensureFlashClock]
  );

  const isInitialFallbackRecovering =
    isMarketDataDegraded &&
    initialMarkets.some((market) => !recoveredStreamSymbols[market.symbol]);
  const isStreamRecovering =
    isInitialFallbackRecovering || isRecoveringStatus(streamStatus);

  return (
    <>
      <MarketLandingStreamSubscription
        symbols={initialMarkets.map((market) => market.symbol)}
        onMessage={handleMarketMessage}
        onRecovering={handleStreamRecovering}
        onStatusChange={setStreamStatus}
      />
      <MarketsLanding
        isMarketDataDegraded={isStreamRecovering}
        isAuthenticated={isAuthenticated}
        markets={[marketMap.BTCUSDT, marketMap.ETHUSDT]}
        rankingEntries={rankingEntries}
        summaryCards={summaryCards}
        priceFlashBySymbol={priceFlashBySymbol}
        lastUpdatedLabel={
          isStreamRecovering
            ? "데이터 복구 중"
            : new Intl.DateTimeFormat("ko-KR", {
                hour: "numeric",
                minute: "2-digit",
                second: "2-digit",
              }).format(lastUpdatedAt)
        }
      />
    </>
  );
}

type MarketLandingStreamSubscriptionProps = {
  symbols: MarketSymbol[];
  onMessage: (event: MessageEvent) => void;
  onRecovering: () => void;
  onStatusChange: (status: EventSourceReconnectStatus) => void;
};

function MarketLandingStreamSubscription({
  symbols,
  onMessage,
  onRecovering,
  onStatusChange,
}: MarketLandingStreamSubscriptionProps) {
  const streamUrl = createMarketSummarySseUrl(symbols);
  const { status } = useResilientEventSource({
    onError: (e) => {
      onRecovering();
    },
    onMessage,
    onReconnect: onRecovering,
    url: streamUrl,
  });

  useEffect(() => {
    onStatusChange(status);
  }, [onStatusChange, status]);

  return null;
}
