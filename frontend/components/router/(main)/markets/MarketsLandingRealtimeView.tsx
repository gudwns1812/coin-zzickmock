"use client";

import MarketsLanding from "@/components/router/(main)/markets/MarketsLanding";
import type {MarketApiResponse} from "@/lib/futures-api";
import {isSupportedMarketSymbol, type MarketSnapshot, type MarketSymbol,} from "@/lib/markets";
import {startTransition, useEffect, useRef, useState} from "react";

type PriceFlashTone = "rise" | "fall";

type MarketsLandingRealtimeViewProps = {
    initialMarkets: [MarketSnapshot, MarketSnapshot];
};

type MarketSnapshotMap = Record<MarketSymbol, MarketSnapshot>;

function toMarketMap(
    markets: readonly MarketSnapshot[]
): MarketSnapshotMap {
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

export default function MarketsLandingRealtimeView({
                                                       initialMarkets,
                                                   }: MarketsLandingRealtimeViewProps) {
    const [marketMap, setMarketMap] = useState<MarketSnapshotMap>(() =>
        toMarketMap(initialMarkets)
    );
    const [priceFlashBySymbol, setPriceFlashBySymbol] = useState<
        Partial<Record<MarketSymbol, PriceFlashTone>>
    >({});
    const marketMapRef = useRef(marketMap);
    const flashTimeoutRef = useRef<Partial<Record<MarketSymbol, number>>>({});

    useEffect(() => {
        const nextMap = toMarketMap(initialMarkets);
        marketMapRef.current = nextMap;
        setMarketMap(nextMap);
    }, [initialMarkets]);

    useEffect(() => {
        const streams = initialMarkets.map((market) => {
            const symbol = market.symbol;
            const stream = new EventSource(
                `/api/futures/markets/${encodeURIComponent(symbol)}/stream`
            );

            stream.onmessage = (event) => {
                try {
                    const data = JSON.parse(event.data) as MarketApiResponse;

                    if (!isSupportedMarketSymbol(data.symbol)) {
                        return;
                    }

                    const symbol: MarketSymbol = data.symbol;
                    const currentSnapshot = marketMapRef.current[symbol];
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
                                [symbol]: nextSnapshot,
                            };
                            marketMapRef.current = updated;
                            return updated;
                        });

                        if (!priceFlashTone) {
                            return;
                        }

                        window.clearTimeout(flashTimeoutRef.current[symbol]);
                        setPriceFlashBySymbol((current) => ({
                            ...current,
                            [symbol]: priceFlashTone,
                        }));
                        flashTimeoutRef.current[symbol] = window.setTimeout(() => {
                            setPriceFlashBySymbol((current) => {
                                if (!current[symbol]) {
                                    return current;
                                }

                                return {
                                    ...current,
                                    [symbol]: undefined,
                                };
                            });
                        }, 900);
                    });
                } catch {
                    // Ignore malformed events and keep the last known snapshot.
                }
            };

            return stream;
        });

        return () => {
            streams.forEach((stream) => stream.close());
            Object.values(flashTimeoutRef.current).forEach((timeoutId) => {
                if (timeoutId) {
                    window.clearTimeout(timeoutId);
                }
            });
        };
    }, [initialMarkets]);

    return (
        <MarketsLanding
            markets={[
                marketMap.BTCUSDT,
                marketMap.ETHUSDT,
            ]}
            priceFlashBySymbol={priceFlashBySymbol}
        />
    );
}
