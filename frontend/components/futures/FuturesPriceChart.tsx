"use client";

import type { FuturesOpenOrder, FuturesPosition } from "@/lib/futures-api";
import { formatPercent, formatUsd, type MarketSymbol } from "@/lib/markets";
import Modal from "@/components/ui/Modal";
import { useInfiniteQuery, useQueryClient } from "@tanstack/react-query";
import {
  Activity,
  ChartArea,
  ChartLine,
  ChevronDown,
  ChevronUp,
  Settings2,
  type LucideIcon,
} from "lucide-react";
import {
  CandlestickSeries,
  ColorType,
  HistogramSeries,
  LineSeries,
  type CandlestickData,
  type HistogramData,
  type IChartApi,
  type IPriceLine,
  type ISeriesApi,
  type Time,
  type UTCTimestamp,
  createChart,
} from "lightweight-charts";
import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type MutableRefObject,
} from "react";
import {
  canEditIndicators,
  calculateIndicatorValueRows,
  DEFAULT_INDICATOR_CONFIGS,
  getIndicatorFallbackMessage,
  getEnabledIndicatorCount,
  resetIndicators,
  setIndicatorEnabled,
  updateBollingerStdDev,
  updateIndicatorPeriod,
  type IndicatorConfig,
  type IndicatorConfigs,
  type IndicatorValueRow,
  type IndicatorType,
} from "./futuresChartIndicators";
import {
  clearIndicatorSeries,
  createIndicatorSeriesRefs,
  syncIndicatorSeries,
} from "./futuresChartIndicatorSeries";
import { isFreshFuturesHistory } from "./futuresChartHistory";
import {
  formatChartTickInKst,
  formatChartTimeInKst,
} from "./futuresChartTime";
import { mergeCandlesWithRealtimeCandle } from "./futuresRealtimeCandles";
import {
  getIntervalConfig,
  getLatestVisibleLogicalRange,
  getViewportScaleOptions,
  INTERVAL_OPTIONS,
  isViewingLatestRange,
  type FuturesCandleInterval,
} from "./futuresChartViewport";
import {
  readStoredFuturesChartInterval,
  writeStoredFuturesChartInterval,
} from "./futuresChartIntervalPreference";
import {
  getFuturesChartRenderMode,
} from "./futuresChartRenderMode";
import type {
  MarketStreamCandle,
  MarketStreamHistoryFinalized,
} from "./marketStreamEnvelope";
import {
  getOrderPriceLineColor,
  getOrderPriceLineTitle,
  getPositionPriceLineTitle,
} from "./futuresChartTradeLabels";

type Props = {
  symbol: MarketSymbol;
  currentPrice: number;
  currentPriceUpdatedAt: number;
  change24h: number;
  positions: FuturesPosition[];
  openOrders: FuturesOpenOrder[];
  selectedInterval: FuturesCandleInterval;
  onSelectedIntervalChange: (interval: FuturesCandleInterval) => void;
  marketStreamCandle: (MarketStreamCandle & { interval: FuturesCandleInterval; serverTime: string }) | null;
  marketStreamHistoryFinalized: (MarketStreamHistoryFinalized & { interval: FuturesCandleInterval; serverTime: string }) | null;
  onLatestCandleClosePriceChange?: (closePrice: number, receivedAt: number) => void;
};

type CandleResponse = {
  openTime: string;
  closeTime: string;
  openPrice: number;
  highPrice: number;
  lowPrice: number;
  closePrice: number;
  volume: number;
};

