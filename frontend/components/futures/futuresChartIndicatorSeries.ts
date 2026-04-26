import {
  LineSeries,
  type IChartApi,
  type ISeriesApi,
  type SeriesPartialOptionsMap,
  type Time,
} from "lightweight-charts";
import {
  calculateBollingerBands,
  calculateEmaSeries,
  calculateSmaSeries,
  canEditIndicators,
  INDICATOR_COLORS,
  type IndicatorCandle,
  type IndicatorConfigs,
} from "./futuresChartIndicators";

type IndicatorSeriesRefs = {
  bollingerBasis: ISeriesApi<"Line"> | null;
  bollingerLower: ISeriesApi<"Line"> | null;
  bollingerUpper: ISeriesApi<"Line"> | null;
  ema: ISeriesApi<"Line"> | null;
  sma: ISeriesApi<"Line"> | null;
};

type SyncIndicatorSeriesInput = {
  candlestickData: IndicatorCandle[];
  chart: IChartApi;
  configs: IndicatorConfigs;
  hasFreshHistory: boolean;
  refs: IndicatorSeriesRefs;
};

export function createIndicatorSeriesRefs(): IndicatorSeriesRefs {
  return {
    bollingerBasis: null,
    bollingerLower: null,
    bollingerUpper: null,
    ema: null,
    sma: null,
  };
}

export function syncIndicatorSeries({
  candlestickData,
  chart,
  configs,
  hasFreshHistory,
  refs,
}: SyncIndicatorSeriesInput): void {
  if (!canEditIndicators(hasFreshHistory) || candlestickData.length === 0) {
    clearIndicatorSeries(chart, refs);
    return;
  }

  syncSingleLineSeries({
    chart,
    data: configs.EMA.enabled
      ? calculateEmaSeries(candlestickData, configs.EMA.period)
      : [],
    options: {
      color: INDICATOR_COLORS.ema,
      lastValueVisible: true,
      lineWidth: 2,
      priceLineVisible: false,
      title: "",
    },
    refs,
    refKey: "ema",
  });

  syncSingleLineSeries({
    chart,
    data: configs.SMA.enabled
      ? calculateSmaSeries(candlestickData, configs.SMA.period)
      : [],
    options: {
      color: INDICATOR_COLORS.sma,
      lastValueVisible: true,
      lineWidth: 2,
      priceLineVisible: false,
      title: "",
    },
    refs,
    refKey: "sma",
  });

  if (!configs.BOLLINGER.enabled) {
    removeLineSeries(chart, refs, "bollingerBasis");
    removeLineSeries(chart, refs, "bollingerUpper");
    removeLineSeries(chart, refs, "bollingerLower");
    return;
  }

  const bands = calculateBollingerBands(
    candlestickData,
    configs.BOLLINGER.period,
    configs.BOLLINGER.stdDev
  );

  syncSingleLineSeries({
    chart,
    data: bands.basis,
    options: {
      color: INDICATOR_COLORS.bollingerBasis,
      lastValueVisible: true,
      lineStyle: 2,
      lineWidth: 1,
      priceLineVisible: false,
      title: "",
    },
    refs,
    refKey: "bollingerBasis",
  });
  syncSingleLineSeries({
    chart,
    data: bands.upper,
    options: {
      color: INDICATOR_COLORS.bollingerBand,
      lastValueVisible: true,
      lineWidth: 1,
      priceLineVisible: false,
      title: "",
    },
    refs,
    refKey: "bollingerUpper",
  });
  syncSingleLineSeries({
    chart,
    data: bands.lower,
    options: {
      color: INDICATOR_COLORS.bollingerBand,
      lastValueVisible: true,
      lineWidth: 1,
      priceLineVisible: false,
      title: "",
    },
    refs,
    refKey: "bollingerLower",
  });
}

export function clearIndicatorSeries(
  chart: IChartApi,
  refs: IndicatorSeriesRefs
): void {
  removeLineSeries(chart, refs, "ema");
  removeLineSeries(chart, refs, "sma");
  removeLineSeries(chart, refs, "bollingerBasis");
  removeLineSeries(chart, refs, "bollingerUpper");
  removeLineSeries(chart, refs, "bollingerLower");
}

function syncSingleLineSeries({
  chart,
  data,
  options,
  refs,
  refKey,
}: {
  chart: IChartApi;
  data: { time: Time; value: number }[];
  options: SeriesPartialOptionsMap["Line"];
  refs: IndicatorSeriesRefs;
  refKey: keyof IndicatorSeriesRefs;
}): void {
  if (data.length === 0) {
    removeLineSeries(chart, refs, refKey);
    return;
  }

  if (!refs[refKey]) {
    refs[refKey] = chart.addSeries(LineSeries, options);
  } else {
    refs[refKey]?.applyOptions(options);
  }

  refs[refKey]?.setData(data);
}

function removeLineSeries(
  chart: IChartApi,
  refs: IndicatorSeriesRefs,
  key: keyof IndicatorSeriesRefs
): void {
  const series = refs[key];

  if (!series) {
    return;
  }

  chart.removeSeries(series);
  refs[key] = null;
}
