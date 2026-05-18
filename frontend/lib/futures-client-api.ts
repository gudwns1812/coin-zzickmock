import type {
  AccountRefillResult,
  AccountRefillStatus,
  AdminRewardRedemptionsResult,
  AdminShopItemsResult,
  CommunityApiResult,
  CommunityCommentList,
  CommunityPostDetail,
  CommunityPostList,
  FuturesAccountSummary,
  FuturesLeaderboard,
  FuturesOpenOrder,
  FuturesOrderHistory,
  FuturesPosition,
  FuturesPositionHistory,
  FuturesReward,
  FuturesWalletHistoryPoint,
  AdminShopItem,
  AdminShopItemInput,
  LeaderboardMode,
  LeaderboardSearchResult,
  ModifyFuturesOrderResult,
  PeekItemBalance,
  PositionPeekSnapshot,
  PositionPeekStatus,
  RewardRedemption,
  ShopPurchaseResult,
  CommunityCategory,
  RewardPointHistory,
  RewardRedemptionsResult,
  RewardRedemptionStatus,
  RewardShopHistoryResult,
  RewardShopHistoryRow,
  ShopItem,
} from "@/lib/futures-api";
import { isSupportedMarketSymbol, type MarketSymbol } from "@/lib/markets";
import { createFuturesBackendApiUrl } from "@/lib/futures-sse-url";

export type CommunityPostInput = {
  category: CommunityCategory;
  title: string;
  contentJson: unknown;
  imageObjectKeys: string[];
};

export type CommunityPostMutationResult = {
  postId: number;
};

export type CommunityCommentMutationResult = {
  commentId: number;
};

export type CommunityDeleteResult = {
  deleted: boolean;
};

export type CommunityLikeResult = {
  postId: number;
  likedByMe: boolean;
};

export type CommunityImageUploadPresignRequest = {
  fileName: string;
  contentType: string;
  sizeBytes: number;
};

export type CommunityImageUploadPresignResult = {
  uploadUrl: string;
  objectKey: string;
  publicUrl: string;
  contentType: string;
  expiresAt: string;
  maxBytes: number;
};

type ClientApiResponse<T> = {
  success: boolean;
  data: T | null;
  message: string | null;
};

type ClientApiErrorPayload = {
  message?: string | null;
};

export class FuturesClientApiError extends Error {
  constructor(
    message: string,
    public readonly status: number
  ) {
    super(message);
    this.name = "FuturesClientApiError";
  }
}

function readClientApiData<T>(
  response: Response,
  payload: ClientApiResponse<T> | ClientApiErrorPayload | null
): T {
  if (
    !response.ok ||
    !payload ||
    !("success" in payload) ||
    !payload.success ||
    !payload.data
  ) {
    throw new FuturesClientApiError(
      payload?.message ?? "요청을 처리하지 못했습니다.",
      response.status
    );
  }

  return payload.data;
}

export async function getFuturesAccountSummaryClient(): Promise<FuturesAccountSummary> {
  return readFuturesApi<FuturesAccountSummary>("/account/me");
}

export async function getAccountRefillStatusClient(): Promise<AccountRefillStatus> {
  return readFuturesApi<AccountRefillStatus>("/account/me/refill");
}

export async function getFuturesWalletHistoryClient(): Promise<FuturesWalletHistoryPoint[]> {
  return readFuturesApi<FuturesWalletHistoryPoint[]>("/account/me/wallet-history");
}

export async function getFuturesPositionsClient(): Promise<FuturesPosition[]> {
  const positions = await readFuturesApi<FuturesPosition[]>("/positions/me");
  return positions.filter((position) =>
    isSupportedMarketSymbol(position.symbol)
  );
}

export async function getFuturesOpenOrdersClient(
  symbol?: MarketSymbol
): Promise<FuturesOpenOrder[]> {
  const query = symbol ? `?symbol=${encodeURIComponent(symbol)}` : "";
  const orders = await readFuturesApi<FuturesOpenOrder[]>(
    `/orders/open${query}`
  );
  return orders.filter((order) => isSupportedMarketSymbol(order.symbol));
}

export async function getFuturesOrderHistoryClient(
  symbol?: MarketSymbol
): Promise<FuturesOrderHistory[]> {
  const query = symbol ? `?symbol=${encodeURIComponent(symbol)}` : "";
  const orders = await readFuturesApi<FuturesOrderHistory[]>(
    `/orders/history${query}`
  );
  return orders
    .filter((order) => isSupportedMarketSymbol(order.symbol))
    .filter((order) => !symbol || order.symbol === symbol);
}

