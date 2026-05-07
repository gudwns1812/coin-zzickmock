import { isSupportedMarketSymbol } from "@/lib/markets";
import { FUTURES_API_BASE_URL } from "@/lib/futures-env";
import { readRequiredSseClientKey, SSE_CLIENT_KEY_PARAM } from "@/lib/sse-client-key";
import { createSseUpstreamHeaders, proxySseStream } from "@/lib/sse-proxy";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

type RouteContext = {
  params: Promise<{
    symbol: string;
  }>;
};

export async function GET(request: Request, context: RouteContext) {
  const { symbol } = await context.params;

  if (!isSupportedMarketSymbol(symbol)) {
    return new Response("Unsupported market symbol", {
      status: 404,
    });
  }

  const interval = new URL(request.url).searchParams.get("interval");

  if (!interval) {
    return new Response("Missing candle interval", {
      status: 400,
    });
  }

  const clientKey = readRequiredSseClientKey(request.url);

  if (!clientKey) {
    return new Response("Missing SSE client key", {
      status: 400,
    });
  }

  const upstreamParams = new URLSearchParams({
    interval,
    [SSE_CLIENT_KEY_PARAM]: clientKey,
  });

  return proxySseStream({
    failureMessage: "Failed to open futures market candle stream",
    request,
    upstreamHeaders: createSseUpstreamHeaders(request),
    upstreamUrl: `${FUTURES_API_BASE_URL}/api/futures/markets/${encodeURIComponent(symbol)}/candles/stream?${upstreamParams.toString()}`,
  });
}
