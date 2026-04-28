import type {
  AdminShopItem,
  AdminShopItemInput,
  RewardRedemption,
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

export async function markRewardRedemptionSent(
  requestId: string,
  memo: string
): Promise<RewardRedemption> {
  return writeFuturesApi<RewardRedemption>(
    `/admin/reward-redemptions/${encodeURIComponent(requestId)}/send`,
    { memo: memo.trim() || null }
  );
}

export async function cancelRewardRedemption(
  requestId: string,
  memo: string
): Promise<RewardRedemption> {
  return writeFuturesApi<RewardRedemption>(
    `/admin/reward-redemptions/${encodeURIComponent(requestId)}/cancel`,
    { memo: memo.trim() || null }
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
