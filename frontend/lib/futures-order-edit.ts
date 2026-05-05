import type { ModifyFuturesOrderResult } from "@/lib/futures-api";

type Notify = (message: string) => void;

type SubmitOrderPriceEditOptions = {
  orderId: string;
  limitPrice: string;
  modifyOrderPrice: (
    orderId: string,
    limitPrice: number
  ) => Promise<ModifyFuturesOrderResult>;
  refresh: () => void;
  closeModal: () => void;
  showSuccess: Notify;
  showError: Notify;
};

export function toEditableLimitPrice(limitPrice: string): number | null {
  const nextLimitPrice = Number(limitPrice);
  if (!Number.isFinite(nextLimitPrice) || nextLimitPrice <= 0) {
    return null;
  }
  return nextLimitPrice;
}

export async function submitOrderPriceEdit({
  orderId,
  limitPrice,
  modifyOrderPrice,
  refresh,
  closeModal,
  showSuccess,
  showError,
}: SubmitOrderPriceEditOptions): Promise<boolean> {
  const nextLimitPrice = toEditableLimitPrice(limitPrice);
  if (nextLimitPrice == null) {
    showError("수정할 주문 가격을 확인해주세요.");
    return false;
  }

  try {
    const result = await modifyOrderPrice(orderId, nextLimitPrice);
    showSuccess(`${result.symbol} 대기 주문 가격을 수정했습니다.`);
    closeModal();
    refresh();
    return true;
  } catch (error) {
    showError(error instanceof Error ? error.message : "주문 수정에 실패했습니다.");
    return false;
  }
}
