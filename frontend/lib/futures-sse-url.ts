const PUBLIC_FUTURES_API_BASE_URL = (
  process.env.NEXT_PUBLIC_FUTURES_API_BASE_URL ?? ""
).replace(/\/+$/, "");

function buildFuturesSseUrl(path: string, params: URLSearchParams) {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  const query = params.toString();
  const relativeUrl = `/api/futures${normalizedPath}${query ? `?${query}` : ""}`;

  if (!PUBLIC_FUTURES_API_BASE_URL) {
    return relativeUrl;
  }

  return `${PUBLIC_FUTURES_API_BASE_URL}${relativeUrl}`;
}

export function createFuturesBackendApiUrl(path: string) {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  const relativeUrl = `/api/futures${normalizedPath}`;

  if (!PUBLIC_FUTURES_API_BASE_URL) {
    return relativeUrl;
  }

  return `${PUBLIC_FUTURES_API_BASE_URL}${relativeUrl}`;
}

export function createUnifiedMarketSseUrl(
  symbol: string,
  interval: string
) {
  return buildFuturesSseUrl(
    "/markets/stream",
    new URLSearchParams({
      symbol,
      interval,
    })
  );
}

export function createOrderExecutionSseUrl() {
  return buildFuturesSseUrl("/orders/stream", new URLSearchParams());
}

export function createMarketSummarySseUrl(symbols: string[]) {
  return buildFuturesSseUrl(
    "/markets/summary/stream",
    new URLSearchParams({
      symbols: symbols.join(","),
    })
  );
}
