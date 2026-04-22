"use client";

import type { FuturesOpenOrder, FuturesPosition } from "@/lib/futures-api";
import { formatPercent, formatUsd, type MarketSymbol } from "@/lib/markets";
import { useQuery } from "@tanstack/react-query";
import {
  CandlestickSeries,
  ColorType,
  HistogramSeries,
  LineSeries,
  type UTCTimestamp,
  createChart,
} from "lightweight-charts";
import { useEffect, useMemo, useRef, useState } from "react";

type Props = {
  symbol: MarketSymbol;
  currentPrice: number;
  change24h: number;
  positions: FuturesPosition[];
  openOrders: FuturesOpenOrder[];
};

type FuturesCandleInterval =
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

type CandleResponse = {
  openTime: string;
  closeTime: string;
  openPrice: number;
  highPrice: number;
  lowPrice: number;
  closePrice: number;
  volume: number;
};

type ClientApiResponse<T> = {
  success: boolean;
  data: T | null;
  message: string | null;
};

type LinePoint = {
  time: UTCTimestamp;
  value: number;
};

const INTERVAL_OPTIONS: Array<{
  value: FuturesCandleInterval;
  label: string;
  limit: number;
}> = [
  { value: "1m", label: "1m", limit: 180 },
  { value: "3m", label: "3m", limit: 180 },
  { value: "5m", label: "5m", limit: 180 },
  { value: "15m", label: "15m", limit: 180 },
  { value: "1h", label: "1h", limit: 168 },
  { value: "4h", label: "4h", limit: 180 },
  { value: "12h", label: "12h", limit: 180 },
  { value: "1D", label: "1D", limit: 180 },
  { value: "1W", label: "1W", limit: 104 },
  { value: "1M", label: "1M", limit: 60 },
];

const CHART_COLORS = {
  up: "#10b981",
  down: "#ef4444",
  neutral: "#64748b",
  grid: "#e5e7eb",
  text: "#475569",
  surface: "#ffffff",
  live: "#0f172a",
  order: "#f59e0b",
  ma: "#8b5cf6",
} as const;

