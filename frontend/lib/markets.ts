export const SUPPORTED_MARKET_SYMBOLS = ["BTCUSDT", "ETHUSDT"] as const;

export type MarketSymbol = (typeof SUPPORTED_MARKET_SYMBOLS)[number];

export type MarketSnapshot = {
  symbol: MarketSymbol;
  displayName: string;
  lastPrice: number;
  markPrice: number;
  indexPrice: number;
  change24h: number;
  volume24h: number;
  fundingRate: number;
  openInterestLabel: string;
  description: string;
};

export const MARKET_SNAPSHOTS: Record<MarketSymbol, MarketSnapshot> = {
  BTCUSDT: {
    symbol: "BTCUSDT",
    displayName: "Bitcoin Perpetual",
    lastPrice: 102_450,
    markPrice: 102_418,
    indexPrice: 102_401,
    change24h: 2.84,
    volume24h: 1_280_000_000,
    fundingRate: 0.0001,
    openInterestLabel: "상위 관심도",
    description: "변동성이 가장 큰 대표 심볼. 주문 규칙과 청산 구조를 학습하기 좋습니다.",
  },
  ETHUSDT: {
    symbol: "ETHUSDT",
    displayName: "Ethereum Perpetual",
    lastPrice: 3_280,
    markPrice: 3_276,
    indexPrice: 3_274,
    change24h: 1.72,
    volume24h: 640_000_000,
    fundingRate: 0.00008,
    openInterestLabel: "안정적 유동성",
    description: "BTC보다 완만한 흐름으로 레버리지와 마진 모드 차이를 비교하기 좋습니다.",
  },
};

export const MARKET_SNAPSHOT_LIST = SUPPORTED_MARKET_SYMBOLS.map(
  (symbol) => MARKET_SNAPSHOTS[symbol]
);

export function isSupportedMarketSymbol(
  value: string
): value is MarketSymbol {
  return SUPPORTED_MARKET_SYMBOLS.includes(value as MarketSymbol);
}

export function formatUsd(value: number) {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: value >= 100 ? 0 : 2,
  }).format(value);
}

export function formatPercent(value: number) {
  const formatted = `${value >= 0 ? "+" : ""}${value.toFixed(2)}%`;
  return formatted;
}

