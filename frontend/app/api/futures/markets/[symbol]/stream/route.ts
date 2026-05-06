import { isSupportedMarketSymbol } from "@/lib/markets";
import { FUTURES_API_BASE_URL } from "@/lib/futures-env";

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

  const upstreamHeaders = new Headers({
    Accept: "text/event-stream",
    "Cache-Control": "no-cache",
  });
  const cookie = request.headers.get("cookie");

  if (cookie) {
    upstreamHeaders.set("Cookie", cookie);
  }

  try {
    const upstreamResponse = await fetch(
      `${FUTURES_API_BASE_URL}/api/futures/markets/${encodeURIComponent(symbol)}/stream`,
      {
        headers: upstreamHeaders,
        cache: "no-store",
        signal: request.signal,
      }
    );

    if (!upstreamResponse.ok || !upstreamResponse.body) {
      return new Response("Failed to open futures market stream", {
        status: upstreamResponse.status || 502,
      });
    }

    return new Response(upstreamResponse.body, {
      status: upstreamResponse.status,
      headers: {
        "Content-Type":
          upstreamResponse.headers.get("content-type") ?? "text/event-stream",
        "Cache-Control": "no-cache, no-transform",
        "X-Accel-Buffering": "no",
      },
    });
  } catch {
    return new Response("Failed to open futures market stream", {
      status: 502,
    });
  }
}