type HistoryFinalizedResponse = {
  type: "historyFinalized";
  symbol: string;
  openTime: string;
  closeTime: string;
  affectedIntervals: FuturesCandleInterval[];
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

type OhlcSnapshot = {
  close: number;
  high: number;
  low: number;
  open: number;
};

type OhlcTone = "bearish" | "bullish" | "neutral";

type PriceLineOwner = ISeriesApi<"Candlestick"> | ISeriesApi<"Line">;

type OwnedPriceLine = {
  line: IPriceLine;
  owner: PriceLineOwner;
};

const CHART_COLORS = {
  down: "#ef4444",
  grid: "#e5e7eb",
  surface: "#ffffff",
  text: "#475569",
  up: "#10b981",
} as const;

const LOAD_MORE_THRESHOLD = 25;
const CLOSED_CANDLE_REFETCH_DELAY_MS = 4_000;
const CHART_HEIGHT = 620;
const MAIN_PANE_INITIAL_HEIGHT = 455;
const VOLUME_PANE_INITIAL_HEIGHT = CHART_HEIGHT - MAIN_PANE_INITIAL_HEIGHT;
const DEFAULT_VOLUME_PANE_TOP = MAIN_PANE_INITIAL_HEIGHT;
const VOLUME_PANE_INDEX = 1;
const MAIN_PANE_STRETCH_FACTOR = 0.76;
const VOLUME_PANE_STRETCH_FACTOR = 0.24;
const VOLUME_LEGEND_TOP_OFFSET = 10;
const VOLUME_LEGEND_MAX_TOP = CHART_HEIGHT - 72;
const INDICATOR_TYPES: IndicatorType[] = ["EMA", "SMA", "BOLLINGER"];

export default function FuturesPriceChart({
  symbol,
  currentPrice,
  currentPriceUpdatedAt,
  change24h,
  positions,
  openOrders,
  selectedInterval,
  onSelectedIntervalChange,
  marketStreamCandle,
  marketStreamHistoryFinalized,
  onLatestCandleClosePriceChange,
}: Props) {
  const queryClient = useQueryClient();
  const chartContainerRef = useRef<HTMLDivElement | null>(null);
  const chartRef = useRef<IChartApi | null>(null);
  const candleSeriesRef = useRef<ISeriesApi<"Candlestick"> | null>(null);
  const liveSeriesRef = useRef<ISeriesApi<"Line"> | null>(null);
  const volumeSeriesRef = useRef<ISeriesApi<"Histogram"> | null>(null);
  const indicatorSeriesRefs = useRef(createIndicatorSeriesRefs());
  const positionPriceLinesRef = useRef<OwnedPriceLine[]>([]);
  const orderPriceLinesRef = useRef<OwnedPriceLine[]>([]);
  const appliedInitialViewportKeyRef = useRef<string | null>(null);
  const realtimeCandleOpenTimeRef = useRef<string | null>(null);
  const closedCandleRefetchTimeoutRef = useRef<number | null>(null);
  const [isIntervalPreferenceHydrated, setIsIntervalPreferenceHydrated] =
    useState(false);
  const [hoveredOhlc, setHoveredOhlc] = useState<OhlcSnapshot | null>(null);
  const [hoveredVolume, setHoveredVolume] = useState<number | null>(null);
  const [hoveredTime, setHoveredTime] = useState<Time | null>(null);
  const [volumePaneTop, setVolumePaneTop] = useState<number | null>(null);
  const [indicatorConfigs, setIndicatorConfigs] = useState<IndicatorConfigs>(() =>
    resetIndicators(DEFAULT_INDICATOR_CONFIGS)
  );
  const [isIndicatorValuePanelOpen, setIsIndicatorValuePanelOpen] =
    useState(true);
  const [isIndicatorSettingsOpen, setIsIndicatorSettingsOpen] = useState(false);
  const [isAtLatest, setIsAtLatest] = useState(true);
  const [livePoints, setLivePoints] = useState<LinePoint[]>([
    {
      time: toUnixSeconds(currentPriceUpdatedAt),
      value: currentPrice,
    },
  ]);
  const [realtimeCandle, setRealtimeCandle] =
    useState<CandleResponse | null>(null);

  const selectedConfig = getIntervalConfig(selectedInterval);
  const initialViewportKey = `${symbol}:${selectedInterval}`;

  useEffect(() => {
    const storedInterval = readStoredFuturesChartInterval();

    if (storedInterval && storedInterval !== selectedInterval) {
      onSelectedIntervalChange(storedInterval);
    }
    setIsIntervalPreferenceHydrated(true);
  }, [onSelectedIntervalChange, selectedInterval]);

  const historyQuery = useInfiniteQuery<CandleResponse[], Error>({
    queryKey: [
      "futures-candles",
      symbol,
      selectedInterval,
      selectedConfig.limit,
    ],
    enabled: isIntervalPreferenceHydrated,
    initialPageParam: undefined,
    queryFn: async ({ pageParam }) => {
      const response = await fetch(
        buildCandleRequestUrl(
          symbol,
          selectedInterval,
          selectedConfig.limit,
          typeof pageParam === "string" ? pageParam : undefined
        ),
        {
          cache: "no-store",
        }
      );

      const payload =
        (await response.json()) as ClientApiResponse<CandleResponse[]>;

      if (!response.ok || !payload.success || !payload.data) {
        throw new Error(payload.message ?? "차트 데이터를 불러오지 못했습니다.");
      }

      console.log("[FuturesPriceChart] fetched candles", {
        before: typeof pageParam === "string" ? pageParam : null,
        count: payload.data.length,
        interval: selectedInterval,
        limit: selectedConfig.limit,
        symbol,
      });

      return payload.data;
    },
    getNextPageParam: (lastPage) =>
      lastPage.length > 0 ? lastPage[0]?.openTime : undefined,
    retry: 1,
    staleTime: 15_000,
  });


  const candles = useMemo(() => {
    const merged = [...(historyQuery.data?.pages ?? [])].reverse().flat();
    const uniqueByOpenTime = new Map<string, CandleResponse>();

    for (const candle of merged) {
      uniqueByOpenTime.set(candle.openTime, candle);
    }

    return Array.from(uniqueByOpenTime.values()).sort(
      (left, right) => Date.parse(left.openTime) - Date.parse(right.openTime)
    );
  }, [historyQuery.data]);

  const historyStatus = useMemo(() => {
    if (candles.length === 0) {
      return "missing" as const;
    }

    return isFreshFuturesHistory(candles, selectedInterval)
      ? ("ready" as const)
      : ("stale" as const);
  }, [candles, selectedInterval]);

  const hasFreshHistory = historyStatus === "ready";
  const historicalCandles = hasFreshHistory ? candles : [];
  const displayCandles = useMemo(() => {
    if (!hasFreshHistory) {
      return historicalCandles;
    }

    return mergeCandlesWithRealtimeCandle(historicalCandles, realtimeCandle);
  }, [hasFreshHistory, historicalCandles, realtimeCandle]);
  const latestVisibleCandle = displayCandles.at(-1) ?? null;
  const displayedOhlc = hoveredOhlc ?? toOhlcSnapshot(latestVisibleCandle);
  const displayedVolume = hoveredVolume ?? latestVisibleCandle?.volume ?? null;
  const {
    fetchNextPage,
    hasNextPage,
    isError,
    isFetchingNextPage,
    isLoading,
  } = historyQuery;

  useEffect(() => {
    realtimeCandleOpenTimeRef.current = null;
    clearClosedCandleRefetchTimeout(closedCandleRefetchTimeoutRef);
    setRealtimeCandle(null);
    setLivePoints([
      {
        time: toUnixSeconds(currentPriceUpdatedAt),
        value: currentPrice,
      },
    ]);
  }, [symbol]);

  useEffect(() => {
    setLivePoints((current) => {
      const nextPoint = {
        time: toUnixSeconds(currentPriceUpdatedAt),
        value: currentPrice,
      };
      const previous = current[current.length - 1];

      if (previous && previous.time === nextPoint.time) {
        return [...current.slice(0, -1), nextPoint];
      }

      return [...current, nextPoint].slice(-240);
    });
  }, [currentPrice, currentPriceUpdatedAt]);

  const candlestickData = useMemo<CandlestickData<Time>[]>(() => {
    return displayCandles.map((candle) => ({
      time: toUnixSeconds(candle.openTime),
      open: candle.openPrice,
      high: candle.highPrice,
      low: candle.lowPrice,
      close: candle.closePrice,
    }));
  }, [displayCandles]);

  const indicatorCandles = useMemo(
    () =>
      candlestickData.map((candle) => ({
        close: candle.close,
        time: candle.time,
      })),
    [candlestickData]
  );

  const volumeData = useMemo<HistogramData<Time>[]>(() => {
    return displayCandles.map((candle) => ({
      time: toUnixSeconds(candle.openTime),
      value: candle.volume,
      color:
        candle.closePrice >= candle.openPrice
          ? "rgba(16,185,129,0.35)"
          : "rgba(239,68,68,0.35)",
    }));
  }, [displayCandles]);

  const hasCandleData = candlestickData.length > 0;
  const chartRenderMode = getFuturesChartRenderMode({
    hasCandleData,
    hasQueryData: historyQuery.data !== undefined,
    historyStatus,
    isError,
    isLoading,
  });
  const indicatorControlsEnabled = canEditIndicators(hasFreshHistory);
  const indicatorFallbackMessage = getIndicatorFallbackMessage(hasFreshHistory);
  const enabledIndicatorCount = getEnabledIndicatorCount(indicatorConfigs);
  const hoveredIndicatorValues = useMemo(
    () =>
      calculateIndicatorValueRows(
        indicatorCandles,
        indicatorConfigs,
        hoveredTime
      ),
    [hoveredTime, indicatorCandles, indicatorConfigs]
  );
  const canShowIndicatorValues =
    hasFreshHistory && enabledIndicatorCount > 0 && hasCandleData;
  const ohlcTone = getOhlcTone(displayedOhlc);

  useEffect(() => {
    if (!chartContainerRef.current) {
      return;
    }

    const container = chartContainerRef.current;
    const chart = createChart(container, {
      layout: {
        background: { type: ColorType.Solid, color: CHART_COLORS.surface },
        panes: {
          enableResize: true,
          separatorColor: CHART_COLORS.grid,
          separatorHoverColor: "rgba(37, 99, 235, 0.12)",
        },
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
      height: CHART_HEIGHT,
      handleScale: true,
      handleScroll: true,
      localization: {
        dateFormat: "yyyy-MM-dd",
        locale: "ko-KR",
        timeFormatter: formatChartTimeInKst,
      },
      rightPriceScale: {
        borderColor: CHART_COLORS.grid,
      },
      timeScale: {
        borderColor: CHART_COLORS.grid,
        timeVisible: true,
        secondsVisible: selectedConfig.secondsVisible,
        tickMarkFormatter: formatChartTickInKst,
      },
    });

    chartRef.current = chart;

    const candleSeries = chart.addSeries(CandlestickSeries, {
      upColor: CHART_COLORS.up,
      downColor: CHART_COLORS.down,
      wickUpColor: CHART_COLORS.up,
      wickDownColor: CHART_COLORS.down,
      borderVisible: false,
    });
    candleSeries.priceScale().applyOptions({
      scaleMargins: { top: 0.18, bottom: 0.1 },
    });
    candleSeriesRef.current = candleSeries;

    const liveSeries = chart.addSeries(LineSeries, {
      color: change24h >= 0 ? CHART_COLORS.up : CHART_COLORS.down,
      lineWidth: 3,
      priceLineVisible: true,
    });
    liveSeriesRef.current = liveSeries;

    const volumeSeries = chart.addSeries(
      HistogramSeries,
      {
        lastValueVisible: false,
        priceFormat: { type: "volume" },
        priceLineVisible: false,
      },
      VOLUME_PANE_INDEX
    );
    volumeSeries.priceScale().applyOptions({
      scaleMargins: {
        top: 0.18,
        bottom: 0,
      },
      visible: true,
    });
    volumeSeriesRef.current = volumeSeries;

    chart.panes()[0]?.setStretchFactor(MAIN_PANE_STRETCH_FACTOR);
    chart.panes()[VOLUME_PANE_INDEX]?.setStretchFactor(VOLUME_PANE_STRETCH_FACTOR);
    chart.panes()[0]?.setHeight(MAIN_PANE_INITIAL_HEIGHT);
    chart.panes()[VOLUME_PANE_INDEX]?.setHeight(VOLUME_PANE_INITIAL_HEIGHT);

    let paneResizeObserver: ResizeObserver | null = null;
    let observedVolumePaneElement: HTMLElement | null = null;
    const syncVolumePaneTop = () => {
      const volumePaneElement =
        chart.panes()[VOLUME_PANE_INDEX]?.getHTMLElement() ?? null;

      if (!volumePaneElement) {
        setVolumePaneTop(DEFAULT_VOLUME_PANE_TOP);
        return;
      }

      if (volumePaneElement !== observedVolumePaneElement) {
        if (observedVolumePaneElement) {
          paneResizeObserver?.unobserve(observedVolumePaneElement);
        }
        paneResizeObserver?.observe(volumePaneElement);
        observedVolumePaneElement = volumePaneElement;
      }

      const containerTop = container.getBoundingClientRect().top;
      const paneTop = volumePaneElement.getBoundingClientRect().top;
      setVolumePaneTop(Math.max(0, paneTop - containerTop));
    };
    paneResizeObserver = new window.ResizeObserver(syncVolumePaneTop);

    chart.subscribeCrosshairMove((param) => {
      if (!param.point || !param.time) {
        setHoveredOhlc(null);
        setHoveredVolume(null);
        setHoveredTime(null);
        return;
      }

      const hoveredData = param.seriesData.get(candleSeries);
      const hoveredVolumeData = param.seriesData.get(volumeSeries);
      if (!hoveredData || !("open" in hoveredData)) {
        setHoveredOhlc(null);
        setHoveredVolume(null);
        setHoveredTime(null);
        return;
      }

      setHoveredTime(param.time);
      setHoveredOhlc({
        open: hoveredData.open,
        high: hoveredData.high,
        low: hoveredData.low,
        close: hoveredData.close,
      });
      setHoveredVolume(
        hoveredVolumeData && "value" in hoveredVolumeData
          ? hoveredVolumeData.value
          : null
      );
    });

    const resizeObserver = new window.ResizeObserver(() => {
      chart.resize(container.clientWidth, CHART_HEIGHT);
      syncVolumePaneTop();
    });
    resizeObserver.observe(container);

    syncVolumePaneTop();
    const paneSyncAnimationFrame = requestAnimationFrame(syncVolumePaneTop);

    return () => {
      clearPriceLines(positionPriceLinesRef.current);
      clearPriceLines(orderPriceLinesRef.current);
      clearIndicatorSeries(chart, indicatorSeriesRefs.current);
      cancelAnimationFrame(paneSyncAnimationFrame);
      resizeObserver.disconnect();
      paneResizeObserver?.disconnect();
      chart.remove();
      chartRef.current = null;
      candleSeriesRef.current = null;
      liveSeriesRef.current = null;
      volumeSeriesRef.current = null;
    };
  }, []);

  useEffect(() => {
    setHoveredOhlc(null);
    setHoveredVolume(null);
    setHoveredTime(null);

    chartRef.current?.applyOptions({
      timeScale: {
        rightOffset: selectedConfig.rightOffset,
        secondsVisible: selectedConfig.secondsVisible,
      },
    });
  }, [selectedConfig.rightOffset, selectedConfig.secondsVisible]);

  useEffect(() => {
    if (!isIntervalPreferenceHydrated) {
      return;
    }

    realtimeCandleOpenTimeRef.current = null;
    setRealtimeCandle(null);
  }, [isIntervalPreferenceHydrated, selectedInterval, symbol]);

  const invalidateCurrentCandleQueries = useCallback(() => {
    void queryClient.invalidateQueries({
      exact: false,
      queryKey: ["futures-candles", symbol, selectedInterval],
    });
  }, [queryClient, selectedInterval, symbol]);

  const scheduleClosedCandleFinalizationRefetch = useCallback(() => {
    clearClosedCandleRefetchTimeout(closedCandleRefetchTimeoutRef);
    closedCandleRefetchTimeoutRef.current = window.setTimeout(() => {
      closedCandleRefetchTimeoutRef.current = null;
      invalidateCurrentCandleQueries();
    }, CLOSED_CANDLE_REFETCH_DELAY_MS);
  }, [invalidateCurrentCandleQueries]);

  useEffect(() => {
    if (!marketStreamHistoryFinalized) {
      return;
    }

    if (
      marketStreamHistoryFinalized.symbol === symbol &&
      marketStreamHistoryFinalized.affectedIntervals.includes(selectedInterval)
    ) {
      clearClosedCandleRefetchTimeout(closedCandleRefetchTimeoutRef);
      invalidateCurrentCandleQueries();
    }
  }, [
    invalidateCurrentCandleQueries,
    marketStreamHistoryFinalized,
    selectedInterval,
    symbol,
  ]);

  useEffect(() => {
    if (
      !marketStreamCandle ||
      marketStreamCandle.interval !== selectedInterval ||
      !isCandleResponse(marketStreamCandle)
    ) {
      return;
    }

    const previousOpenTime = realtimeCandleOpenTimeRef.current;
    realtimeCandleOpenTimeRef.current = marketStreamCandle.openTime;
    setRealtimeCandle(marketStreamCandle);
    onLatestCandleClosePriceChange?.(marketStreamCandle.closePrice, Date.now());

    if (!previousOpenTime || previousOpenTime !== marketStreamCandle.openTime) {
      scheduleClosedCandleFinalizationRefetch();
    }
  }, [
    marketStreamCandle,
    onLatestCandleClosePriceChange,
    scheduleClosedCandleFinalizationRefetch,
    selectedInterval,
  ]);

  useEffect(() => {
    return () => {
      clearClosedCandleRefetchTimeout(closedCandleRefetchTimeoutRef);
    };
  }, [selectedInterval, symbol]);

  useEffect(() => {
    liveSeriesRef.current?.applyOptions({
      color: change24h >= 0 ? CHART_COLORS.up : CHART_COLORS.down,
    });
  }, [change24h]);

  useEffect(() => {
    const chart = chartRef.current;
    const candleSeries = candleSeriesRef.current;
    const liveSeries = liveSeriesRef.current;
    const volumeSeries = volumeSeriesRef.current;

    if (!chart || !candleSeries || !liveSeries || !volumeSeries) {
      return;
    }

    if (chartRenderMode === "candles") {
      liveSeries.setData([]);
      candleSeries.setData(candlestickData);
      volumeSeries.setData(volumeData);
    } else if (chartRenderMode === "live-fallback") {
      candleSeries.setData([]);
      volumeSeries.setData([]);
      liveSeries.setData(livePoints);
    } else if (chartRenderMode === "loading-empty") {
      candleSeries.setData([]);
      volumeSeries.setData([]);
      liveSeries.setData([]);
    }

    if (
      appliedInitialViewportKeyRef.current === initialViewportKey ||
      chartRenderMode !== "candles" ||
      historyStatus !== "ready"
    ) {
      return;
    }

    const initialLogicalRange = getLatestVisibleLogicalRange(
      candlestickData.length,
      selectedConfig
    );

    if (initialLogicalRange) {
      const timeScale = chart.timeScale();
      timeScale.applyOptions({
        ...getViewportScaleOptions(candlestickData.length, selectedConfig),
        rightOffset: selectedConfig.rightOffset,
      });
      timeScale.setVisibleLogicalRange(initialLogicalRange);
      appliedInitialViewportKeyRef.current = initialViewportKey;
      setIsAtLatest(true);
    }
  }, [
    candlestickData,
    chartRenderMode,
    historyStatus,
    initialViewportKey,
    livePoints,
    selectedConfig,
    volumeData,
  ]);

  useEffect(() => {
    const chart = chartRef.current;

    if (!chart) {
      return;
    }

    syncIndicatorSeries({
      candlestickData: indicatorCandles,
      chart,
      configs: indicatorConfigs,
      hasFreshHistory,
      refs: indicatorSeriesRefs.current,
    });
  }, [hasFreshHistory, indicatorCandles, indicatorConfigs]);

  useEffect(() => {
    clearPriceLines(positionPriceLinesRef.current);
    clearPriceLines(orderPriceLinesRef.current);
    positionPriceLinesRef.current = [];
    orderPriceLinesRef.current = [];

    const activeSeries =
      chartRenderMode === "candles"
        ? candleSeriesRef.current
        : chartRenderMode === "live-fallback"
          ? liveSeriesRef.current
          : null;

    if (!activeSeries) {
      return;
    }

    positionPriceLinesRef.current = positions.map((position) => ({
      owner: activeSeries,
      line: activeSeries.createPriceLine({
        price: position.entryPrice,
        color:
          position.positionSide === "LONG"
            ? CHART_COLORS.up
            : CHART_COLORS.down,
        lineWidth: 2,
        axisLabelVisible: true,
        title: getPositionPriceLineTitle(
          position,
          formatUsd(position.entryPrice)
        ),
      }),
    }));

    orderPriceLinesRef.current = openOrders
      .map((order) => ({
        order,
        price: order.triggerPrice ?? order.limitPrice,
      }))
      .filter((entry): entry is { order: FuturesOpenOrder; price: number } => entry.price !== null && entry.price !== undefined)
      .map((order) => ({
        owner: activeSeries,
        line: activeSeries.createPriceLine({
          price: order.price,
          color: getOrderPriceLineColor(order.order),
          lineWidth: 1,
          lineStyle: 2,
          axisLabelVisible: true,
          title: getOrderPriceLineTitle(order.order, formatUsd(order.price)),
        }),
      }));
  }, [chartRenderMode, openOrders, positions]);

  useEffect(() => {
    const chart = chartRef.current;
    const candleSeries = candleSeriesRef.current;

    if (!chart || !candleSeries) {
      return;
    }

    const handleVisibleLogicalRangeChange = (
      logicalRange: { from: number; to: number } | null
    ) => {
      setIsAtLatest(
        isViewingLatestRange(chart, candlestickData.length, selectedConfig)
      );

      if (
        !logicalRange ||
        !hasCandleData ||
        historyStatus !== "ready" ||
        !hasNextPage ||
        isFetchingNextPage
      ) {
        return;
      }

      const barsInfo = candleSeries.barsInLogicalRange(logicalRange);
      if (barsInfo !== null && barsInfo.barsBefore < LOAD_MORE_THRESHOLD) {
        void fetchNextPage();
      }
    };

    chart
      .timeScale()
      .subscribeVisibleLogicalRangeChange(handleVisibleLogicalRangeChange);

    return () => {
      chart
        .timeScale()
        .unsubscribeVisibleLogicalRangeChange(handleVisibleLogicalRangeChange);
    };
  }, [
    candlestickData.length,
    fetchNextPage,
    hasCandleData,
    hasNextPage,
    historyStatus,
    isFetchingNextPage,
    selectedConfig,
  ]);

  return (
    <div className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
      <div className="flex items-start justify-between gap-main">
        <div>
          <p className="text-sm-custom font-semibold text-main-dark-gray">
            인터랙티브 차트
          </p>
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
              onClick={() => {
                onSelectedIntervalChange(option.value);
                writeStoredFuturesChartInterval(option.value);
              }}
              type="button"
            >
              {option.label}
            </button>
          );
        })}
      </div>

      <div className="mt-4 flex items-stretch gap-3">
        <div className="flex w-16 shrink-0 flex-col items-center gap-2 rounded-main border border-main-light-gray/70 bg-main-light-gray/20 px-2 py-3">
          <span className="rounded-full bg-white px-2 py-1 text-[10px] font-semibold text-main-dark-gray shadow-sm ring-1 ring-main-light-gray/70">
            {enabledIndicatorCount}/3
          </span>

          <div className="flex flex-col items-center gap-1.5">
            {INDICATOR_TYPES.map((type) => (
              <IndicatorToolbarButton
                controlsEnabled={indicatorControlsEnabled}
                enabled={indicatorConfigs[type].enabled}
                key={type}
                onClick={() =>
                  setIndicatorConfigs((current) =>
                    setIndicatorEnabled(current, type, !current[type].enabled)
                  )
                }
                type={type}
              />
            ))}
            <button
              aria-label="Indicator settings"
              className={[
                "inline-flex h-10 w-10 items-center justify-center rounded-main border transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-main-dark-gray/20",
                isIndicatorSettingsOpen
                  ? "border-main-dark-gray bg-main-dark-gray text-white"
                  : "border-main-light-gray bg-white text-main-dark-gray/70 hover:border-main-dark-gray/25 hover:bg-main-light-gray/25 hover:text-main-dark-gray",
              ].join(" ")}
              onClick={() => setIsIndicatorSettingsOpen(true)}
              title="Indicator settings"
              type="button"
            >
              <Settings2 size={16} />
            </button>
          </div>

          {indicatorFallbackMessage && (
            <span
              className="rounded-full bg-amber-50 px-2 py-1 text-[10px] font-semibold text-amber-700 ring-1 ring-amber-200"
              title={indicatorFallbackMessage}
            >
              대기
            </span>
          )}
        </div>

        <div className="relative h-[620px] min-w-0 flex-1 overflow-hidden rounded-main border border-main-light-gray/70 bg-white">
          <div className="pointer-events-none absolute inset-x-0 top-0 z-10 flex flex-col gap-3 p-3">
            <div className="flex items-start justify-between gap-4">
              <div
                aria-label={`${symbol} OHLC legend`}
                className="rounded-main bg-white/92 px-3 py-2 shadow-sm ring-1 ring-main-light-gray/70 backdrop-blur"
                role="group"
              >
                <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs-custom text-main-dark-gray">
                  <span className="text-[11px] font-semibold uppercase tracking-[0.18em] text-main-dark-gray/55">
                    {symbol}
                  </span>
                  <OhlcLegendValue
                    label="O"
                    tone={ohlcTone}
                    value={formatOhlc(displayedOhlc?.open)}
                  />
                  <OhlcLegendValue
                    label="H"
                    tone={ohlcTone}
                    value={formatOhlc(displayedOhlc?.high)}
                  />
                  <OhlcLegendValue
                    label="L"
                    tone={ohlcTone}
                    value={formatOhlc(displayedOhlc?.low)}
                  />
                  <OhlcLegendValue
                    label="C"
                    tone={ohlcTone}
                    value={formatOhlc(displayedOhlc?.close)}
                  />
                </div>
                {canShowIndicatorValues && (
                  <div className="mt-2 border-t border-main-light-gray/70 pt-2">
                    <button
                      aria-expanded={isIndicatorValuePanelOpen}
                      className="pointer-events-auto inline-flex items-center gap-1.5 text-[11px] font-semibold text-main-dark-gray/60 transition-colors hover:text-main-dark-gray focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-main-dark-gray/20"
                      onClick={() =>
                        setIsIndicatorValuePanelOpen((current) => !current)
                      }
                      type="button"
                    >
                      보조지표 가격
                      {isIndicatorValuePanelOpen ? (
                        <ChevronUp aria-hidden="true" size={13} />
                      ) : (
                        <ChevronDown aria-hidden="true" size={13} />
                      )}
                    </button>
                    {isIndicatorValuePanelOpen && (
                      <IndicatorValuePanel
                        hoveredTime={hoveredTime}
                        values={hoveredIndicatorValues}
                      />
                    )}
                  </div>
                )}
              </div>

              <div className="flex flex-wrap items-center justify-end gap-2">
                <button
                  className={[
                    "pointer-events-auto rounded-main border px-3 py-2 text-xs-custom font-semibold transition-colors",
                    isAtLatest
                      ? "border-main-light-gray bg-white/92 text-main-dark-gray/45"
                      : "border-emerald-300 bg-emerald-50 text-emerald-700",
                  ].join(" ")}
                  onClick={() => {
                    chartRef.current?.timeScale().scrollToRealTime();
                    setIsAtLatest(true);
                  }}
                  type="button"
                >
                  최신 보기
                </button>
                {isFetchingNextPage && (
                  <span className="rounded-full bg-main-dark-gray px-3 py-1 text-[11px] font-semibold text-white shadow-sm">
                    과거 데이터 로딩 중
                  </span>
                )}
              </div>
            </div>
          </div>

          <div
            aria-label={`${symbol} volume legend`}
            className="pointer-events-none absolute left-3 z-10 rounded-main bg-white/92 px-3 py-1.5 text-[11px] font-semibold text-main-dark-gray shadow-sm ring-1 ring-main-light-gray/70 backdrop-blur"
            role="group"
            style={{
              top: Math.min(
                (volumePaneTop ?? DEFAULT_VOLUME_PANE_TOP) +
                  VOLUME_LEGEND_TOP_OFFSET,
                VOLUME_LEGEND_MAX_TOP
              ),
            }}
          >
            <span className="text-main-dark-gray/55">Vol</span>{" "}
            <span className="text-main-dark-gray">
              {formatVolume(displayedVolume)}
            </span>
          </div>

          <div className="h-full w-full" ref={chartContainerRef} />
        </div>
      </div>

      <Modal
        hasBackdropBlur={false}
        isEscapeClose
        isOpen={isIndicatorSettingsOpen}
        onClose={() => setIsIndicatorSettingsOpen(false)}
      >
        <div className="w-[min(720px,calc(100vw-48px))] pr-6">
          <p className="text-sm-custom font-semibold text-main-dark-gray">
            Indicator 설정
          </p>
          <p className="mt-2 text-xs-custom text-main-dark-gray/60">
            최대 3개까지 활성화할 수 있으며, 히스토리가 준비된 차트에서만 표시됩니다.
          </p>

          <div className="mt-5 grid gap-3">
            {INDICATOR_TYPES.map((type) => (
              <IndicatorControl
                config={indicatorConfigs[type]}
                controlsEnabled={indicatorControlsEnabled}
                key={type}
                onPeriodChange={(nextPeriod) =>
                  setIndicatorConfigs((current) =>
                    updateIndicatorPeriod(current, type, nextPeriod)
                  )
                }
                onStdDevChange={(nextStdDev) =>
                  setIndicatorConfigs((current) =>
                    updateBollingerStdDev(current, nextStdDev)
                  )
                }
                onToggle={(enabled) =>
                  setIndicatorConfigs((current) =>
                    setIndicatorEnabled(current, type, enabled)
                  )
                }
              />
            ))}
          </div>

          <div className="mt-5 flex flex-wrap items-center justify-between gap-3 border-t border-main-light-gray pt-4">
            <span className="text-xs-custom font-medium text-main-dark-gray/70">
              활성 indicator {enabledIndicatorCount}/3
            </span>

            <button
              className={[
                "rounded-main border border-main-light-gray bg-white px-3 py-2 text-xs-custom font-semibold text-main-dark-gray transition-colors hover:border-main-dark-gray/25",
                !indicatorControlsEnabled ? "cursor-not-allowed opacity-50" : "",
              ].join(" ")}
              disabled={!indicatorControlsEnabled}
              onClick={() =>
                setIndicatorConfigs((current) => resetIndicators(current))
              }
              type="button"
            >
              초기화
            </button>
          </div>
        </div>
      </Modal>

      {(isLoading || isError || historyStatus === "stale") && (
        <div className="mt-4 rounded-main border border-main-light-gray bg-main-light-gray/30 px-main py-3 text-sm-custom text-main-dark-gray/70">
          {getChartHistoryBannerMessage(isLoading, isError)}
        </div>
      )}
    </div>
  );
}

