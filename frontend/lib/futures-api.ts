import {
  MARKET_SNAPSHOT_LIST,
  MARKET_SNAPSHOTS,
  SUPPORTED_MARKET_SYMBOLS,
  type MarketRankingMemberRank,
  type MarketRankingEntry,
  type MarketSnapshot,
  type MarketSymbol,
  isSupportedMarketSymbol,
} from "@/lib/markets";
import { FUTURES_API_BASE_URL } from "./futures-env";

type ApiResponse<T> = {
  success: boolean;
  data: T | null;
  message: string | null;
};

export type AuthUser = {
  memberId: number;
  account: string;
  memberName: string;
  nickname: string;
  role: "USER" | "ADMIN";
  admin: boolean;
};

export type MarketApiResponse = {
  symbol: string;
  displayName: string;
  lastPrice: number;
  markPrice: number;
  indexPrice: number;
  fundingRate: number;
  nextFundingAt?: string | null;
  fundingIntervalHours?: number | null;
  serverTime?: string | null;
  change24h: number;
  turnover24hUsdt?: number;
  volume24h?: number;
  marketCap?: number;
  dominance?: number;
};

export type FuturesMarketsResult = {
  markets: MarketSnapshot[];
  isFallback: boolean;
};

export type FuturesAccountSummary = {
  memberId: number;
  account: string;
  memberName: string;
  nickname: string;
  usdtBalance: number;
  walletBalance: number;
  available: number;
  totalUnrealizedPnl: number;
  roi: number;
  rewardPoint: number;
};

export type AccountRefillStatus = {
  remainingCount: number;
  refillable: boolean;
  disabledReason: string | null;
  targetWalletBalance: number;
  targetAvailableMargin: number;
  nextResetAt: string;
};

export type AccountRefillResult = {
  walletBalance: number;
  availableMargin: number;
  remainingCount: number;
};

export type FuturesWalletHistoryPoint = {
  snapshotDate: string;
  walletBalance: number;
  dailyWalletChange: number;
  recordedAt: string;
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
  liquidationPriceType: LiquidationPriceType;
  margin: number;
  unrealizedPnl: number;
  realizedPnl?: number;
  roi: number;
  accumulatedClosedQuantity?: number;
  pendingCloseQuantity?: number;
  closeableQuantity?: number;
  takeProfitPrice?: number | null;
  stopLossPrice?: number | null;
};

export type LiquidationPriceType = "EXACT" | "ESTIMATED" | "UNAVAILABLE";

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
  triggerPrice?: number | null;
  triggerType?: "TAKE_PROFIT" | "STOP_LOSS" | null;
  triggerSource?: "MARK_PRICE" | null;
  ocoGroupId?: string | null;
};

export type FuturesOrderHistory = Omit<FuturesOpenOrder, "status"> & {
  status: "PENDING" | "OPEN" | "FILLED" | "CANCELLED" | "REJECTED";
};

export type ModifyFuturesOrderResult = Pick<
  FuturesOpenOrder,
  | "orderId"
  | "symbol"
  | "status"
  | "limitPrice"
  | "feeType"
  | "estimatedFee"
  | "executionPrice"
>;

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
  grossRealizedPnl: number;
  openFee: number;
  closeFee: number;
  totalFee: number;
  fundingCost: number;
  netRealizedPnl: number;
  roi: number;
  closedAt: string;
  closeReason: "MANUAL" | "LIMIT_CLOSE" | "LIQUIDATION" | string;
};

export type FuturesReward = {
  rewardPoint: number;
  tierLabel: string;
};

export type FuturesLeaderboard = {
  mode: "profitRate" | "walletBalance";
  source: "redis" | "database" | "empty";
  lastRefreshedAt: string | null;
  entries: MarketRankingEntry[];
  myRank: MarketRankingMemberRank | null;
};

export type ShopItem = {
  code: string;
  name: string;
  description: string;
  itemType: "COFFEE_VOUCHER" | "ACCOUNT_REFILL_COUNT" | string;
  price: number;
  active: boolean;
  totalStock: number | null;
  soldQuantity: number;
  remainingStock: number | null;
  perMemberPurchaseLimit: number | null;
  remainingPurchaseLimit: number | null;
};

