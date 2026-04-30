import { isSupportedMarketSymbol } from "@/lib/markets";

const FUTURES_API_BASE_URL =
  process.env.FUTURES_API_BASE_URL ?? "http://127.0.0.1:8080";

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

  const upstreamHeaders = new Headers({
    Accept: "text/event-stream",
    "Cache-Control": "no-cache",
  });
  const cookie = request.headers.get("cookie");

  if (cookie) {
    upstreamHeaders.set("Cookie", cookie);
  }

  const upstreamParams = new URLSearchParams({ interval });

  try {
    const upstreamResponse = await fetch(
      `${FUTURES_API_BASE_URL}/api/futures/markets/${encodeURIComponent(symbol)}/candles/stream?${upstreamParams.toString()}`,
      {
        headers: upstreamHeaders,
        cache: "no-store",
        signal: request.signal,
      }
    );

    if (!upstreamResponse.ok || !upstreamResponse.body) {
      return new Response("Failed to open futures market candle stream", {
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
    return new Response("Failed to open futures market candle stream", {
      status: 502,
    });
  }
}
