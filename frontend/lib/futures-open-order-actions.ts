import type { FuturesOpenOrder } from "./futures-api";

export type EditableFuturesOpenLimitOrder = FuturesOpenOrder & {
  limitPrice: number;
  orderType: "LIMIT";
  status: "PENDING";
};

export function isEditableOpenLimitOrder(
  order: FuturesOpenOrder,
): order is EditableFuturesOpenLimitOrder {
  return order.status === "PENDING"
    && order.orderType === "LIMIT"
    && order.limitPrice !== null
    && order.triggerPrice == null
    && order.triggerType == null
    && order.triggerSource == null
    && order.ocoGroupId == null;
}
