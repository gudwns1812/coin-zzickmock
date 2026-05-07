import { isSupportedMarketSymbol } from "@/lib/markets";
import { FUTURES_API_BASE_URL } from "@/lib/futures-env";
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

  return proxySseStream({
    failureMessage: "Failed to open futures market stream",
    request,
    upstreamHeaders: createSseUpstreamHeaders(request),
    upstreamUrl: `${FUTURES_API_BASE_URL}/api/futures/markets/${encodeURIComponent(symbol)}/stream`,
  });
}
