import type { ShopItem } from "@/lib/futures-api";

const COFFEE_SHOP_ITEM_IMAGE_PATH = "/images/IceAmericano.png";

export function normalizeVoucherPhoneNumber(value: string): string {
  return value.trim().replaceAll("-", "");
}

export function validateVoucherPhoneNumber(value: string): string | null {
  const trimmed = value.trim();

  if (!trimmed) {
    return "휴대폰 번호를 입력해주세요.";
  }

  if (!/^[0-9-]+$/.test(trimmed)) {
    return "숫자와 하이픈만 입력할 수 있습니다.";
  }

  const normalized = normalizeVoucherPhoneNumber(trimmed);

  if (!/^[0-9]{10,11}$/.test(normalized)) {
    return "휴대폰 번호는 숫자 10~11자리여야 합니다.";
  }

  return null;
}

export function isShopItemSoldOut(item: ShopItem): boolean {
  return item.remainingStock !== null && item.remainingStock <= 0;
}

export function isShopItemLimitReached(item: ShopItem): boolean {
  return (
    item.remainingPurchaseLimit !== null && item.remainingPurchaseLimit <= 0
  );
}

export function canRedeemShopItem(item: ShopItem, rewardPoint: number): boolean {
  return (
    item.active &&
    !isShopItemSoldOut(item) &&
    !isShopItemLimitReached(item) &&
    rewardPoint >= item.price
  );
}

export function getShopItemAvailabilityLabel(item: ShopItem): string {
  if (!item.active) {
    return "판매 중지";
  }

  if (isShopItemSoldOut(item)) {
    return "품절";
  }

  if (isShopItemLimitReached(item)) {
    return "구매 제한 도달";
  }

  if (item.remainingStock !== null) {
    return `잔여 ${item.remainingStock.toLocaleString("ko-KR")}개`;
  }

  if (item.remainingPurchaseLimit !== null) {
    return `구매 가능 ${item.remainingPurchaseLimit.toLocaleString("ko-KR")}회`;
  }

  return "구매 가능";
}

export function getShopItemImagePath(_item: Pick<ShopItem, "code" | "name">) {
  return COFFEE_SHOP_ITEM_IMAGE_PATH;
}
