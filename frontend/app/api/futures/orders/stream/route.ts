import { FUTURES_API_BASE_URL } from "@/lib/futures-env";
import { readRequiredSseClientKey, SSE_CLIENT_KEY_PARAM } from "@/lib/sse-client-key";
import { createSseUpstreamHeaders, proxySseStream } from "@/lib/sse-proxy";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

export async function GET(request: Request) {
  const clientKey = readRequiredSseClientKey(request.url);

  if (!clientKey) {
    return new Response("Missing SSE client key", {
      status: 400,
    });
  }

  const upstreamParams = new URLSearchParams({
    [SSE_CLIENT_KEY_PARAM]: clientKey,
  });

  return proxySseStream({
    failureMessage: "Failed to open futures order stream",
    request,
    upstreamHeaders: createSseUpstreamHeaders(request),
    upstreamUrl: `${FUTURES_API_BASE_URL}/api/futures/orders/stream?${upstreamParams.toString()}`,
  });
}
