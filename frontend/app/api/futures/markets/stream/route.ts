import { FUTURES_API_BASE_URL } from "@/lib/futures-env";
import { isSupportedMarketSymbol } from "@/lib/markets";
import { readRequiredSseClientKey, SSE_CLIENT_KEY_PARAM } from "@/lib/sse-client-key";
import { createSseUpstreamHeaders, proxySseStream } from "@/lib/sse-proxy";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

export async function GET(request: Request) {
  const url = new URL(request.url);
  const symbol = url.searchParams.get("symbol");
  const interval = url.searchParams.get("interval");

  if (!symbol || !isSupportedMarketSymbol(symbol)) {
    return new Response("Unsupported market symbol", { status: 404 });
  }

  if (!interval) {
    return new Response("Missing candle interval", { status: 400 });
  }

  const clientKey = readRequiredSseClientKey(request.url);

  if (!clientKey) {
    return new Response("Missing SSE client key", { status: 400 });
  }

  const upstreamParams = new URLSearchParams({
    symbol,
    interval,
    [SSE_CLIENT_KEY_PARAM]: clientKey,
  });

  return proxySseStream({
    failureMessage: "Failed to open unified futures market stream",
    request,
    upstreamHeaders: createSseUpstreamHeaders(request),
    upstreamUrl: `${FUTURES_API_BASE_URL}/api/futures/markets/stream?${upstreamParams.toString()}`,
  });
}
