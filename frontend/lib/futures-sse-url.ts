const PUBLIC_FUTURES_API_BASE_URL = (
  process.env.NEXT_PUBLIC_FUTURES_API_BASE_URL ?? ""
)
  .trim()
  .replace(/\/+$/, "");

function isLoopbackHost(hostname: string) {
  return hostname === "localhost" || hostname === "127.0.0.1" || hostname === "::1";
}

function getPublicFuturesApiBaseUrl() {
  if (!PUBLIC_FUTURES_API_BASE_URL || typeof window === "undefined") {
    return PUBLIC_FUTURES_API_BASE_URL;
  }

  try {
    const apiUrl = new URL(PUBLIC_FUTURES_API_BASE_URL);
    const pageHostname = window.location.hostname;

    if (
      isLoopbackHost(apiUrl.hostname) &&
      isLoopbackHost(pageHostname) &&
      apiUrl.hostname !== pageHostname
    ) {
      apiUrl.hostname = pageHostname;
      return apiUrl.toString().replace(/\/+$/, "");
    }
  } catch {
    return PUBLIC_FUTURES_API_BASE_URL;
  }

  return PUBLIC_FUTURES_API_BASE_URL;
}

function buildFuturesSseUrl(path: string, params: URLSearchParams) {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  const query = params.toString();
  const relativeUrl = `/api/futures${normalizedPath}${query ? `?${query}` : ""}`;
  const publicBaseUrl = getPublicFuturesApiBaseUrl();

  if (!publicBaseUrl) {
    return relativeUrl;
  }

  return `${publicBaseUrl}${relativeUrl}`;
}

export function createFuturesBackendApiUrl(path: string) {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  const relativeUrl = `/api/futures${normalizedPath}`;
  const publicBaseUrl = getPublicFuturesApiBaseUrl();

  if (!publicBaseUrl) {
    return relativeUrl;
  }

  return `${publicBaseUrl}${relativeUrl}`;
}

export function createFuturesBackendUrl(path: string) {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  const publicBaseUrl = getPublicFuturesApiBaseUrl();

  if (!publicBaseUrl) {
    return normalizedPath;
  }

  return `${publicBaseUrl}${normalizedPath}`;
}

export function createUnifiedMarketSseUrl(
  symbol: string,
  interval: string
) {
  return buildFuturesSseUrl(
    "/stream/markets",
    new URLSearchParams({
      symbol,
      interval,
    })
  );
}

export function createOrderExecutionSseUrl() {
  return buildFuturesSseUrl("/stream/orders", new URLSearchParams());
}

export function createMarketSummarySseUrl(symbols: string[]) {
  return buildFuturesSseUrl(
    "/stream/markets/summary",
    new URLSearchParams({
      symbols: symbols.join(","),
    })
  );
}
