export const OPEN_MARKET_TAKER_FEE_RATE = 0.0005;
export const DEFAULT_QUANTITY_PRECISION = 3;

export function calculateMaxOpenMarketQuantity(
  availableBalance: number,
  leverage: number,
  price: number,
  feeRate = OPEN_MARKET_TAKER_FEE_RATE
): number {
  if (
    !Number.isFinite(availableBalance) ||
    !Number.isFinite(leverage) ||
    !Number.isFinite(price) ||
    !Number.isFinite(feeRate) ||
    availableBalance <= 0 ||
    leverage <= 0 ||
    price <= 0 ||
    feeRate < 0
  ) {
    return 0;
  }

  return availableBalance / (price * (1 / leverage + feeRate));
}

export function floorQuantityToPrecision(
  quantity: number,
  precision = DEFAULT_QUANTITY_PRECISION
): number {
  if (!Number.isFinite(quantity) || quantity <= 0) {
    return 0;
  }

  const factor = 10 ** precision;
  return Math.floor(quantity * factor) / factor;
}

export function formatFlooredQuantity(
  quantity: number,
  precision = DEFAULT_QUANTITY_PRECISION
): string {
  const floored = floorQuantityToPrecision(quantity, precision);
  if (floored <= 0) {
    return "0";
  }

  return floored.toFixed(precision);
}