export async function getFuturesPositionHistoryClient(
  symbol?: MarketSymbol
): Promise<FuturesPositionHistory[]> {
  const query = symbol ? `?symbol=${encodeURIComponent(symbol)}` : "";
  const history = await readFuturesApi<FuturesPositionHistory[]>(
    `/positions/history${query}`
  );
  return history.filter((position) =>
    isSupportedMarketSymbol(position.symbol)
  );
}

export async function getFuturesRewardClient(): Promise<FuturesReward> {
  return readFuturesApi<FuturesReward>("/rewards/me");
}

export async function getFuturesLeaderboardClient(
  options: { mode?: LeaderboardMode; limit?: number } = {}
): Promise<FuturesLeaderboard> {
  const params = new URLSearchParams({
    mode: options.mode ?? "profitRate",
    limit: String(options.limit ?? 4),
  });
  return readFuturesApi<FuturesLeaderboard>(
    `/leaderboard?${params.toString()}`
  );
}

export async function getShopItemsClient(): Promise<ShopItem[]> {
  return readFuturesApi<ShopItem[]>("/shop/items");
}

export async function getRewardPointHistoryClient(): Promise<RewardPointHistory[]> {
  return readFuturesApi<RewardPointHistory[]>("/rewards/history");
}

export async function getRewardRedemptionsClient(): Promise<RewardRedemptionsResult> {
  return readFuturesApiResult<
    RewardRedemption[],
    Pick<RewardRedemptionsResult, "redemptions">
  >("/shop/redemptions", (rows) => ({
    redemptions: rows ?? [],
  }));
}

export async function getRewardShopHistoryClient(): Promise<RewardShopHistoryResult> {
  return readFuturesApiResult<
    RewardShopHistoryRow[],
    Pick<RewardShopHistoryResult, "rows">
  >("/shop/history", (rows) => ({
    rows: rows ?? [],
  }));
}

export async function getAdminRewardRedemptionsClient(
  status: RewardRedemptionStatus = "PENDING"
): Promise<AdminRewardRedemptionsResult> {
  return readFuturesApiResult<
    RewardRedemption[],
    Pick<AdminRewardRedemptionsResult, "redemptions">
  >(
    `/admin/reward-redemptions?status=${encodeURIComponent(status)}`,
    (rows) => ({ redemptions: rows ?? [] })
  );
}

export async function getAdminShopItemsClient(): Promise<AdminShopItemsResult> {
  return readFuturesApiResult<
    AdminShopItem[],
    Pick<AdminShopItemsResult, "items">
  >("/admin/shop-items", (items) => ({
    items: items ?? [],
  }));
}

export async function getCommunityPostsClient(options: {
  category?: Exclude<CommunityCategory, "NOTICE"> | null;
  page?: number;
  size?: number;
} = {}): Promise<CommunityApiResult<CommunityPostList>> {
  const params = new URLSearchParams({
    page: String(options.page ?? 0),
    size: String(options.size ?? 20),
  });
  if (options.category) {
    params.set("category", options.category);
  }
  return readCommunityApi<CommunityPostList>(
    `/community/posts?${params.toString()}`
  );
}

export async function getCommunityPostClient(
  postId: number
): Promise<CommunityApiResult<CommunityPostDetail>> {
  return readCommunityApi<CommunityPostDetail>(
    `/community/posts/${encodeURIComponent(String(postId))}`
  );
}

export async function getCommunityPostForEditClient(
  postId: number
): Promise<CommunityApiResult<CommunityPostDetail>> {
  return readCommunityApi<CommunityPostDetail>(
    `/community/posts/${encodeURIComponent(String(postId))}/edit`
  );
}

export async function getCommunityCommentsClient(
  postId: number,
  options: { page?: number; size?: number } = {}
): Promise<CommunityApiResult<CommunityCommentList>> {
  const params = new URLSearchParams({
    page: String(options.page ?? 0),
    size: String(options.size ?? 20),
  });
  return readCommunityApi<CommunityCommentList>(
    `/community/posts/${encodeURIComponent(String(postId))}/comments?${params.toString()}`
  );
}

