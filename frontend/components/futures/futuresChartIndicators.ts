import type { LineData, Time } from "lightweight-charts";

export type IndicatorType = "EMA" | "SMA" | "BOLLINGER";

export type MovingAverageIndicatorConfig<T extends "EMA" | "SMA"> = {
  type: T;
  enabled: boolean;
  period: number;
};

export type BollingerIndicatorConfig = {
  type: "BOLLINGER";
  enabled: boolean;
  period: number;
  stdDev: number;
};

export type IndicatorConfig =
  | MovingAverageIndicatorConfig<"EMA">
  | MovingAverageIndicatorConfig<"SMA">
  | BollingerIndicatorConfig;

export type IndicatorConfigs = {
  EMA: MovingAverageIndicatorConfig<"EMA">;
  SMA: MovingAverageIndicatorConfig<"SMA">;
  BOLLINGER: BollingerIndicatorConfig;
};

export type IndicatorCandle = {
  time: Time;
  close: number;
};

export type BollingerBandSeries = {
  basis: LineData<Time>[];
  lower: LineData<Time>[];
  upper: LineData<Time>[];
};

export const MAX_ACTIVE_INDICATORS = 3;

export const DEFAULT_INDICATOR_CONFIGS: IndicatorConfigs = {
  EMA: {
    type: "EMA",
    enabled: false,
    period: 20,
  },
  SMA: {
    type: "SMA",
    enabled: false,
    period: 20,
  },
  BOLLINGER: {
    type: "BOLLINGER",
    enabled: false,
    period: 20,
    stdDev: 2,
  },
};

export function getEnabledIndicatorCount(configs: IndicatorConfigs): number {
  return Object.values(configs).filter((indicator) => indicator.enabled).length;
}

export function setIndicatorEnabled(
  configs: IndicatorConfigs,
  type: IndicatorType,
  enabled: boolean
): IndicatorConfigs {
  if (!enabled) {
    return {
      ...configs,
      [type]: {
        ...configs[type],
        enabled: false,
      },
    };
  }

  const canEnableIndicator =
    configs[type].enabled ||
    getEnabledIndicatorCount(configs) < MAX_ACTIVE_INDICATORS;

  if (canEnableIndicator) {
    return {
      ...configs,
      [type]: {
        ...configs[type],
        enabled: true,
      },
    };
  }

  return configs;
}

export function updateIndicatorPeriod(
  configs: IndicatorConfigs,
  type: IndicatorType,
  period: number
): IndicatorConfigs {
  switch (type) {
    case "EMA":
      return {
        ...configs,
        EMA: {
          ...configs.EMA,
          period: clampMovingAveragePeriod(period),
        },
      };
    case "SMA":
      return {
        ...configs,
        SMA: {
          ...configs.SMA,
          period: clampMovingAveragePeriod(period),
        },
      };
    case "BOLLINGER":
      return {
        ...configs,
        BOLLINGER: {
          ...configs.BOLLINGER,
          period: clampBollingerPeriod(period),
        },
      };
  }
}

export function updateBollingerStdDev(
  configs: IndicatorConfigs,
  stdDev: number
): IndicatorConfigs {
  return {
    ...configs,
    BOLLINGER: {
      ...configs.BOLLINGER,
      stdDev: clampBollingerStdDev(stdDev),
    },
  };
}

export function resetIndicators(configs?: IndicatorConfigs): IndicatorConfigs {
  return {
    EMA: {
      ...DEFAULT_INDICATOR_CONFIGS.EMA,
      enabled: false,
      period: configs?.EMA.period ?? DEFAULT_INDICATOR_CONFIGS.EMA.period,
    },
    SMA: {
      ...DEFAULT_INDICATOR_CONFIGS.SMA,
      enabled: false,
      period: configs?.SMA.period ?? DEFAULT_INDICATOR_CONFIGS.SMA.period,
    },
    BOLLINGER: {
      ...DEFAULT_INDICATOR_CONFIGS.BOLLINGER,
      enabled: false,
      period:
        configs?.BOLLINGER.period ?? DEFAULT_INDICATOR_CONFIGS.BOLLINGER.period,
      stdDev:
        configs?.BOLLINGER.stdDev ?? DEFAULT_INDICATOR_CONFIGS.BOLLINGER.stdDev,
    },
  };
}

export function canEditIndicators(hasFreshHistory: boolean): boolean {
  return hasFreshHistory;
}

export function getIndicatorFallbackMessage(hasFreshHistory: boolean): string | null {
  if (hasFreshHistory) {
    return null;
  }

  return "히스토리 준비 후 indicator 표시";
}

export function clampMovingAveragePeriod(value: number): number {
  return clampInteger(value, 5, 200);
}

export function clampBollingerPeriod(value: number): number {
  return clampInteger(value, 5, 100);
}

export function clampBollingerStdDev(value: number): number {
  const rounded = Math.round(value * 2) / 2;
  return Math.max(1, Math.min(4, rounded));
}

export function calculateSmaSeries(
  candles: IndicatorCandle[],
  period: number
): LineData<Time>[] {
  const clampedPeriod = clampMovingAveragePeriod(period);
  const results: LineData<Time>[] = [];

  for (let index = clampedPeriod - 1; index < candles.length; index += 1) {
    let total = 0;

    for (let offset = index - clampedPeriod + 1; offset <= index; offset += 1) {
      total += candles[offset].close;
    }

    results.push({
      time: candles[index].time,
      value: total / clampedPeriod,
    });
  }

  return results;
}

export function calculateEmaSeries(
  candles: IndicatorCandle[],
  period: number
): LineData<Time>[] {
  const clampedPeriod = clampMovingAveragePeriod(period);

  if (candles.length < clampedPeriod) {
    return [];
  }

  const multiplier = 2 / (clampedPeriod + 1);
  let seed = 0;

  for (let index = 0; index < clampedPeriod; index += 1) {
    seed += candles[index].close;
  }

  let ema = seed / clampedPeriod;
  const results: LineData<Time>[] = [
    {
      time: candles[clampedPeriod - 1].time,
      value: ema,
    },
  ];

  for (let index = clampedPeriod; index < candles.length; index += 1) {
    ema = (candles[index].close - ema) * multiplier + ema;
    results.push({
      time: candles[index].time,
      value: ema,
    });
  }

  return results;
}

export function calculateBollingerBands(
  candles: IndicatorCandle[],
  period: number,
  stdDev: number
): BollingerBandSeries {
  const clampedPeriod = clampBollingerPeriod(period);
  const clampedStdDev = clampBollingerStdDev(stdDev);
  const basis: LineData<Time>[] = [];
  const upper: LineData<Time>[] = [];
  const lower: LineData<Time>[] = [];

  for (let index = clampedPeriod - 1; index < candles.length; index += 1) {
    const window = candles.slice(index - clampedPeriod + 1, index + 1);
    const mean = window.reduce((sum, candle) => sum + candle.close, 0) / clampedPeriod;
    const variance =
      window.reduce((sum, candle) => sum + (candle.close - mean) ** 2, 0) /
      clampedPeriod;
    const deviation = Math.sqrt(variance) * clampedStdDev;

    basis.push({
      time: candles[index].time,
      value: mean,
    });
    upper.push({
      time: candles[index].time,
      value: mean + deviation,
    });
    lower.push({
      time: candles[index].time,
      value: mean - deviation,
    });
  }

  return { basis, lower, upper };
}

function clampInteger(value: number, min: number, max: number): number {
  const normalized = Number.isFinite(value) ? Math.round(value) : min;
  return Math.max(min, Math.min(max, normalized));
}
