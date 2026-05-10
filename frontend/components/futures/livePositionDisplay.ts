import type { FuturesPosition } from "@/lib/futures-api";
import type { MarketSnapshot } from "@/lib/markets";

export function deriveLivePositionDisplay(
  position: FuturesPosition,
  market: Pick<MarketSnapshot, "symbol" | "markPrice">
): FuturesPosition {
  if (position.symbol !== market.symbol) {
    return position;
  }

  const unrealizedPnl = calculateUnrealizedPnl(
    position.positionSide,
    position.entryPrice,
    market.markPrice,
    position.quantity
  );
  const roi = calculateRoe(unrealizedPnl, position.margin);

  return {
    ...position,
    markPrice: market.markPrice,
    unrealizedPnl,
    roi,
  };
}

export function deriveLivePositionsDisplay(
  positions: FuturesPosition[],
  market: Pick<MarketSnapshot, "symbol" | "markPrice">
): FuturesPosition[] {
  return positions.map((position) => deriveLivePositionDisplay(position, market));
}

export function deriveLivePositionDisplayFromSnapshots(
  position: FuturesPosition,
  marketsBySymbol: ReadonlyMap<string, Pick<MarketSnapshot, "symbol" | "markPrice">>
): FuturesPosition {
  const market = marketsBySymbol.get(position.symbol);

  if (!market) {
    return position;
  }

  return deriveLivePositionDisplay(position, market);
}

export function calculateUnrealizedPnl(
  positionSide: FuturesPosition["positionSide"],
  entryPrice: number,
  markPrice: number,
  quantity: number
): number {
  if (![entryPrice, markPrice, quantity].every(Number.isFinite)) {
    return 0;
  }

  return positionSide === "LONG"
    ? (markPrice - entryPrice) * quantity
    : (entryPrice - markPrice) * quantity;
}

export function calculateRoe(unrealizedPnl: number, margin: number): number {
  if (!Number.isFinite(unrealizedPnl) || !Number.isFinite(margin) || margin <= 0) {
    return 0;
  }

  return unrealizedPnl / margin;
}

export function getAccumulatedClosedQuantity(position: FuturesPosition): number {
  return position.accumulatedClosedQuantity ?? 0;
}
