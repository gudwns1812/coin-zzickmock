import type {
  AccountRefillResult,
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
} from "@/lib/futures-api";

type ClientApiResponse<T> = {
  success: boolean;
  data: T | null;
  message: string | null;
};

type ClientApiErrorPayload = {
  message?: string | null;
};


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
      "이미지를 업로드하지 못했습니다. S3 CORS와 업로드 설정을 확인해주세요.",
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

async function readFuturesApi<T>(path: string): Promise<T> {
  const response = await fetch(`/proxy-futures${path}`, {
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
  const response = await fetch(`/proxy-futures${path}`, {
    method,
    credentials: "include",
    headers: body === undefined
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
