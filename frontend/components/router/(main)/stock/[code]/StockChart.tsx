"use client";

import { useEffect, useRef, useState, useCallback, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { usePortfolioStore } from "@/store/usePortfolio";
import {
  createChart,
  IChartApi,
  ISeriesApi,
  CandlestickSeries,
  HistogramSeries,
  LineSeries,
} from "lightweight-charts";
import { IntervalKey } from "./IntervalSelector";
import { RealTimeStockData } from "@/hooks/useRealTimeStock";

// 시장 오픈 여부 체크 함수
const isMarketOpen = (now: Date = new Date()) => {
  const dayOfWeek = now.getDay(); // 0=일요일, 6=토요일
  if (dayOfWeek === 0 || dayOfWeek === 6) return false; // 주말

  const hour = now.getHours();
  const minute = now.getMinutes();
  const currentTime = hour * 100 + minute;

  return currentTime >= 900 && currentTime <= 1530; // 09:00 ~ 15:30
};

// 상수 정의
const CHART_CONSTANTS = {
  HEIGHT: 320,
  DEBOUNCE_MS: 300,
  TIME_MARGIN: 15,
  STALE_TIME: 5 * 60 * 1000,
  GC_TIME: 10 * 60 * 1000,
  COLORS: {
    UP: "rgb(240, 66, 81)",
    DOWN: "rgb(52, 133, 250)",
    VOLUME_UP: "rgba(240,66,81,0.6)",
    VOLUME_DOWN: "rgba(52,133,250,0.6)",
    VOLUME_BASE: "rgba(52,133,250,0.3)",
    GRID: "#e0e0e0",
    TEXT: "#333",
    AVERAGE_LINE: "orange",
  },
  LOAD_PERIODS: {
    D: { initial: 1, additional: 0.5 }, // 1년, 6개월 (~250개 포인트)
    W: { initial: 10, additional: 3 }, // 10년, 3년 (~520개 포인트)
    M: { initial: 25, additional: 5 }, // 25년, 5년 (~300개 포인트)
    Y: { initial: 100, additional: 10 }, // 100년, 10년 (~100개 포인트)
  },
} as const;

interface StockData {
  stockCode: string;
  date: [number, number, number]; // [year, month, day]
  type: string | null;
  open: string;
  high: string;
  low: string;
  close: string;
  volume: string;
  volumeAmount: string;
  prevPrice: number;
  openFromPrev: number;
  closeFromPrev: number;
  highFromPrev: number;
  lowFromPrev: number;
}

interface CandleData {
  time: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

interface StockChartProps {
  code: string;
  selectedInterval: IntervalKey;
  realTimeData?: RealTimeStockData;
}

// 유틸리티 함수들을 컴포넌트 외부로 이동
const formatDate = (date: Date): string => {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(
    2,
    "0"
  )}-${String(date.getDate()).padStart(2, "0")}`;
};

const convertStockDataToCandles = (stockData: StockData[]): CandleData[] => {
  return stockData.map((item) => {
    const [year, month, day] = item.date;
    const dateString = `${year}-${String(month).padStart(2, "0")}-${String(
      day
    ).padStart(2, "0")}`;

    return {
      time: dateString,
      open: parseFloat(item.open),
      high: parseFloat(item.high),
      low: parseFloat(item.low),
      close: parseFloat(item.close),
      volume: parseFloat(item.volume),
    };
  });
};

const createDateRange = (period: IntervalKey, isAdditional = false) => {
  const endDate = new Date();
  const startDate = new Date();
  const config = CHART_CONSTANTS.LOAD_PERIODS[period];
  const years = isAdditional ? config.additional : config.initial;

  if (period === "D" && isAdditional) {
    startDate.setMonth(startDate.getMonth() - Math.floor(years * 12));
  } else {
    startDate.setFullYear(startDate.getFullYear() - years);
  }

  return {
    startDate: formatDate(startDate),
    endDate: formatDate(endDate),
  };
};

// 중복 제거 최적화된 함수
const removeDuplicatesAndSort = (data: CandleData[]): CandleData[] => {
  const timeMap = new Map<string, CandleData>();

  data.forEach((item) => {
    timeMap.set(item.time, item);
  });

  return Array.from(timeMap.values()).sort(
    (a, b) => new Date(a.time).getTime() - new Date(b.time).getTime()
  );
};

const StockChart = ({
  code,
  selectedInterval,
  realTimeData,
}: StockChartProps) => {
  const chartContainerRef = useRef<HTMLDivElement>(null);
  const chartRef = useRef<IChartApi | null>(null);
  const candlestickRef = useRef<ISeriesApi<"Candlestick"> | null>(null);
  const volumeRef = useRef<ISeriesApi<"Histogram"> | null>(null);
  const tooltipRef = useRef<HTMLDivElement>(null);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [allLoadedData, setAllLoadedData] = useState<{
    [key: string]: CandleData[];
  }>({});

  const loadedStartDatesRef = useRef<{
    [key in IntervalKey]?: Set<string>;
  }>({});
  const isLoadingRef = useRef(false);

  const { portfolio } = usePortfolioStore();

  // 메모이제이션된 계산들
  const myStock = useMemo(
    () => portfolio.find((stock: any) => stock.stockCode === code),
    [portfolio, code]
  );

  const isMyStock = useMemo(() => Boolean(myStock), [myStock]);

  // useQuery 로직 통합
  const createStockQuery = useCallback(
    (period: IntervalKey) => ({
      queryKey: ["stockData", code, period],
      queryFn: async () => {
        const { startDate, endDate } = createDateRange(period);
        const res = await fetch(
          `/proxy2/v2/stocks/${code}?period=${period}&startDate=${startDate}&endDate=${endDate}`
        );
        if (!res.ok) throw new Error("주식 데이터를 불러오는데 실패했습니다.");
        const json = await res.json();
        return json.data as StockData[];
      },
      enabled: !!code,
      staleTime: CHART_CONSTANTS.STALE_TIME,
      gcTime: CHART_CONSTANTS.GC_TIME,
    }),
    [code]
  );

  const { data: stockDataD = [] } = useQuery(createStockQuery("D"));
  const { data: stockDataW = [] } = useQuery(createStockQuery("W"));
  const { data: stockDataM = [] } = useQuery(createStockQuery("M"));
  const { data: stockDataY = [] } = useQuery(createStockQuery("Y"));

  // 이전 데이터 로드 함수 (최적화된 버전)
  const loadMoreData = useCallback(
    async (period: IntervalKey, earliestTime: string) => {
      if (isLoadingRef.current) return;

      if (!loadedStartDatesRef.current[period]) {
        loadedStartDatesRef.current[period] = new Set([earliestTime]);
      } else if (loadedStartDatesRef.current[period]?.has(earliestTime)) {
        return;
      } else {
        loadedStartDatesRef.current[period]?.add(earliestTime);
      }

      isLoadingRef.current = true;
      setIsLoadingMore(true);

      try {
        const earliestDate = new Date(earliestTime);
        const extendedStartDate = new Date(earliestDate);
        const config = CHART_CONSTANTS.LOAD_PERIODS[period];

        if (period === "D") {
          extendedStartDate.setMonth(
            extendedStartDate.getMonth() - Math.floor(config.additional * 12)
          );
        } else {
          extendedStartDate.setFullYear(
            extendedStartDate.getFullYear() - config.additional
          );
        }

        const startDateStr = formatDate(extendedStartDate);

        const res = await fetch(
          `/proxy2/v2/stocks/${code}?period=${period}&startDate=${startDateStr}&endDate=${earliestTime}`
        );

        if (!res.ok)
          throw new Error("추가 주식 데이터를 불러오는데 실패했습니다.");

        const json = await res.json();
        const newStockData = json.data as StockData[];

        if (newStockData?.length > 0) {
          const newCandles = convertStockDataToCandles(newStockData);

          setAllLoadedData((prev) => {
            const currentData = prev[period] || [];
            const mergedData = [...newCandles, ...currentData];
            const uniqueData = removeDuplicatesAndSort(mergedData);

            return {
              ...prev,
              [period]: uniqueData,
            };
          });
        }
      } catch (error) {
        console.error("추가 데이터 로드 실패:", error);
      } finally {
        isLoadingRef.current = false;
        setIsLoadingMore(false);
      }
    },
    [code]
  );

  // 차트 설정 객체
  const chartConfig = useMemo(
    () => ({
      width: 0, // 동적으로 설정됨
      height: CHART_CONSTANTS.HEIGHT,
      layout: {
        background: { color: "transparent" },
        textColor: CHART_CONSTANTS.COLORS.TEXT,
      },
      grid: {
        vertLines: { color: CHART_CONSTANTS.COLORS.GRID },
        horzLines: { color: CHART_CONSTANTS.COLORS.GRID },
      },
      rightPriceScale: {
        borderColor: CHART_CONSTANTS.COLORS.GRID,
      },
      timeScale: {
        borderColor: CHART_CONSTANTS.COLORS.GRID,
        timeVisible: true,
        secondsVisible: false,
      },
      crosshair: {
        mode: 1,
      },
    }),
    []
  );

  // 차트 업데이트 useEffect
  useEffect(() => {
    const currentData = allLoadedData[selectedInterval];
    if (!currentData?.length) return;

    candlestickRef.current?.setData(currentData);

    // volumeData를 직접 계산하여 dependency 제거
    const volumeData = currentData.map((candle) => ({
      time: candle.time,
      value: candle.volume,
      color:
        candle.close >= candle.open
          ? CHART_CONSTANTS.COLORS.VOLUME_UP
          : CHART_CONSTANTS.COLORS.VOLUME_DOWN,
    }));
    volumeRef.current?.setData(volumeData);
  }, [allLoadedData, selectedInterval]);

  // 차트 초기화
  useEffect(() => {
    if (!chartContainerRef.current) return;

    const chart = createChart(chartContainerRef.current, {
      ...chartConfig,
      width: chartContainerRef.current.clientWidth,
    });

    chartRef.current = chart;

    // 커서 변경 및 툴팁 표시
    chart.subscribeCrosshairMove((param) => {
      if (!chartContainerRef.current || !tooltipRef.current) return;
      const container = chartContainerRef.current;
      const tooltip = tooltipRef.current;

      if (param.point === undefined || !param.time) {
        container.style.cursor = "default";
        tooltip.style.display = "none";
        return;
      }

      container.style.cursor = "crosshair";

      const data = param.seriesData.get(candlestickRef.current!);
      if (!data) {
        tooltip.style.display = "none";
        return;
      }

      // 툴팁 내용 업데이트
      const tooltipContent = `
        <div style="margin-bottom: 4px;"><strong>날짜:</strong> ${
          param.time
        }</div>
        ${
          "open" in data
            ? `<div style="margin-bottom: 2px;"><strong>시가:</strong> ${data.open.toLocaleString()}원</div>`
            : ""
        }
        ${
          "high" in data
            ? `<div style="margin-bottom: 2px;"><strong>고가:</strong> ${data.high.toLocaleString()}원</div>`
            : ""
        }
        ${
          "low" in data
            ? `<div style="margin-bottom: 2px;"><strong>저가:</strong> ${data.low.toLocaleString()}원</div>`
            : ""
        }
        ${
          "close" in data
            ? `<div><strong>종가:</strong> ${data.close.toLocaleString()}원</div>`
            : ""
        }
      `;

      tooltip.innerHTML = tooltipContent;
      tooltip.style.display = "block";

      // 툴팁 위치 설정
      const containerRect = container.getBoundingClientRect();
      const left = param.point.x + 15;
      const top = param.point.y - 10;

      // 화면 경계 체크
      const tooltipRect = tooltip.getBoundingClientRect();
      const finalLeft =
        left + tooltipRect.width > containerRect.width
          ? param.point.x - tooltipRect.width - 15
          : left;
      const finalTop = top < 0 ? param.point.y + 15 : top;

      tooltip.style.left = `${finalLeft}px`;
      tooltip.style.top = `${finalTop}px`;
    });

    const candlestickSeries = chart.addSeries(CandlestickSeries, {
      wickDownColor: CHART_CONSTANTS.COLORS.DOWN,
      downColor: CHART_CONSTANTS.COLORS.DOWN,
      wickUpColor: CHART_CONSTANTS.COLORS.UP,
      upColor: CHART_CONSTANTS.COLORS.UP,
      borderVisible: false,
      priceScaleId: "right",
    });
    candlestickSeries.priceScale().applyOptions({
      scaleMargins: { top: 0.1, bottom: 0.3 },
    });
    candlestickRef.current = candlestickSeries;

    const lineSeries = chart.addSeries(LineSeries, {
      priceScaleId: "right",
    });

    if (isMyStock && myStock) {
      lineSeries.setData([
        {
          time: new Date().toISOString().split("T")[0],
          value: myStock.entryPrice,
        },
      ]);

      lineSeries.createPriceLine({
        price: myStock.entryPrice,
        color: CHART_CONSTANTS.COLORS.AVERAGE_LINE,
        lineWidth: 2,
        lineStyle: 2, // Dashed line
        axisLabelVisible: true,
        title: "평균 매입가",
      });
    }

    const volumeSeries = chart.addSeries(HistogramSeries, {
      priceFormat: { type: "volume" },
      color: CHART_CONSTANTS.COLORS.VOLUME_BASE,
      priceScaleId: "volume",
    });
    chart.priceScale("volume").applyOptions({
      scaleMargins: { top: 0.8, bottom: 0 },
      alignLabels: false,
      visible: false,
    });
    volumeRef.current = volumeSeries;

    return () => {
      if (tooltipRef.current) {
        tooltipRef.current.style.display = "none";
      }
      chart.remove();
    };
  }, [isMyStock, myStock, chartConfig]);

  // 스크롤 이벤트 구독 (최적화된 버전)
  useEffect(() => {
    if (!chartRef.current) return;

    const chart = chartRef.current;
    let debounceTimeout: NodeJS.Timeout | null = null;

    // 디바운싱된 스크롤 핸들러
    const handleVisibleRangeChange = (newVisibleTimeRange: any) => {
      if (debounceTimeout) {
        clearTimeout(debounceTimeout);
      }

      debounceTimeout = setTimeout(() => {
        if (!newVisibleTimeRange) return;

        setAllLoadedData((currentAllData) => {
          const currentData = currentAllData[selectedInterval];
          if (!currentData?.length) return currentAllData;

          const earliestDataTime = currentData[0].time;
          const visibleFrom = newVisibleTimeRange.from as string;

          // 사용자가 차트의 왼쪽 끝 근처로 스크롤했을 때 추가 데이터 로드
          const fromIndex = currentData.findIndex(
            (item) => item.time >= visibleFrom
          );

          if (
            fromIndex <= CHART_CONSTANTS.TIME_MARGIN &&
            !isLoadingRef.current
          ) {
            loadMoreData(selectedInterval, earliestDataTime);
          }

          return currentAllData; // 상태 변경 없음
        });
      }, CHART_CONSTANTS.DEBOUNCE_MS);
    };

    chart.timeScale().subscribeVisibleTimeRangeChange(handleVisibleRangeChange);

    return () => {
      if (debounceTimeout) {
        clearTimeout(debounceTimeout);
      }
    };
  }, [selectedInterval]);

  // 캔들 데이터 업데이트
  useEffect(() => {
    const stockDataMap = {
      D: stockDataD,
      W: stockDataW,
      M: stockDataM,
      Y: stockDataY,
    };

    const stockArr = stockDataMap[selectedInterval];
    if (!Array.isArray(stockArr) || !stockArr.length) return;

    const newCandles = convertStockDataToCandles(stockArr);

    setAllLoadedData((prev) => ({
      ...prev,
      [selectedInterval]: newCandles,
    }));
  }, [stockDataD, stockDataW, stockDataM, stockDataY, selectedInterval]);

  // 실시간 데이터 업데이트
  useEffect(() => {
    if (!realTimeData || !candlestickRef.current || !volumeRef.current) return;

    // 함수형 업데이트를 사용하여 최신 상태 참조
    setAllLoadedData((prevAllData) => {
      const currentData = prevAllData[selectedInterval];
      if (!currentData?.length) return prevAllData;

      // 오늘 날짜 생성
      const today = new Date().toISOString().split("T")[0];
      const lastDataIndex = currentData.length - 1;
      const lastCandle = currentData[lastDataIndex];

      let updatedData: CandleData[];

      // 마지막 캔들이 오늘 날짜인지 확인
      if (lastCandle.time === today) {
        // 기존 오늘 캔들을 실시간 데이터로 업데이트
        const updatedCandle: CandleData = {
          time: today,
          open: lastCandle.open, // 시가는 유지
          high: Math.max(lastCandle.high, parseFloat(realTimeData.price)),
          low: Math.min(lastCandle.low, parseFloat(realTimeData.price)),
          close: parseFloat(realTimeData.price), // 현재가를 종가로
          volume: parseFloat(realTimeData.volume),
        };

        updatedData = [...currentData];
        updatedData[lastDataIndex] = updatedCandle;
      } else {
        // 오늘 날짜의 새로운 캔들 생성
        const newCandle: CandleData = {
          time: today,
          open: parseFloat(realTimeData.openPrice),
          high: parseFloat(realTimeData.highPrice),
          low: parseFloat(realTimeData.lowPrice),
          close: parseFloat(realTimeData.price),
          volume: parseFloat(realTimeData.volume),
        };

        updatedData = [...currentData, newCandle];
      }

      // 차트 데이터 업데이트
      candlestickRef.current?.setData(updatedData);

      // 볼륨 데이터도 업데이트
      const volumeData = updatedData.map((candle) => ({
        time: candle.time,
        value: candle.volume,
        color:
          candle.close >= candle.open
            ? CHART_CONSTANTS.COLORS.VOLUME_UP
            : CHART_CONSTANTS.COLORS.VOLUME_DOWN,
      }));
      volumeRef.current?.setData(volumeData);

      return {
        ...prevAllData,
        [selectedInterval]: updatedData,
      };
    });
  }, [realTimeData, selectedInterval]);

  // interval 변경 시 실시간 스크롤
  useEffect(() => {
    if (chartRef.current) {
      chartRef.current.timeScale().scrollToRealTime();
    }

    // 현재 로딩 상태만 초기화 (로드 기록은 유지)
    isLoadingRef.current = false;
    setIsLoadingMore(false);
  }, [selectedInterval]);

  // 부모 크기 변경 감지 및 차트 리사이즈
  useEffect(() => {
    if (!chartContainerRef.current || !chartRef.current) return;
    const container = chartContainerRef.current;
    const chart = chartRef.current;

    const resize = () => {
      chart.resize(container.clientWidth, container.clientHeight);
    };

    resize();

    const observer = new window.ResizeObserver(resize);
    observer.observe(container);

    return () => {
      observer.disconnect();
    };
  }, []);

  const marketOpen = isMarketOpen();

  return (
    <div className="h-[320px] relative">
      <div
        ref={chartContainerRef}
        style={{ width: "100%", height: "100%", position: "relative" }}
      />

      {/* 실시간 상태 인디케이터 */}
      <div className="absolute top-0 left-main flex items-center gap-2">
        {marketOpen && realTimeData && (
          <div className="flex items-center gap-1 bg-green-100 text-green-800 px-2 py-1 rounded-full text-xs font-medium">
            <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
            20초 주기
          </div>
        )}
        {!marketOpen && (
          <div className="flex items-center gap-1 bg-gray-100 text-gray-600 px-2 py-1 rounded-full text-xs font-medium">
            <div className="w-2 h-2 bg-gray-400 rounded-full"></div>
            장마감
          </div>
        )}
      </div>

      {/* 툴팁 */}
      <div
        ref={tooltipRef}
        style={{
          position: "absolute",
          display: "none",
          padding: "8px 12px",
          backgroundColor: "rgba(0, 0, 0, 0.8)",
          color: "white",
          borderRadius: "6px",
          fontSize: "12px",
          lineHeight: "1.4",
          pointerEvents: "none",
          zIndex: 1000,
          minWidth: "120px",
          boxShadow: "0 2px 8px rgba(0, 0, 0, 0.2)",
        }}
      />

      {isLoadingMore && (
        <div className="absolute top-2 left-2 bg-blue-500 text-white px-2 py-1 rounded text-sm">
          이전 데이터 로딩 중...
        </div>
      )}
    </div>
  );
};

export default StockChart;
