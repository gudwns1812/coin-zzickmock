import type {
  AccountRefillResult,
  AdminShopItem,
  AdminShopItemInput,
  ModifyFuturesOrderResult,
  RewardRedemption,
  ShopPurchaseResult,
} from "@/lib/futures-api";

type ClientApiResponse<T> = {
  success: boolean;
  data: T | null;
  message: string | null;
};

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
    | null;

  if (!response.ok || !payload?.success || !payload.data) {
    throw new Error(payload?.message ?? "요청을 처리하지 못했습니다.");
  }

  return payload.data;
}
