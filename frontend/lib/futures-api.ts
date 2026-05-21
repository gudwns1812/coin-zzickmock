import {
  MARKET_SNAPSHOTS,
  SUPPORTED_MARKET_SYMBOLS,
  type MarketRankingMemberRank,
  type MarketRankingEntry,
  type MarketSnapshot,
  type MarketSymbol,
  isSupportedMarketSymbol,
} from "@/lib/markets";
import { FUTURES_API_BASE_URL } from "./futures-env";
import { normalizeFuturesApiPath } from "./futures-api-request";
import { fetchWithFrontendTiming } from "./frontend-performance-log";

const SUPPORTED_MARKET_SNAPSHOT_TUPLE: [MarketSnapshot, MarketSnapshot] = [
  MARKET_SNAPSHOTS.BTCUSDT,
  MARKET_SNAPSHOTS.ETHUSDT,
];

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
  markets: [MarketSnapshot, MarketSnapshot];
  isFallback: boolean;
};

export type FuturesMarketResult = {
  market: MarketSnapshot;
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

export type LeaderboardMode = FuturesLeaderboard["mode"];

export type LeaderboardSearchResult = MarketRankingEntry & {
  targetToken: string;
};

export type PositionPeekTarget = {
  rank: number | null;
  nickname: string;
  walletBalance: number | null;
  profitRate: number | null;
  targetToken: string;
};

export type PositionPeekPublicPosition = {
  symbol: MarketSymbol | string;
  positionSide: "LONG" | "SHORT" | string;
  leverage: number;
  positionSize: number;
  entryPrice: number | null;
  notionalValue: number;
  unrealizedPnl: number;
  roi: number;
};

export type PositionPeekSnapshot = {
  peekId: string;
  target: Omit<PositionPeekTarget, "targetToken">;
  createdAt?: string;
  snapshotCreatedAt?: string;
  positions: PositionPeekPublicPosition[];
  remainingPeekItemCount?: number;
};

export type PositionPeekStatus = {
  target: PositionPeekTarget;
  latestSnapshot: PositionPeekSnapshot | null;
  peekItemCount?: number;
  remainingPeekItemCount?: number;
  itemCount?: number;
};

export type PeekItemBalance = {
  peekItemCount?: number;
  remainingPeekItemCount?: number;
  itemCount?: number;
};

export type ShopItem = {
  code: string;
  name: string;
  description: string;
  itemType: "COFFEE_VOUCHER" | "ACCOUNT_REFILL_COUNT" | "POSITION_PEEK" | string;
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

export type RewardShopHistoryKind =
  | "INSTANT_PURCHASE"
  | "REDEMPTION_REQUEST";

export type RewardShopHistoryRow = {
  kind: RewardShopHistoryKind;
  entryId: string;
  itemCode: string;
  itemName: string;
  itemType: string;
  pointAmount: number;
  quantity: number;
  eventAt: string;
  submittedPhoneNumber: string | null;
  status: RewardRedemptionStatus | null;
  purchasedAt: string | null;
  requestedAt: string | null;
  sentAt: string | null;
  cancelledAt: string | null;
};

export type RewardShopHistoryResult = {
  rows: RewardShopHistoryRow[];
  unavailable: boolean;
  message: string | null;
};

export type ShopPurchaseResult = {
  itemCode: string;
  rewardPoint: number;
  refillRemainingCount: number | null;
  positionPeekItemBalance: number | null;
};

export type CommunityCategory = "NOTICE" | "CHART_ANALYSIS" | "COIN_INFORMATION" | "CHAT";

export type CommunityPostSummary = {
  id: number;
  category: CommunityCategory;
  title: string;
  authorNickname: string;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  createdAt: string;
};

export type CommunityPage = {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
};

export type CommunityPostList = {
  pinnedNotices: CommunityPostSummary[];
  posts: CommunityPostSummary[];
  page: CommunityPage;
};

export type CommunityPostDetail = CommunityPostSummary & {
  contentJson: string;
  canEdit: boolean;
  canDelete: boolean;
  likedByMe: boolean;
  updatedAt: string;
};

export type CommunityComment = {
  id: number;
  postId: number;
  authorNickname: string;
  content: string;
  canDelete: boolean;
  createdAt: string;
};

export type CommunityCommentList = {
  comments: CommunityComment[];
  page: CommunityPage;
};

export type CommunityApiResult<T> = {
  data: T | null;
  unavailable: boolean;
  status: number | null;
  message: string | null;
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
      markets: SUPPORTED_MARKET_SNAPSHOT_TUPLE,
      isFallback: true,
    };
  }

  const supportedMarketMap = new Map<MarketSymbol, MarketSnapshot>();
  for (const market of response) {
    if (isSupportedMarketSymbol(market.symbol)) {
      supportedMarketMap.set(
        market.symbol,
        mergeMarketSnapshot(market.symbol, market)
      );
    }
  }

  const markets = SUPPORTED_MARKET_SNAPSHOT_TUPLE.map(
    (market) => supportedMarketMap.get(market.symbol) ?? market
  ) as [MarketSnapshot, MarketSnapshot];

  if (supportedMarketMap.size !== SUPPORTED_MARKET_SYMBOLS.length) {
    return {
      markets,
      isFallback: true,
    };
  }

  return {
    markets,
    isFallback: false,
  };
}

export async function getFuturesMarket(
  symbol: MarketSymbol
): Promise<MarketSnapshot> {
  const result = await getFuturesMarketResult(symbol);
  return result.market;
}

export async function getFuturesMarketResult(
  symbol: MarketSymbol
): Promise<FuturesMarketResult> {
  const response = await readApi<MarketApiResponse>(`/api/futures/markets/${symbol}`);

  if (!response || !isSupportedMarketSymbol(response.symbol)) {
    return {
      market: MARKET_SNAPSHOTS[symbol],
      isFallback: true,
    };
  }

  if (response.symbol !== symbol) {
    return {
      market: MARKET_SNAPSHOTS[symbol],
      isFallback: true,
    };
  }

  return {
    market: mergeMarketSnapshot(response.symbol, response),
    isFallback: false,
  };
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
    const response = await fetchWithFrontendTiming(
      `${FUTURES_API_BASE_URL}${path}`,
      {
        cache: "no-store",
        signal: AbortSignal.timeout(2000),
      },
      {
        pathPattern: normalizeFuturesApiPath(path),
      }
    );

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
