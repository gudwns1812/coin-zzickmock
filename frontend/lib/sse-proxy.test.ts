import assert from "node:assert/strict";
import test from "node:test";

type ProxySseStream = (options: {
  failureMessage: string;
  fetcher?: (input: string, init?: RequestInit) => Promise<Response>;
  request: Request;
  upstreamHeaders: Headers;
  upstreamUrl: string;
}) => Promise<Response>;
type SseFetcher = (input: string, init?: RequestInit) => Promise<Response>;

const { proxySseStream } = (await import(
  new URL("./sse-proxy.ts", import.meta.url).href
)) as { proxySseStream: ProxySseStream };

async function waitFor(
  predicate: () => boolean,
  message: string,
  timeoutMs = 500
) {
  const startedAt = Date.now();

  while (!predicate()) {
    if (Date.now() - startedAt > timeoutMs) {
      assert.fail(message);
    }

    await new Promise((resolve) => setTimeout(resolve, 10));
  }
}

function createProxyRequest(signal?: AbortSignal) {
  return new Request("http://localhost/api/futures/markets/BTCUSDT/stream", {
    signal,
  });
}

test("SSE proxy aborts upstream when downstream response is cancelled", async () => {
  let upstreamCanceled = false;
  let upstreamSignal: AbortSignal | undefined;

  const fetcher: SseFetcher = async (_input, init) => {
    upstreamSignal = init?.signal as AbortSignal;

    return new Response(
      new ReadableStream<Uint8Array>({
        start(controller) {
          controller.enqueue(new TextEncoder().encode("data: tick\n\n"));
        },
        cancel() {
          upstreamCanceled = true;
        },
      }),
      {
        headers: {
          "Content-Type": "text/event-stream",
        },
      }
    );
  };

  const response = await proxySseStream({
    failureMessage: "failed",
    fetcher,
    request: createProxyRequest(),
    upstreamHeaders: new Headers(),
    upstreamUrl: "http://backend/stream",
  });
  const reader = response.body?.getReader();

  assert.ok(reader);
  assert.equal((await reader.read()).done, false);

  await reader.cancel();

  await waitFor(() => upstreamCanceled, "upstream body was not cancelled");
  assert.equal(upstreamSignal?.aborted, true);
});

test("SSE proxy does not wait for upstream body cancel before aborting fetch", async () => {
  let upstreamSignal: AbortSignal | undefined;

  const fetcher: SseFetcher = async (_input, init) => {
    upstreamSignal = init?.signal as AbortSignal;

    return new Response(
      new ReadableStream<Uint8Array>({
        start(controller) {
          controller.enqueue(new TextEncoder().encode("data: tick\n\n"));
        },
        cancel() {
          return new Promise(() => undefined);
        },
      }),
      {
        headers: {
          "Content-Type": "text/event-stream",
        },
      }
    );
  };

  const response = await proxySseStream({
    failureMessage: "failed",
    fetcher,
    request: createProxyRequest(),
    upstreamHeaders: new Headers(),
    upstreamUrl: "http://backend/stream",
  });
  const reader = response.body?.getReader();

  assert.ok(reader);
  assert.equal((await reader.read()).done, false);

  await reader.cancel();

  assert.equal(upstreamSignal?.aborted, true);
});

test("SSE proxy aborts upstream when the incoming request is aborted", async () => {
  const requestAbortController = new AbortController();
  let upstreamCanceled = false;
  let upstreamSignal: AbortSignal | undefined;

  const fetcher: SseFetcher = async (_input, init) => {
    upstreamSignal = init?.signal as AbortSignal;

    return new Response(
      new ReadableStream<Uint8Array>({
        start(controller) {
          controller.enqueue(new TextEncoder().encode("data: tick\n\n"));
        },
        cancel() {
          upstreamCanceled = true;
        },
      }),
      {
        headers: {
          "Content-Type": "text/event-stream",
        },
      }
    );
  };

  const response = await proxySseStream({
    failureMessage: "failed",
    fetcher,
    request: createProxyRequest(requestAbortController.signal),
    upstreamHeaders: new Headers(),
    upstreamUrl: "http://backend/stream",
  });
  const reader = response.body?.getReader();

  assert.ok(reader);
  assert.equal((await reader.read()).done, false);

  requestAbortController.abort();

  await waitFor(() => upstreamCanceled, "upstream body was not cancelled");
  assert.equal(upstreamSignal?.aborted, true);
  assert.equal((await reader.read()).done, true);
});
