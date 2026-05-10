import { FUTURES_API_BASE_URL } from "@/lib/futures-env";
import { isSupportedMarketSymbol } from "@/lib/markets";
import { readRequiredSseClientKey, SSE_CLIENT_KEY_PARAM } from "@/lib/sse-client-key";
import { createSseUpstreamHeaders, proxySseStream } from "@/lib/sse-proxy";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

function parseSymbols(rawSymbols: string | null) {
  if (!rawSymbols) {
    return null;
  }

  const symbols = rawSymbols
    .split(",")
    .map((symbol) => symbol.trim())
    .filter(Boolean);

  if (symbols.length === 0 || symbols.some((symbol) => !isSupportedMarketSymbol(symbol))) {
    return null;
  }

  return Array.from(new Set(symbols));
}

export async function GET(request: Request) {
  const url = new URL(request.url);
  const symbols = parseSymbols(url.searchParams.get("symbols"));

  if (!symbols) {
    return new Response("Unsupported market symbol", { status: 404 });
  }

  const clientKey = readRequiredSseClientKey(request.url);

  if (!clientKey) {
    return new Response("Missing SSE client key", { status: 400 });
  }

  const upstreamParams = new URLSearchParams({
    symbols: symbols.join(","),
    [SSE_CLIENT_KEY_PARAM]: clientKey,
  });

  return proxySseStream({
    failureMessage: "Failed to open futures market summary stream",
    request,
    upstreamHeaders: createSseUpstreamHeaders(request),
    upstreamUrl: `${FUTURES_API_BASE_URL}/api/futures/markets/summary/stream?${upstreamParams.toString()}`,
  });
}
