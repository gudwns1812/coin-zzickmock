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
} from "@/lib/futures-api";

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
): Promise<PositionPeekStatus> {
  return writeFuturesApi<PositionPeekStatus>("/position-peeks", {
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
  const response = await fetch(`/proxy-futures${path}`, {
    method: "POST",
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });

  const payload = (await response.json().catch(() => null)) as
    | ClientApiResponse<T>
    | ClientApiErrorPayload
    | null;

  return readClientApiData(response, payload);
}
