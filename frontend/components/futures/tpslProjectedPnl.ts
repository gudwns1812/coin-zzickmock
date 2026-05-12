import type { FuturesPosition } from "@/lib/futures-api";

type TpslProjectedPnlPosition = Pick<
  FuturesPosition,
  "entryPrice" | "positionSide" | "quantity"
>;

export function calculateTpslProjectedPnl(
  value: string,
  position: TpslProjectedPnlPosition
): number | null {
  const parsedPrice = parseOptionalPositiveNumber(value);

  if (typeof parsedPrice !== "number") {
    return null;
  }

  if (
    ![position.entryPrice, parsedPrice, position.quantity].every(Number.isFinite)
  ) {
    return 0;
  }

  return position.positionSide === "LONG"
    ? (parsedPrice - position.entryPrice) * position.quantity
    : (position.entryPrice - parsedPrice) * position.quantity;
}

function parseOptionalPositiveNumber(value: string): number | null | "invalid" {
  const trimmed = value.trim();
  if (trimmed.length === 0) {
    return null;
  }

  const parsed = Number(trimmed);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    return "invalid";
  }
  return parsed;
}