export type AdminShopItem = Omit<ShopItem, "remainingPurchaseLimit"> & {
  itemType: string;
  sortOrder: number;
};

export type AdminShopItemInput = {
  code: string | null;
  name: string;
  description: string;
  itemType: string;
  price: number;
  active: boolean;
  totalStock: number | null;
  perMemberPurchaseLimit: number | null;
  sortOrder: number;
};

export type AdminShopItemsResult = {
  items: AdminShopItem[];
  unavailable: boolean;
  message: string | null;
};

export type RewardPointHistoryType =
  | "GRANT"
  | "REDEMPTION_DEDUCT"
  | "REDEMPTION_REFUND";

export type RewardPointHistory = {
  historyType: RewardPointHistoryType;
  amount: number;
  balanceAfter: number;
  sourceType: string;
  sourceReference: string;
};

// Canonical lifecycle is PENDING -> APPROVED / REJECTED / CANCELLED.
// SENT and CANCELLED_REFUNDED are temporary legacy aliases accepted during the PR rollout.
export type RewardRedemptionStatus =
  | "PENDING"
  | "APPROVED"
  | "REJECTED"
  | "CANCELLED"
  | "SENT"
  | "CANCELLED_REFUNDED";

export type RewardRedemption = {
  requestId: string;
  memberId: number;
  itemCode: string;
  itemName: string;
  pointAmount: number;
  submittedPhoneNumber: string;
  status: RewardRedemptionStatus;
  requestedAt: string;
  sentAt: string | null;
  cancelledAt: string | null;
  adminMemberId: number | null;
  adminMemo: string | null;
};

export type AdminRewardRedemptionsResult = {
  redemptions: RewardRedemption[];
  unavailable: boolean;
  message: string | null;
};

export type RewardRedemptionsResult = {
  redemptions: RewardRedemption[];
  unavailable: boolean;
  message: string | null;
};

export type ShopPurchaseResult = {
  itemCode: string;
  rewardPoint: number;
  refillRemainingCount: number;
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
  estimatedLiquidationPriceType: LiquidationPriceType;
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
  estimatedLiquidationPriceType: LiquidationPriceType;
  executionPrice: number;
};

export type FuturesTradingExecutionEvent = {
  type:
    | "ORDER_FILLED"
    | "POSITION_LIQUIDATED"
    | "POSITION_TAKE_PROFIT"
    | "POSITION_STOP_LOSS";
  orderId: string | null;
  symbol: MarketSymbol;
  positionSide: "LONG" | "SHORT";
  marginMode: "ISOLATED" | "CROSS";
  quantity: number;
  executionPrice: number;
  realizedPnl: number;
  message: string;
};

const SHOP_ITEM_FALLBACKS: ShopItem[] = [
  {
    code: "voucher.coffee",
    name: "커피 교환권",
    price: 50,
    description: "커피 교환권",
    itemType: "COFFEE_VOUCHER",
    active: true,
    totalStock: null,
    soldQuantity: 0,
    remainingStock: null,
    perMemberPurchaseLimit: null,
    remainingPurchaseLimit: null,
  },
];

const ACCOUNT_REFILL_FALLBACK: AccountRefillStatus = {
  remainingCount: 0,
  refillable: false,
  disabledReason: "리필 상태를 불러오지 못했습니다.",
  targetWalletBalance: 100_000,
  targetAvailableMargin: 100_000,
  nextResetAt: new Date().toISOString(),
};