function IndicatorControl({
  config,
  controlsEnabled,
  onPeriodChange,
  onStdDevChange,
  onToggle,
}: {
  config: IndicatorConfig;
  controlsEnabled: boolean;
  onPeriodChange: (period: number) => void;
  onStdDevChange: (stdDev: number) => void;
  onToggle: (enabled: boolean) => void;
}) {
  return (
    <div className="pointer-events-auto rounded-main bg-white/92 px-3 py-2 shadow-sm ring-1 ring-main-light-gray/70 backdrop-blur">
      <div className="flex items-center gap-2">
        <span className="text-[11px] font-semibold uppercase tracking-[0.16em] text-main-dark-gray/55">
          {config.type === "BOLLINGER" ? "BB" : config.type}
        </span>
        <button
          className={[
            "rounded-full border px-2.5 py-1 text-[11px] font-semibold transition-colors",
            config.enabled
              ? "border-rose-200 bg-rose-50 text-rose-700"
              : "border-main-light-gray text-main-dark-gray/70",
            !controlsEnabled ? "cursor-not-allowed opacity-50" : "",
          ].join(" ")}
          disabled={!controlsEnabled}
          onClick={() => onToggle(!config.enabled)}
          type="button"
        >
          {config.enabled ? "제거" : "추가"}
        </button>
      </div>

      <div className="mt-2 flex flex-wrap items-center gap-2 text-[11px] text-main-dark-gray/75">
        <label className="flex items-center gap-1.5">
          <span>P</span>
          <input
            className="w-16 rounded-md border border-main-light-gray bg-white px-2 py-1 text-right text-[11px] text-main-dark-gray disabled:cursor-not-allowed disabled:bg-main-light-gray/20"
            disabled={!controlsEnabled}
            min={5}
            onChange={(event) => onPeriodChange(Number(event.target.value))}
            type="number"
            value={config.period}
          />
        </label>

        {config.type === "BOLLINGER" && (
          <label className="flex items-center gap-1.5">
            <span>σ</span>
            <input
              className="w-16 rounded-md border border-main-light-gray bg-white px-2 py-1 text-right text-[11px] text-main-dark-gray disabled:cursor-not-allowed disabled:bg-main-light-gray/20"
              disabled={!controlsEnabled}
              max={4}
              min={1}
              onChange={(event) => onStdDevChange(Number(event.target.value))}
              step={0.5}
              type="number"
              value={config.stdDev}
            />
          </label>
        )}
      </div>
    </div>
  );
}

