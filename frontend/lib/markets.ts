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
  turnover24hUsdt: number;
  fundingRate: number;
  nextFundingAt: string | null;
  fundingIntervalHours: number | null;
  serverTime: string | null;
  marketCap: number;
  dominance: number;
  hasExtendedMetrics: boolean;
  openInterestLabel: string;
  description: string;
};

export type MarketRankingEntry = {
  rank: number;
  nickname: string;
  walletBalance: number;
  profitRate: number;
  targetToken?: string;
};

export type MarketRankingMemberRank = {
  rank: number;
};

const MARKET_RANK_ICON_PATHS: Record<number, string> = {
  1: "/images/leaderboard/first.webp",
  2: "/images/leaderboard/second.webp",
  3: "/images/leaderboard/third.webp",
  4: "/images/leaderboard/4th.webp",
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
    turnover24hUsdt: 1_280_000_000,
    fundingRate: 0.0001,
    nextFundingAt: null,
    fundingIntervalHours: null,
    serverTime: null,
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
    turnover24hUsdt: 640_000_000,
    fundingRate: 0.00008,
    nextFundingAt: null,
    fundingIntervalHours: null,
    serverTime: null,
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
    walletBalance: 123_500,
    profitRate: 0.235,
  },
  {
    rank: 2,
    nickname: "MoonTrader",
    walletBalance: 117_900,
    profitRate: 0.179,
  },
  {
    rank: 3,
    nickname: "SolRunner",
    walletBalance: 114_400,
    profitRate: 0.144,
  },
  {
    rank: 4,
    nickname: "DeltaPulse",
    walletBalance: 111_100,
    profitRate: 0.111,
  },
];

export const MARKET_SNAPSHOT_LIST = SUPPORTED_MARKET_SYMBOLS.map(
  (symbol) => MARKET_SNAPSHOTS[symbol]
);

export function getMarketLogoPath(symbol: MarketSymbol) {
  if (symbol === "ETHUSDT") {
    return "/images/logo/ethereum.webp";
  }

  return "/images/logo/bitcoin.webp";
}

export function getMarketRankIconPath(rank: number) {
  return MARKET_RANK_ICON_PATHS[rank] ?? null;
}

export function formatMarketRank(rank: number | null | undefined) {
  return rank == null ? "집계 중" : `${rank.toLocaleString("ko-KR")}위`;
}

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

export function formatRatioPercent(value: number, fractionDigits = 2) {
  const percentValue = value * 100;
  return `${percentValue >= 0 ? "+" : ""}${percentValue.toFixed(fractionDigits)}%`;
}
