const SSE_RESPONSE_HEADERS = {
  "Cache-Control": "no-cache, no-transform",
  "X-Accel-Buffering": "no",
};

type ProxySseStreamOptions = {
  failureMessage: string;
  request: Request;
  upstreamHeaders: Headers;
  upstreamUrl: string;
};

export function createSseUpstreamHeaders(request: Request) {
  const headers = new Headers({
    Accept: "text/event-stream",
    "Cache-Control": "no-cache",
  });
  const cookie = request.headers.get("cookie");

  if (cookie) {
    headers.set("Cookie", cookie);
  }

  return headers;
}

export async function proxySseStream({
  failureMessage,
  request,
  upstreamHeaders,
  upstreamUrl,
}: ProxySseStreamOptions) {
  const upstreamAbortController = new AbortController();
  let upstreamReader: ReadableStreamDefaultReader<Uint8Array> | null = null;
  let isClosed = false;

  const closeProxy = () => {
    if (isClosed) {
      return;
    }

    isClosed = true;
    request.signal.removeEventListener("abort", handleRequestAbort);
  };

  const abortUpstream = () => {
    upstreamReader?.cancel().catch(() => undefined);

    if (!upstreamAbortController.signal.aborted) {
      upstreamAbortController.abort();
    }
  };

  function handleRequestAbort() {
    abortUpstream();
  }

  if (request.signal.aborted) {
    abortUpstream();
  } else {
    request.signal.addEventListener("abort", handleRequestAbort, {
      once: true,
    });
  }

  try {
    const upstreamResponse = await fetch(upstreamUrl, {
      headers: upstreamHeaders,
      cache: "no-store",
      signal: upstreamAbortController.signal,
    });

    if (!upstreamResponse.ok || !upstreamResponse.body) {
      closeProxy();
      return new Response(failureMessage, {
        status: upstreamResponse.status || 502,
      });
    }

    upstreamReader = upstreamResponse.body.getReader();

    const stream = new ReadableStream<Uint8Array>({
      async pull(controller) {
        try {
          const reader = upstreamReader;

          if (!reader) {
            closeProxy();
            controller.close();
            return;
          }

          const { done, value } = await reader.read();

          if (done) {
            upstreamReader = null;
            closeProxy();
            controller.close();
            return;
          }

          controller.enqueue(value);
        } catch (error) {
          upstreamReader = null;
          closeProxy();

          if (
            request.signal.aborted ||
            upstreamAbortController.signal.aborted
          ) {
            controller.close();
            return;
          }

          controller.error(error);
        }
      },
      async cancel() {
        const reader = upstreamReader;
        upstreamReader = null;

        try {
          await reader?.cancel();
        } catch {
          // The downstream connection is already gone; the abort below closes upstream.
        }

        abortUpstream();
        closeProxy();
      },
    });

    return new Response(stream, {
      status: upstreamResponse.status,
      headers: {
        ...SSE_RESPONSE_HEADERS,
        "Content-Type":
          upstreamResponse.headers.get("content-type") ?? "text/event-stream",
      },
    });
  } catch {
    closeProxy();

    if (request.signal.aborted || upstreamAbortController.signal.aborted) {
      return new Response(null, {
        status: 204,
      });
    }

    return new Response(failureMessage, {
      status: 502,
    });
  }
}