export default function FuturesPriceChart({
  symbol,
  currentPrice,
  change24h,
  positions,
  openOrders,
}: Props) {
  const chartContainerRef = useRef<HTMLDivElement | null>(null);
  const [selectedInterval, setSelectedInterval] =
    useState<FuturesCandleInterval>("1m");
  const [showMovingAverage, setShowMovingAverage] = useState(true);
  const [livePoints, setLivePoints] = useState<LinePoint[]>([
    {
      time: toUnixSeconds(Date.now()),
      value: currentPrice,
    },
  ]);

  const selectedConfig = INTERVAL_OPTIONS.find(
    (option) => option.value === selectedInterval
  )!;

  const {
    data: candles = [],
    isLoading,
    isError,
  } = useQuery({
    queryKey: ["futures-candles", symbol, selectedInterval, selectedConfig.limit],
    queryFn: async () => {
      const response = await fetch(
        `/proxy-futures/markets/${symbol}/candles?interval=${selectedInterval}&limit=${selectedConfig.limit}`,
        {
          cache: "no-store",
        }
      );

      const payload =
        (await response.json()) as ClientApiResponse<CandleResponse[]>;

      if (!response.ok || !payload.success || !payload.data) {
        throw new Error(payload.message ?? "차트 데이터를 불러오지 못했습니다.");
      }

      return payload.data;
    },
    retry: 1,
    staleTime: 15_000,
  });

  useEffect(() => {
    setLivePoints((current) => {
      const nextPoint = {
        time: toUnixSeconds(Date.now()),
        value: currentPrice,
      };
      const previous = current[current.length - 1];

      if (previous && previous.time === nextPoint.time) {
        return [...current.slice(0, -1), nextPoint];
      }

      return [...current, nextPoint].slice(-240);
    });
  }, [currentPrice]);

  const candlestickData = useMemo(
    () =>
      candles.map((candle) => ({
        time: toUnixSeconds(candle.openTime),
        open: candle.openPrice,
        high: candle.highPrice,
        low: candle.lowPrice,
        close: candle.closePrice,
      })),
    [candles]
  );

  const volumeData = useMemo(
    () =>
      candles.map((candle) => ({
        time: toUnixSeconds(candle.openTime),
        value: candle.volume,
        color:
          candle.closePrice >= candle.openPrice
            ? "rgba(16,185,129,0.35)"
            : "rgba(239,68,68,0.35)",
      })),
    [candles]
  );

  const movingAverageData = useMemo(() => {
    if (!showMovingAverage || candles.length < 20) {
      return [];
    }

    return candles
      .map((candle, index) => {
        if (index < 19) {
          return null;
        }

        const slice = candles.slice(index - 19, index + 1);
        const average =
          slice.reduce((sum, current) => sum + current.closePrice, 0) /
          slice.length;

        return {
          time: toUnixSeconds(candle.openTime),
          value: average,
        };
      })
      .filter((value): value is LinePoint => value !== null);
  }, [candles, showMovingAverage]);

  const hasCandleData = candlestickData.length > 0;

  useEffect(() => {
    if (!chartContainerRef.current) {
      return;
    }

    const container = chartContainerRef.current;
    const chart = createChart(container, {
      layout: {
        background: { type: ColorType.Solid, color: CHART_COLORS.surface },
        textColor: CHART_COLORS.text,
      },
      grid: {
        vertLines: { color: CHART_COLORS.grid },
        horzLines: { color: CHART_COLORS.grid },
      },
      crosshair: {
        mode: 0,
      },
      width: container.clientWidth,
      height: 420,
      handleScale: true,
      handleScroll: true,
      rightPriceScale: {
        borderColor: CHART_COLORS.grid,
      },
      timeScale: {
        borderColor: CHART_COLORS.grid,
        timeVisible: true,
        secondsVisible: selectedInterval === "1m" || selectedInterval === "3m",
      },
    });

    if (hasCandleData) {
      const candleSeries = chart.addSeries(CandlestickSeries, {
        upColor: CHART_COLORS.up,
        downColor: CHART_COLORS.down,
        wickUpColor: CHART_COLORS.up,
        wickDownColor: CHART_COLORS.down,
        borderVisible: false,
      });
      candleSeries.setData(candlestickData);

      const histogramSeries = chart.addSeries(HistogramSeries, {
        priceFormat: { type: "volume" },
        priceScaleId: "volume",
      });
      histogramSeries.setData(volumeData);
      chart.priceScale("volume").applyOptions({
        scaleMargins: {
          top: 0.8,
          bottom: 0,
        },
        visible: false,
      });

      if (movingAverageData.length > 0) {
        const movingAverageSeries = chart.addSeries(LineSeries, {
          color: CHART_COLORS.ma,
          lineWidth: 2,
          lastValueVisible: false,
          priceLineVisible: false,
        });
        movingAverageSeries.setData(movingAverageData);
      }
      positions.forEach((position) => {
        candleSeries.createPriceLine({
          price: position.entryPrice,
          color:
            position.positionSide === "LONG"
              ? CHART_COLORS.up
              : CHART_COLORS.down,
          lineWidth: 2,
          axisLabelVisible: true,
          title: `${position.positionSide} ${position.marginMode} ${formatUsd(position.entryPrice)}`,
        });
      });

      openOrders.forEach((order) => {
        if (order.limitPrice === null) {
          return;
        }

        candleSeries.createPriceLine({
          price: order.limitPrice,
          color: CHART_COLORS.order,
          lineWidth: 1,
          lineStyle: 2,
          axisLabelVisible: true,
          title: `ORDER ${order.positionSide} ${formatUsd(order.limitPrice)}`,
        });
      });
    } else {
      const liveSeries = chart.addSeries(LineSeries, {
        color: change24h >= 0 ? CHART_COLORS.up : CHART_COLORS.down,
        lineWidth: 3,
        priceLineVisible: true,
      });
      liveSeries.setData(livePoints);

      positions.forEach((position) => {
        liveSeries.createPriceLine({
          price: position.entryPrice,
          color:
            position.positionSide === "LONG"
              ? CHART_COLORS.up
              : CHART_COLORS.down,
          lineWidth: 2,
          axisLabelVisible: true,
          title: `${position.positionSide} ${position.marginMode} ${formatUsd(position.entryPrice)}`,
        });
      });

      openOrders.forEach((order) => {
        if (order.limitPrice === null) {
          return;
        }

        liveSeries.createPriceLine({
          price: order.limitPrice,
          color: CHART_COLORS.order,
          lineWidth: 1,
          lineStyle: 2,
          axisLabelVisible: true,
          title: `ORDER ${order.positionSide} ${formatUsd(order.limitPrice)}`,
        });
      });
    }

    chart.timeScale().fitContent();

    const resizeObserver = new window.ResizeObserver(() => {
      chart.resize(container.clientWidth, 420);
    });
    resizeObserver.observe(container);

    return () => {
      resizeObserver.disconnect();
      chart.remove();
    };
  }, [
    candlestickData,
    change24h,
    hasCandleData,
    livePoints,
    movingAverageData,
    openOrders,
    positions,
    selectedInterval,
    volumeData,
  ]);

  return (
    <div className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
      <div className="flex items-start justify-between gap-main">
        <div>
          <p className="text-sm-custom font-semibold text-main-dark-gray">
            인터랙티브 차트
          </p>
          <p className="mt-1 text-xs-custom text-main-dark-gray/60">
            {hasCandleData
              ? "캔들 히스토리, 포지션선, 주문선을 한 화면에서 확인합니다."
              : "히스토리 응답이 없어서 실시간 가격선 셸을 보여주고 있습니다."}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            className={[
              "rounded-main border px-3 py-2 text-xs-custom font-semibold transition-colors",
              showMovingAverage && hasCandleData
                ? "border-violet-300 bg-violet-50 text-violet-700"
                : "border-main-light-gray text-main-dark-gray/60",
            ].join(" ")}
            disabled={!hasCandleData}
            onClick={() => setShowMovingAverage((current) => !current)}
            type="button"
          >
            MA20
          </button>
        </div>
      </div>

      <div className="mt-4 flex flex-wrap gap-2">
        {INTERVAL_OPTIONS.map((option) => {
          const active = option.value === selectedInterval;
          return (
            <button
              className={[
                "rounded-main border px-3 py-2 text-xs-custom font-semibold transition-colors",
                active
                  ? "border-main-dark-gray bg-main-dark-gray text-white"
                  : "border-main-light-gray text-main-dark-gray/70 hover:border-main-dark-gray/30 hover:bg-main-light-gray/30",
              ].join(" ")}
              key={option.value}
              onClick={() => setSelectedInterval(option.value)}
              type="button"
            >
              {option.label}
            </button>
          );
        })}
      </div>

      <div className="mt-4 h-[420px]">
        <div className="h-full w-full" ref={chartContainerRef} />
      </div>

      <div className="mt-4 grid grid-cols-4 gap-main">
        <ChartMeta
          label="24h 변화율"
          tone={change24h >= 0 ? "positive" : "negative"}
          value={formatPercent(change24h)}
        />
        <ChartMeta
          label="차트 상태"
          tone={hasCandleData ? "neutral" : "warning"}
          value={hasCandleData ? "히스토리 연결됨" : "실시간 라인 셸"}
        />
        <ChartMeta
          label="열린 포지션선"
          tone={positions.length > 0 ? "positive" : "neutral"}
          value={`${positions.length}개`}
        />
        <ChartMeta
          label="열린 주문선"
          tone={openOrders.length > 0 ? "warning" : "neutral"}
          value={`${openOrders.length}개`}
        />
      </div>

      {(isLoading || isError) && (
        <div className="mt-4 rounded-main border border-main-light-gray bg-main-light-gray/30 px-main py-3 text-sm-custom text-main-dark-gray/70">
          {isLoading
            ? "차트 히스토리를 불러오는 중입니다."
            : "차트 히스토리 응답을 받지 못해 실시간 가격선만 표시합니다."}
        </div>
      )}
    </div>
  );
}

function ChartMeta({
  label,
  value,
  tone,
}: {
  label: string;
  value: string;
  tone: "positive" | "negative" | "neutral" | "warning";
}) {
  const toneClassName =
    tone === "positive"
      ? "text-emerald-600"
      : tone === "negative"
        ? "text-rose-600"
        : tone === "warning"
          ? "text-amber-600"
          : "text-main-dark-gray";

  return (
    <div className="rounded-main border border-main-light-gray px-main py-3">
      <p className="text-xs-custom text-main-dark-gray/60">{label}</p>
      <p className={`mt-2 text-sm-custom font-semibold ${toneClassName}`}>
        {value}
      </p>
    </div>
  );
}

function toUnixSeconds(value: string | number): UTCTimestamp {
  const timestamp =
    typeof value === "number" ? Math.floor(value / 1000) : Date.parse(value) / 1000;
  return Math.floor(timestamp) as UTCTimestamp;
}