function IndicatorValuePanel({
  hoveredTime,
  values,
}: {
  hoveredTime: Time | null;
  values: IndicatorValueRow[];
}) {
  if (hoveredTime == null) {
    return (
      <p className="mt-1 text-[11px] font-medium text-main-dark-gray/45">
        캔들에 커서를 올리면 표시됩니다.
      </p>
    );
  }

  if (values.length === 0) {
    return (
      <p className="mt-1 text-[11px] font-medium text-main-dark-gray/45">
        해당 캔들에는 계산된 값이 없습니다.
      </p>
    );
  }

  return (
    <div className="mt-1 flex max-w-[360px] flex-wrap gap-x-3 gap-y-1">
      {values.map((value) => (
        <span
          className="inline-flex items-center gap-1.5 text-[11px] text-main-dark-gray/70"
          key={value.id}
        >
          <span
            aria-hidden="true"
            className="h-2 w-2 rounded-full"
            style={{ backgroundColor: value.color }}
          />
          <span className="font-semibold text-main-dark-gray/50">
            {value.label}
          </span>
          <span className="font-semibold text-main-dark-gray">
            {formatOhlc(value.value)}
          </span>
        </span>
      ))}
    </div>
  );
}

function IndicatorToolbarButton({
  type,
  enabled,
  controlsEnabled,
  onClick,
}: {
  type: IndicatorType;
  enabled: boolean;
  controlsEnabled: boolean;
  onClick: () => void;
}) {
  const shortLabel = type === "BOLLINGER" ? "BB" : type;
  const ariaLabel =
    type === "BOLLINGER" ? "Bollinger Bands indicator toggle" : `${type} indicator toggle`;
  const Icon = getIndicatorIcon(type);

  return (
    <button
      aria-label={ariaLabel}
      aria-pressed={enabled}
      className={[
        "inline-flex h-10 w-10 items-center justify-center rounded-main border transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-main-dark-gray/20",
        enabled
          ? "border-emerald-200 bg-emerald-50 text-emerald-700"
          : "border-main-light-gray bg-white text-main-dark-gray/70 hover:border-main-dark-gray/25 hover:bg-main-light-gray/25 hover:text-main-dark-gray",
        !controlsEnabled ? "cursor-not-allowed opacity-50" : "",
      ].join(" ")}
      disabled={!controlsEnabled}
      onClick={onClick}
      title={shortLabel}
      type="button"
    >
      <Icon aria-hidden="true" size={16} />
    </button>
  );
}

