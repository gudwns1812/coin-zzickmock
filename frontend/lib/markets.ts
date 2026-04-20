export const SUPPORTED_MARKET_SYMBOLS = ["BTCUSDT", "ETHUSDT"] as const;

export type MarketSymbol = (typeof SUPPORTED_MARKET_SYMBOLS)[number];

export type MarketSnapshot = {
  symbol: MarketSymbol;
  assetName: string;
  displayName: string;
  lastPrice: number;
  markPrice: number;
  indexPrice: number;
  change24h: number;
  volume24h: number;
  fundingRate: number;
  marketCap: number;
  dominance: number;
  hasExtendedMetrics: boolean;
  openInterestLabel: string;
  description: string;
};

export type MarketRankingEntry = {
  rank: number;
  nickname: string;
  totalAsset: number;
  profitRate: number;
};

export const MARKET_SNAPSHOTS: Record<MarketSymbol, MarketSnapshot> = {
  BTCUSDT: {
    symbol: "BTCUSDT",
    assetName: "Bitcoin",
    displayName: "Bitcoin Perpetual",
    lastPrice: 102_450,
    markPrice: 102_418,
    indexPrice: 102_401,
    change24h: 2.84,
    volume24h: 1_280_000_000,
    fundingRate: 0.0001,
    marketCap: 1_320_000_000_000,
    dominance: 54.2,
    hasExtendedMetrics: false,
    openInterestLabel: "상위 관심도",
    description: "변동성이 가장 큰 대표 심볼. 주문 규칙과 청산 구조를 학습하기 좋습니다.",
  },
  ETHUSDT: {
    symbol: "ETHUSDT",
    assetName: "Ethereum",
    displayName: "Ethereum Perpetual",
    lastPrice: 3_280,
    markPrice: 3_276,
    indexPrice: 3_274,
    change24h: 1.72,
    volume24h: 640_000_000,
    fundingRate: 0.00008,
    marketCap: 389_200_000_000,
    dominance: 17.8,
    hasExtendedMetrics: false,
    openInterestLabel: "안정적 유동성",
    description: "BTC보다 완만한 흐름으로 레버리지와 마진 모드 차이를 비교하기 좋습니다.",
  },
};

export const MARKET_RANKING_FALLBACKS: MarketRankingEntry[] = [
  {
    rank: 1,
    nickname: "CryptoKing99",
    totalAsset: 2_450_000,
    profitRate: 245.8,
  },
  {
    rank: 2,
    nickname: "MoonTrader",
    totalAsset: 1_890_000,
    profitRate: 189.3,
  },
  {
    rank: 3,
    nickname: "SolRunner",
    totalAsset: 1_540_000,
    profitRate: 152.4,
  },
  {
    rank: 4,
    nickname: "DeltaPulse",
    totalAsset: 1_210_000,
    profitRate: 118.6,
  },
];

export const MARKET_SNAPSHOT_LIST = SUPPORTED_MARKET_SYMBOLS.map(
  (symbol) => MARKET_SNAPSHOTS[symbol]
);

export function isSupportedMarketSymbol(
  value: string
): value is MarketSymbol {
  return SUPPORTED_MARKET_SYMBOLS.includes(value as MarketSymbol);
}

export function formatUsd(value: number) {
  let fractionDigits = 2;
  if (value >= 10000) {
    fractionDigits = 1;
  } else if (value >= 1000) {
    fractionDigits = 2;
  } else if (value >= 100) {
    fractionDigits = 3;
  } else {
    fractionDigits = 4;
  }

  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  }).format(value);
}

export function formatCompactUsd(value: number) {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    notation: "compact",
    maximumFractionDigits: value >= 100 ? 1 : 2,
  }).format(value);
}

export function formatSignedUsd(value: number) {
  const sign = value > 0 ? "+" : value < 0 ? "-" : "";
  return `${sign}${formatUsd(Math.abs(value))}`;
}

export function formatPercent(value: number) {
  const formatted = `${value >= 0 ? "+" : ""}${value.toFixed(2)}%`;
  return formatted;
}
