import { createFuturesBackendApiUrl } from "@/lib/futures-sse-url";
import { fetchWithFrontendTiming } from "@/lib/frontend-performance-log";

export function fetchFuturesBackendApi(
  path: string,
  init: RequestInit = {}
): Promise<Response> {
  return fetchWithFrontendTiming(createFuturesBackendApiUrl(path), init, {
    method: init.method ?? "GET",
    pathPattern: normalizeFuturesApiPath(path),
  });
}

export function normalizeFuturesApiPath(path: string) {
  const pathWithoutQuery = path.split("?")[0];
  const withLeadingSlash = pathWithoutQuery.startsWith("/")
    ? pathWithoutQuery
    : `/${pathWithoutQuery}`;
  const normalizedPath = withLeadingSlash.startsWith("/api/futures/")
    ? withLeadingSlash.slice("/api/futures".length)
    : withLeadingSlash;
  const apiPath = `/api/futures${normalizedPath}`;

  return normalizeDynamicSegments(apiPath);
}

function normalizeDynamicSegments(pathPattern: string) {
  return pathPattern
    .replace(
      /^\/api\/futures\/markets\/[^/]+$/,
      "/api/futures/markets/{symbol}"
    )
    .replace(
      /^\/api\/futures\/orders\/[^/]+\/cancel$/,
      "/api/futures/orders/{orderId}/cancel"
    )
    .replace(
      /^\/api\/futures\/orders\/[^/]+\/modify$/,
      "/api/futures/orders/{orderId}/modify"
    )
    .replace(
      /^\/api\/futures\/community\/posts\/[^/]+$/,
      "/api/futures/community/posts/{postId}"
    )
    .replace(
      /^\/api\/futures\/community\/posts\/[^/]+\/edit$/,
      "/api/futures/community/posts/{postId}/edit"
    )
    .replace(
      /^\/api\/futures\/community\/posts\/[^/]+\/like$/,
      "/api/futures/community/posts/{postId}/like"
    )
    .replace(
      /^\/api\/futures\/community\/posts\/[^/]+\/comments$/,
      "/api/futures/community/posts/{postId}/comments"
    )
    .replace(
      /^\/api\/futures\/community\/posts\/[^/]+\/comments\/[^/]+$/,
      "/api/futures/community/posts/{postId}/comments/{commentId}"
    )
    .replace(
      /^\/api\/futures\/position-peeks\/[^/]+$/,
      "/api/futures/position-peeks/{peekId}"
    )
    .replace(
      /^\/api\/futures\/shop\/items\/[^/]+\/purchase$/,
      "/api/futures/shop/items/{itemCode}/purchase"
    )
    .replace(
      /^\/api\/futures\/admin\/shop-items\/[^/]+$/,
      "/api/futures/admin/shop-items/{code}"
    )
    .replace(
      /^\/api\/futures\/admin\/shop-items\/[^/]+\/deactivate$/,
      "/api/futures/admin/shop-items/{code}/deactivate"
    )
    .replace(
      /^\/api\/futures\/admin\/reward-redemptions\/[^/]+\/approve$/,
      "/api/futures/admin/reward-redemptions/{requestId}/approve"
    )
    .replace(
      /^\/api\/futures\/admin\/reward-redemptions\/[^/]+\/reject$/,
      "/api/futures/admin/reward-redemptions/{requestId}/reject"
    )
    .replaceAll(/\/[0-9a-fA-F-]{16,}(?=\/|$)/g, "/{id}")
    .replaceAll(/\/\d+(?=\/|$)/g, "/{id}");
}