const ACCOUNT_FALLBACK: FuturesAccountSummary = {
  memberId: 0,
  account: "test",
  memberName: "demo-trader",
  nickname: "demo-trader",
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

export async function getAccountRefillStatus(): Promise<AccountRefillStatus> {
  const response = await readApi<AccountRefillStatus>("/api/futures/account/me/refill");
  return response ?? ACCOUNT_REFILL_FALLBACK;
}

export async function getFuturesWalletHistory(): Promise<FuturesWalletHistoryPoint[]> {
  const response = await readApi<FuturesWalletHistoryPoint[]>(
    "/api/futures/account/me/wallet-history"
  );
  return response ?? [];
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

  return filterSupportedOrderHistory(response, symbol);
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

export async function getFuturesLeaderboard(): Promise<FuturesLeaderboard> {
  const response = await readApi<FuturesLeaderboard>(
    "/api/futures/leaderboard?mode=profitRate&limit=4"
  );

  return response ?? {
    mode: "profitRate",
    source: "empty",
    lastRefreshedAt: null,
    entries: [],
    myRank: null,
  };
}

export async function getShopItems(): Promise<ShopItem[]> {
  const response = await readApi<ShopItem[]>("/api/futures/shop/items");
  return response ?? SHOP_ITEM_FALLBACKS;
}

export async function getRewardPointHistory(): Promise<RewardPointHistory[]> {
  const response = await readApi<RewardPointHistory[]>(
    "/api/futures/rewards/history"
  );
  return response ?? [];
}

export async function getRewardRedemptions(): Promise<RewardRedemptionsResult> {
  const response = await readApiResult<RewardRedemption[]>(
    "/api/futures/shop/redemptions"
  );

  return {
    redemptions: response.data ?? [],
    unavailable: !response.ok,
    message: response.message,
  };
}

export async function getAdminRewardRedemptions(
  status: RewardRedemptionStatus = "PENDING"
): Promise<AdminRewardRedemptionsResult> {
  const response = await readApiResult<RewardRedemption[]>(
    `/api/futures/admin/reward-redemptions?status=${encodeURIComponent(status)}`
  );

  return {
    redemptions: response.data ?? [],
    unavailable: !response.ok,
    message: response.message,
  };
}

export async function getAdminShopItems(): Promise<AdminShopItemsResult> {
  const response = await readApiResult<AdminShopItem[]>(
    "/api/futures/admin/shop-items"
  );

  return {
    items: response.data ?? [],
    unavailable: !response.ok,
    message: response.message,
  };
}

export async function getAuthUser(): Promise<AuthUser | null> {
  const response = await readApiResult<AuthUser>("/api/futures/auth/me");
  return response.ok ? response.data : null;
}

function filterSupportedOrderHistory(
  orders: FuturesOrderHistory[],
  symbol?: MarketSymbol
): FuturesOrderHistory[] {
  return orders
    .filter((order) => isSupportedMarketSymbol(order.symbol))
    .filter((order) => !symbol || order.symbol === symbol);
}

async function readApi<T>(path: string): Promise<T | null> {
  const response = await readApiResult<T>(path);
  return response.ok ? response.data : null;
}

async function readApiResult<T>(
  path: string
): Promise<{
  data: T | null;
  ok: boolean;
  status: number | null;
  message: string | null;
}> {
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

    const payload = (await response.json().catch(() => null)) as
      | ApiResponse<T>
      | null;

    if (!response.ok) {
      return {
        data: null,
        ok: false,
        status: response.status,
        message: payload?.message ?? null,
      };
    }

    if (!payload || !payload.success || payload.data === null) {
      return {
        data: null,
        ok: false,
        status: response.status,
        message: payload?.message ?? null,
      };
    }

    return {
      data: payload.data,
      ok: true,
      status: response.status,
      message: payload.message,
    };
  } catch {
    return {
      data: null,
      ok: false,
      status: null,
      message: null,
    };
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
  const turnover24hUsdt =
    typeof apiMarket.turnover24hUsdt === "number"
      ? apiMarket.turnover24hUsdt
      : typeof apiMarket.volume24h === "number"
        ? apiMarket.volume24h
        : fallback.turnover24hUsdt;
  const volume24h =
    typeof apiMarket.volume24h === "number"
      ? apiMarket.volume24h
      : turnover24hUsdt;
  const hasExtendedMetrics =
    typeof apiMarket.turnover24hUsdt === "number" ||
    typeof apiMarket.volume24h === "number";
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
    nextFundingAt: apiMarket.nextFundingAt ?? fallback.nextFundingAt,
    fundingIntervalHours:
      apiMarket.fundingIntervalHours ?? fallback.fundingIntervalHours,
    serverTime: apiMarket.serverTime ?? fallback.serverTime,
    change24h: apiMarket.change24h,
    volume24h,
    turnover24hUsdt,
    marketCap,
    dominance,
    hasExtendedMetrics,
  };
}
