import type { FuturesOpenOrder, FuturesPosition } from "@/lib/futures-api";

export const TRADE_LABEL_COLORS = {
  green: "#10b981",
  red: "#ef4444",
} as const;

export function getPositionPriceLineTitle(
  position: Pick<FuturesPosition, "entryPrice" | "positionSide">,
  formattedEntryPrice: string
): string {
  return `${formatSide(position.positionSide)} ${formattedEntryPrice}`;
}

export function getOrderPriceLineColor(
  order: Pick<FuturesOpenOrder, "orderPurpose" | "positionSide" | "triggerType">
): string {
  return isGreenOrderLabel(order)
    ? TRADE_LABEL_COLORS.green
    : TRADE_LABEL_COLORS.red;
}

export function getOrderPriceLineTitle(
  order: Pick<FuturesOpenOrder, "orderPurpose" | "positionSide" | "triggerType">,
  formattedLimitPrice: string
): string {
  return `${formatOrderPurpose(order)} ${formatSide(order.positionSide)} ${formattedLimitPrice}`;
}

function isGreenOrderLabel(
  order: Pick<FuturesOpenOrder, "orderPurpose" | "positionSide" | "triggerType">
): boolean {
  return (
    (order.orderPurpose === "OPEN_POSITION" && order.positionSide === "LONG") ||
    (order.orderPurpose === "CLOSE_POSITION" && order.positionSide === "SHORT")
  );
}

function formatOrderPurpose(
  order: Pick<FuturesOpenOrder, "orderPurpose" | "triggerType">
): string {
  if (order.triggerType === "TAKE_PROFIT") {
    return "TP Close";
  }

  if (order.triggerType === "STOP_LOSS") {
    return "SL Close";
  }

  return order.orderPurpose === "CLOSE_POSITION" ? "Close" : "Open";
}

function formatSide(positionSide: FuturesPosition["positionSide"]): string {
  return positionSide === "LONG" ? "Long" : "Short";
}