function getIndicatorIcon(type: IndicatorType): LucideIcon {
  switch (type) {
    case "EMA":
      return ChartLine;
    case "SMA":
      return Activity;
    case "BOLLINGER":
      return ChartArea;
  }
}

function OhlcLegendValue({
  label,
  tone,
  value,
}: {
  label: string;
  tone: OhlcTone;
  value: string;
}) {
  return (
    <span className="flex items-center gap-1.5">
      <span className="text-main-dark-gray/55">{label}</span>
      <span className={getOhlcToneClassName(tone)}>{value}</span>
    </span>
  );
}

function buildCandleRequestUrl(
  symbol: MarketSymbol,
  interval: FuturesCandleInterval,
  limit: number,
  before?: string
): string {
  const params = new URLSearchParams({
    interval,
    limit: String(limit),
  });

  if (before) {
    params.set("before", before);
  }

  return `/proxy-futures/markets/${symbol}/candles?${params.toString()}`;
}

function isCandleResponse(value: unknown): value is CandleResponse {
  if (!value || typeof value !== "object") {
    return false;
  }

  const candidate = value as Partial<CandleResponse>;

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

function getChartHistoryBannerMessage(
  isLoading: boolean,
  isError: boolean
): string {
  if (isLoading) {
    return "차트 히스토리를 불러오는 중입니다.";
  }

  if (isError) {
    return "차트 히스토리 응답을 받지 못해 실시간 가격선만 표시합니다.";
  }

  return "차트 히스토리 시간이 현재 기준으로 너무 오래되어 실시간 가격선만 표시합니다.";
}

function getOhlcTone(snapshot: OhlcSnapshot | null): OhlcTone {
  if (!snapshot) {
    return "neutral";
  }

  if (snapshot.close > snapshot.open) {
    return "bullish";
  }

  if (snapshot.close < snapshot.open) {
    return "bearish";
  }

  return "neutral";
}

function getOhlcToneClassName(tone: OhlcTone): string {
  switch (tone) {
    case "bullish":
      return "font-semibold text-emerald-600";
    case "bearish":
      return "font-semibold text-rose-600";
    case "neutral":
      return "font-semibold text-main-dark-gray";
  }
}

function clearPriceLines(lines: OwnedPriceLine[]): void {
  for (const { line, owner } of lines) {
    owner.removePriceLine(line);
  }
}

function clearClosedCandleRefetchTimeout(
  timeoutRef: MutableRefObject<number | null>
): void {
  if (timeoutRef.current === null) {
    return;
  }

  window.clearTimeout(timeoutRef.current);
  timeoutRef.current = null;
}

function formatOhlc(value: number | undefined): string {
  return typeof value === "number" ? formatUsd(value) : "-";
}

function formatVolume(value: number | null): string {
  if (value === null || !Number.isFinite(value)) {
    return "-";
  }

  return new Intl.NumberFormat("en-US", {
    compactDisplay: "short",
    maximumFractionDigits: 2,
    notation: "compact",
  }).format(value);
}

function toOhlcSnapshot(candle: CandleResponse | null): OhlcSnapshot | null {
  if (!candle) {
    return null;
  }

  return {
    open: candle.openPrice,
    high: candle.highPrice,
    low: candle.lowPrice,
    close: candle.closePrice,
  };
}

function toUnixSeconds(value: string | number): UTCTimestamp {
  const timestamp =
    typeof value === "number" ? Math.floor(value / 1000) : Date.parse(value) / 1000;
  return Math.floor(timestamp) as UTCTimestamp;
}
