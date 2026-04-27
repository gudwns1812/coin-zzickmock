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
  order: Pick<FuturesOpenOrder, "orderPurpose" | "positionSide">
): string {
  return isGreenOrderLabel(order)
    ? TRADE_LABEL_COLORS.green
    : TRADE_LABEL_COLORS.red;
}

export function getOrderPriceLineTitle(
  order: Pick<FuturesOpenOrder, "orderPurpose" | "positionSide">,
  formattedLimitPrice: string
): string {
  return `${formatOrderPurpose(order.orderPurpose)} ${formatSide(order.positionSide)} ${formattedLimitPrice}`;
}

function isGreenOrderLabel(
  order: Pick<FuturesOpenOrder, "orderPurpose" | "positionSide">
): boolean {
  return (
    (order.orderPurpose === "OPEN_POSITION" && order.positionSide === "LONG") ||
    (order.orderPurpose === "CLOSE_POSITION" && order.positionSide === "SHORT")
  );
}

function formatOrderPurpose(orderPurpose: FuturesOpenOrder["orderPurpose"]): string {
  return orderPurpose === "CLOSE_POSITION" ? "Close" : "Open";
}

function formatSide(positionSide: FuturesPosition["positionSide"]): string {
  return positionSide === "LONG" ? "Long" : "Short";
}