export async function searchFuturesLeaderboardMembers(
  query: string,
  options: { mode?: LeaderboardMode; limit?: number } = {}
): Promise<LeaderboardSearchResult[]> {
  const trimmedQuery = query.trim();

  if (trimmedQuery.length === 0) {
    return [];
  }

  const params = new URLSearchParams({
    mode: options.mode ?? "profitRate",
    query: trimmedQuery,
    limit: String(options.limit ?? 10),
  });
  return readFuturesApi<LeaderboardSearchResult[]>(
    `/leaderboard/search?${params.toString()}`
  );
}

export async function getPositionPeekLatest(
  targetToken: string
): Promise<PositionPeekStatus> {
  return writeFuturesApi<PositionPeekStatus>("/position-peeks/latest", {
    targetToken,
  });
}

export async function getPositionPeekSnapshot(
  peekId: string
): Promise<PositionPeekSnapshot> {
  return readFuturesApi<PositionPeekSnapshot>(
    `/position-peeks/${encodeURIComponent(peekId)}`
  );
}

export async function getPositionPeekItemBalance(): Promise<PeekItemBalance> {
  return readFuturesApi<PeekItemBalance>("/position-peeks/item-balance");
}

export async function consumePositionPeek(
  targetToken: string,
  idempotencyKey: string
): Promise<PositionPeekSnapshot> {
  return writeFuturesApi<PositionPeekSnapshot>("/position-peeks", {
    targetToken,
    idempotencyKey,
  });
}

export async function createRewardRedemption(
  itemCode: string,
  phoneNumber: string
): Promise<RewardRedemption> {
  return writeFuturesApi<RewardRedemption>("/shop/redemptions", {
    itemCode,
    phoneNumber,
  });
}

export async function refillFuturesAccount(): Promise<AccountRefillResult> {
  return writeFuturesApi<AccountRefillResult>("/account/me/refill", {});
}

export async function purchaseShopItem(code: string): Promise<ShopPurchaseResult> {
  return writeFuturesApi<ShopPurchaseResult>(
    `/shop/items/${encodeURIComponent(code)}/purchase`,
    {}
  );
}

export async function approveRewardRedemption(
  requestId: string,
  memo: string
): Promise<RewardRedemption> {
  return writeFuturesApi<RewardRedemption>(
    `/admin/reward-redemptions/${encodeURIComponent(requestId)}/approve`,
    { memo: memo.trim() || null }
  );
}

export async function rejectRewardRedemption(
  requestId: string,
  memo: string
): Promise<RewardRedemption> {
  return writeFuturesApi<RewardRedemption>(
    `/admin/reward-redemptions/${encodeURIComponent(requestId)}/reject`,
    { memo: memo.trim() || null }
  );
}

export async function cancelOwnRewardRedemption(
  requestId: string
): Promise<RewardRedemption> {
  return writeFuturesApi<RewardRedemption>(
    `/shop/redemptions/${encodeURIComponent(requestId)}/cancel`,
    {}
  );
}

export async function modifyFuturesOrderPrice(
  orderId: string,
  limitPrice: number
): Promise<ModifyFuturesOrderResult> {
  return writeFuturesApi<ModifyFuturesOrderResult>(
    `/orders/${encodeURIComponent(orderId)}/modify`,
    { limitPrice }
  );
}


export async function createCommunityPost(
  input: CommunityPostInput
): Promise<CommunityPostMutationResult> {
  return requestFuturesApi<CommunityPostMutationResult>(
    "/community/posts",
    "POST",
    input
  );
}

export async function updateCommunityPost(
  postId: number,
  input: CommunityPostInput
): Promise<CommunityPostMutationResult> {
  return requestFuturesApi<CommunityPostMutationResult>(
    `/community/posts/${encodeURIComponent(String(postId))}`,
    "PUT",
    input
  );
}

export async function deleteCommunityPost(
  postId: number
): Promise<CommunityDeleteResult> {
  return requestFuturesApi<CommunityDeleteResult>(
    `/community/posts/${encodeURIComponent(String(postId))}`,
    "DELETE"
  );
}

export async function presignCommunityImageUpload(
  input: CommunityImageUploadPresignRequest
): Promise<CommunityImageUploadPresignResult> {
  return requestFuturesApi<CommunityImageUploadPresignResult>(
    "/community/images/presign",
    "POST",
    input
  );
}

export async function uploadCommunityImageToPresignedUrl(
  file: File,
  presign: CommunityImageUploadPresignResult
): Promise<void> {
  const response = await fetch(presign.uploadUrl, {
    method: "PUT",
    headers: {
      "Content-Type": presign.contentType,
    },
    body: file,
  });

  if (!response.ok) {
    throw new FuturesClientApiError(
      "이미지를 업로드하지 못했습니다. 잠시 후 다시 시도해주세요.",
      response.status
    );
  }
}

