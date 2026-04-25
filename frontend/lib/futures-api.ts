import {
  MARKET_SNAPSHOT_LIST,
  MARKET_SNAPSHOTS,
  SUPPORTED_MARKET_SYMBOLS,
  type MarketSnapshot,
  type MarketSymbol,
  isSupportedMarketSymbol,
} from "@/lib/markets";

type ApiResponse<T> = {
  success: boolean;
  data: T | null;
  message: string | null;
};

export type MarketApiResponse = {
  symbol: string;
  displayName: string;
  lastPrice: number;
  markPrice: number;
  indexPrice: number;
  fundingRate: number;
  change24h: number;
  volume24h?: number;
  marketCap?: number;
  dominance?: number;
};

export type FuturesMarketsResult = {
  markets: MarketSnapshot[];
  isFallback: boolean;
};

export type FuturesAccountSummary = {
  memberId: string;
  memberName: string;
  usdtBalance: number;
  walletBalance: number;
  available: number;
  totalUnrealizedPnl: number;
  roi: number;
  rewardPoint: number;
};

export type FuturesPosition = {
  symbol: MarketSymbol;
  positionSide: "LONG" | "SHORT";
  marginMode: "ISOLATED" | "CROSS";
  leverage: number;
  quantity: number;
  entryPrice: number;
  markPrice: number;
  liquidationPrice: number | null;
  margin: number;
  unrealizedPnl: number;
  roi: number;
};

export type FuturesOpenOrder = {
  orderId: string;
  symbol: MarketSymbol;
  positionSide: "LONG" | "SHORT";
  orderType: "MARKET" | "LIMIT";
  orderPurpose: "OPEN_POSITION" | "CLOSE_POSITION";
  marginMode: "ISOLATED" | "CROSS";
  leverage: number;
  quantity: number;
  limitPrice: number | null;
  status: "PENDING" | "OPEN" | "FILLED" | "CANCELLED";
  feeType: "MAKER" | "TAKER";
  estimatedFee: number;
  executionPrice: number;
  orderTime: string;
};

export type FuturesOrderHistory = Omit<FuturesOpenOrder, "status"> & {
  status: "FILLED" | "CANCELLED" | "REJECTED";
};

export type FuturesPositionHistory = {
  symbol: MarketSymbol;
  positionSide: "LONG" | "SHORT";
  marginMode: "ISOLATED" | "CROSS";
  leverage: number;
  openedAt: string;
  averageEntryPrice: number;
  averageExitPrice: number;
  positionSize: number;
  realizedPnl: number;
  roi: number;
  closedAt: string;
  closeReason: "MANUAL" | "LIMIT_CLOSE" | "LIQUIDATION" | string;
};

export type FuturesReward = {
  rewardPoint: number;
  tierLabel: string;
};

export type ShopItem = {
  code: string;
  name: string;
  price: number;
  description: string;
};

export type OrderPreviewRequest = {
  symbol: MarketSymbol;
  positionSide: "LONG" | "SHORT";
  orderType: "MARKET" | "LIMIT";
  marginMode: "ISOLATED" | "CROSS";
  leverage: number;
  quantity: number;
  limitPrice: number | null;
};

export type OrderPreviewResponse = {
  feeType: "MAKER" | "TAKER";
  estimatedFee: number;
  estimatedInitialMargin: number;
  estimatedLiquidationPrice: number | null;
  estimatedEntryPrice: number;
  executable: boolean;
};

export type OrderExecutionResponse = {
  orderId: string;
  status: "FILLED" | "OPEN";
  symbol: MarketSymbol;
  feeType: "MAKER" | "TAKER";
  estimatedFee: number;
  estimatedInitialMargin: number;
  estimatedLiquidationPrice: number | null;
  executionPrice: number;
};

export type FuturesTradingExecutionEvent = {
  type: "ORDER_FILLED" | "POSITION_LIQUIDATED";
  orderId: string | null;
  symbol: MarketSymbol;
  positionSide: "LONG" | "SHORT";
  marginMode: "ISOLATED" | "CROSS";
  quantity: number;
  executionPrice: number;
  realizedPnl: number;
  message: string;
};

const FUTURES_API_BASE_URL =
  process.env.FUTURES_API_BASE_URL ?? "http://127.0.0.1:8080";

const SHOP_ITEM_FALLBACKS: ShopItem[] = [
  {
    code: "badge.basic",
    name: "프로필 배지",
    price: 10,
    description: "닉네임 옆에 붙는 기본 배지",
  },
  {
    code: "theme.cyan",
    name: "대시보드 테마",
    price: 30,
    description: "마켓 화면 강조 색상 테마",
  },
  {
    code: "title.shark",
    name: "칭호",
    price: 50,
    description: "프로필과 헤더에 표시되는 칭호",
  },
];

const ACCOUNT_FALLBACK: FuturesAccountSummary = {
  memberId: "test",
  memberName: "demo-trader",
  usdtBalance: 100_000,
  walletBalance: 100_000,
  available: 100_000,
  totalUnrealizedPnl: 0,
  roi: 0,
  rewardPoint: 0,
};

const REWARD_FALLBACK: FuturesReward = {
  rewardPoint: 0,
  tierLabel: "POINT_WALLET",
};

export function isSupportedFuturesSymbol(
  value: string
): value is MarketSymbol {
  return isSupportedMarketSymbol(value);
}

export async function getFuturesMarkets(): Promise<MarketSnapshot[]> {
  const result = await getFuturesMarketsResult();
  return result.markets;
}

