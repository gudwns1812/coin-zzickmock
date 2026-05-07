const SSE_RESPONSE_HEADERS = {
  "Cache-Control": "no-cache, no-transform",
  "X-Accel-Buffering": "no",
};

type ProxySseStreamOptions = {
  failureMessage: string;
  fetcher?: (input: string, init?: RequestInit) => Promise<Response>;
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
  fetcher = fetch,
  request,
  upstreamHeaders,
  upstreamUrl,
}: ProxySseStreamOptions) {
  const upstreamAbortController = new AbortController();
  let upstreamReader: ReadableStreamDefaultReader<Uint8Array> | null = null;
  let downstreamController: ReadableStreamDefaultController<Uint8Array> | null =
    null;
  let isClosed = false;
  let isDownstreamClosed = false;

  const closeProxy = () => {
    if (isClosed) {
      return;
    }

    isClosed = true;
    request.signal.removeEventListener("abort", handleRequestAbort);
  };

  const abortUpstream = () => {
    const reader = upstreamReader;
    upstreamReader = null;

    if (!upstreamAbortController.signal.aborted) {
      upstreamAbortController.abort();
    }

    reader?.cancel().catch(() => undefined);
  };

  const closeDownstream = () => {
    if (isDownstreamClosed) {
      return;
    }

    isDownstreamClosed = true;
    const controller = downstreamController;
    downstreamController = null;

    try {
      controller?.close();
    } catch {
      // The browser may have already closed the response body.
    }
  };

  function handleRequestAbort() {
    abortUpstream();
    closeDownstream();
    closeProxy();
  }

  if (request.signal.aborted) {
    abortUpstream();
  } else {
    request.signal.addEventListener("abort", handleRequestAbort, {
      once: true,
    });
  }

  try {
    const upstreamResponse = await fetcher(upstreamUrl, {
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

    const pumpUpstream = async (
      controller: ReadableStreamDefaultController<Uint8Array>
    ) => {
      try {
        while (!isDownstreamClosed) {
          const reader = upstreamReader;

          if (!reader) {
            closeProxy();
            closeDownstream();
            return;
          }

          const { done, value } = await reader.read();

          if (done) {
            upstreamReader = null;
            closeProxy();
            closeDownstream();
            return;
          }

          if (!isDownstreamClosed) {
            controller.enqueue(value);
          }
        }
      } catch (error) {
        upstreamReader = null;
        closeProxy();

        if (
          request.signal.aborted ||
          upstreamAbortController.signal.aborted ||
          isDownstreamClosed
        ) {
          closeDownstream();
          return;
        }

        isDownstreamClosed = true;
        downstreamController = null;
        controller.error(error);
      }
    };

    const stream = new ReadableStream<Uint8Array>({
      start(controller) {
        downstreamController = controller;
        void pumpUpstream(controller);
      },
      async cancel() {
        isDownstreamClosed = true;
        downstreamController = null;

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