export async function createCommunityComment(
  postId: number,
  content: string
): Promise<CommunityCommentMutationResult> {
  return requestFuturesApi<CommunityCommentMutationResult>(
    `/community/posts/${encodeURIComponent(String(postId))}/comments`,
    "POST",
    { content }
  );
}

export async function deleteCommunityComment(
  postId: number,
  commentId: number
): Promise<CommunityDeleteResult> {
  return requestFuturesApi<CommunityDeleteResult>(
    `/community/posts/${encodeURIComponent(String(postId))}/comments/${encodeURIComponent(String(commentId))}`,
    "DELETE"
  );
}

export async function likeCommunityPost(
  postId: number
): Promise<CommunityLikeResult> {
  return requestFuturesApi<CommunityLikeResult>(
    `/community/posts/${encodeURIComponent(String(postId))}/like`,
    "POST"
  );
}

export async function unlikeCommunityPost(
  postId: number
): Promise<CommunityLikeResult> {
  return requestFuturesApi<CommunityLikeResult>(
    `/community/posts/${encodeURIComponent(String(postId))}/like`,
    "DELETE"
  );
}

export async function createAdminShopItem(
  input: AdminShopItemInput
): Promise<AdminShopItem> {
  return writeFuturesApi<AdminShopItem>("/admin/shop-items", input);
}

export async function updateAdminShopItem(
  code: string,
  input: AdminShopItemInput
): Promise<AdminShopItem> {
  return writeFuturesApi<AdminShopItem>(
    `/admin/shop-items/${encodeURIComponent(code)}`,
    input
  );
}

export async function deactivateAdminShopItem(
  code: string
): Promise<AdminShopItem> {
  return writeFuturesApi<AdminShopItem>(
    `/admin/shop-items/${encodeURIComponent(code)}/deactivate`,
    {}
  );
}


async function readFuturesApiResult<T, R extends object>(
  path: string,
  toResult: (data: T | null) => R
): Promise<R & { unavailable: boolean; message: string | null }> {
  const response = await fetch(createFuturesBackendApiUrl(path), {
    method: "GET",
    credentials: "include",
  });

  const payload = (await response.json().catch(() => null)) as
    | ClientApiResponse<T>
    | ClientApiErrorPayload
    | null;
  const ok = Boolean(
    response.ok && payload && "success" in payload && payload.success
  );

  return {
    ...toResult(ok && payload && "data" in payload ? payload.data : null),
    unavailable: !ok,
    message: payload?.message ?? null,
  };
}

async function readCommunityApi<T>(path: string): Promise<CommunityApiResult<T>> {
  const response = await fetch(createFuturesBackendApiUrl(path), {
    method: "GET",
    credentials: "include",
  });

  const payload = (await response.json().catch(() => null)) as
    | ClientApiResponse<T>
    | ClientApiErrorPayload
    | null;

  if (response.ok && payload && "success" in payload && payload.success) {
    return {
      data: payload.data,
      unavailable: payload.data === null,
      status: response.status,
      message: payload.message,
    };
  }

  return {
    data: null,
    unavailable: true,
    status: response.status,
    message: payload?.message ?? null,
  };
}

async function readFuturesApi<T>(path: string): Promise<T> {
  const response = await fetch(createFuturesBackendApiUrl(path), {
    method: "GET",
    credentials: "include",
  });

  const payload = (await response.json().catch(() => null)) as
    | ClientApiResponse<T>
    | ClientApiErrorPayload
    | null;

  return readClientApiData(response, payload);
}

async function writeFuturesApi<T>(
  path: string,
  body: unknown
): Promise<T> {
  return requestFuturesApi(path, "POST", body);
}

async function requestFuturesApi<T>(
  path: string,
  method: "POST" | "PUT" | "DELETE",
  body?: unknown
): Promise<T> {
  const response = await fetch(createFuturesBackendApiUrl(path), {
    method,
    credentials: "include",
    headers:
      body === undefined
        ? undefined
        : {
            "Content-Type": "application/json",
          },
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  const payload = (await response.json().catch(() => null)) as
    | ClientApiResponse<T>
    | ClientApiErrorPayload
    | null;

  return readClientApiData(response, payload);
}