export async function getFuturesMarketsResult(): Promise<FuturesMarketsResult> {
  const response = await readApi<MarketApiResponse[]>("/api/futures/markets");

  if (!response) {
    return {
      markets: MARKET_SNAPSHOT_LIST,
      isFallback: true,
    };
  }

  const supportedMarkets = response
    .flatMap((market) => {
      if (!isSupportedMarketSymbol(market.symbol)) {
        return [];
      }

      return [mergeMarketSnapshot(market.symbol, market)];
    })
    .sort(
      (left, right) =>
        MARKET_SNAPSHOT_LIST.findIndex((market) => market.symbol === left.symbol) -
        MARKET_SNAPSHOT_LIST.findIndex((market) => market.symbol === right.symbol)
    );

  if (supportedMarkets.length !== SUPPORTED_MARKET_SYMBOLS.length) {
    return {
      markets: MARKET_SNAPSHOT_LIST,
      isFallback: true,
    };
  }

  return {
    markets: supportedMarkets,
    isFallback: false,
  };
}

export async function getFuturesMarket(
  symbol: MarketSymbol
): Promise<MarketSnapshot> {
  const response = await readApi<MarketApiResponse>(`/api/futures/markets/${symbol}`);

  if (!response || !isSupportedMarketSymbol(response.symbol)) {
    return MARKET_SNAPSHOTS[symbol];
  }

  return mergeMarketSnapshot(response.symbol, response);
}

export async function getFuturesAccountSummary(): Promise<FuturesAccountSummary> {
  const response = await readApi<FuturesAccountSummary>("/api/futures/account/me");
  return response ?? ACCOUNT_FALLBACK;
}

export async function getFuturesPositions(): Promise<FuturesPosition[]> {
  const response = await readApi<FuturesPosition[]>("/api/futures/positions/me");

  if (!response) {
    return [];
  }

  return response.filter((position) => isSupportedMarketSymbol(position.symbol));
}

export async function getFuturesOpenOrders(
  symbol?: MarketSymbol
): Promise<FuturesOpenOrder[]> {
  const query = symbol ? `?symbol=${encodeURIComponent(symbol)}` : "";
  const response = await readApi<FuturesOpenOrder[]>(
    `/api/futures/orders/open${query}`
  );

  if (!response) {
    return [];
  }

  return response.filter((order) => isSupportedMarketSymbol(order.symbol));
}

export async function getFuturesOrderHistory(
  symbol?: MarketSymbol
): Promise<FuturesOrderHistory[]> {
  const query = symbol ? `?symbol=${encodeURIComponent(symbol)}` : "";
  const response = await readApi<FuturesOrderHistory[]>(
    `/api/futures/orders/history${query}`
  );

  if (!response) {
    return [];
  }

  return response.filter((order) => isSupportedMarketSymbol(order.symbol));
}

export async function getFuturesPositionHistory(
  symbol?: MarketSymbol
): Promise<FuturesPositionHistory[]> {
  const query = symbol ? `?symbol=${encodeURIComponent(symbol)}` : "";
  const response = await readApi<FuturesPositionHistory[]>(
    `/api/futures/positions/history${query}`
  );

  if (!response) {
    return [];
  }

  return response.filter((history) => isSupportedMarketSymbol(history.symbol));
}

export async function getFuturesReward(): Promise<FuturesReward> {
  const response = await readApi<FuturesReward>("/api/futures/rewards/me");
  return response ?? REWARD_FALLBACK;
}

export async function getShopItems(): Promise<ShopItem[]> {
  const response = await readApi<ShopItem[]>("/api/futures/shop/items");
  return response ?? SHOP_ITEM_FALLBACKS;
}

async function readApi<T>(path: string): Promise<T | null> {
  try {
    const cookieHeader = await getAccessTokenCookieHeader();
    const response = await fetch(`${FUTURES_API_BASE_URL}${path}`, {
      cache: "no-store",
      headers: cookieHeader
        ? {
            Cookie: cookieHeader,
          }
        : undefined,
      signal: AbortSignal.timeout(2000),
    });

    if (!response.ok) {
      return null;
    }

    const payload = (await response.json()) as ApiResponse<T>;

    if (!payload.success || payload.data === null) {
      return null;
    }

    return payload.data;
  } catch {
    return null;
  }
}

async function getAccessTokenCookieHeader(): Promise<string | null> {
  if (typeof window !== "undefined") {
    return null;
  }

  try {
    const { cookies } = await import("next/headers");
    const cookieStore = await cookies();
    const accessToken = cookieStore.get("accessToken")?.value;
    return accessToken ? `accessToken=${accessToken}` : null;
  } catch {
    return null;
  }
}

function mergeMarketSnapshot(
  symbol: MarketSymbol,
  apiMarket: MarketApiResponse
): MarketSnapshot {
  const fallback = MARKET_SNAPSHOTS[symbol];
  const hasExtendedMetrics =
    typeof apiMarket.volume24h === "number" &&
    typeof apiMarket.marketCap === "number" &&
    typeof apiMarket.dominance === "number";
  const volume24h =
    typeof apiMarket.volume24h === "number"
      ? apiMarket.volume24h
      : fallback.volume24h;
  const marketCap =
    typeof apiMarket.marketCap === "number"
      ? apiMarket.marketCap
      : fallback.marketCap;
  const dominance =
    typeof apiMarket.dominance === "number"
      ? apiMarket.dominance
      : fallback.dominance;

  return {
    ...fallback,
    displayName: apiMarket.displayName,
    lastPrice: apiMarket.lastPrice,
    markPrice: apiMarket.markPrice,
    indexPrice: apiMarket.indexPrice,
    fundingRate: apiMarket.fundingRate,
    change24h: apiMarket.change24h,
    volume24h,
    marketCap,
    dominance,
    hasExtendedMetrics,
  };
}
