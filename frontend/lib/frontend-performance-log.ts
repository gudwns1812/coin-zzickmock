const REQUEST_ID_HEADER = "X-Request-Id";
const CORRELATION_ID_HEADER = "X-Correlation-Id";

type FrontendLogValue = string | number | boolean | null | undefined;
type PresentLogEntry = [string, Exclude<FrontendLogValue, null | undefined>];

export type FrontendApiTiming = {
  pathPattern: string;
  method?: string;
  route?: string;
};

export function createFrontendRequestId() {
  const randomId =
    typeof crypto !== "undefined" && "randomUUID" in crypto
      ? crypto.randomUUID()
      : Math.random().toString(36).slice(2);
  return `fe-${randomId}`;
}

export function logFrontendPageTiming(fields: {
  route: string;
  durationMs: number;
  source: "initial_load" | "route_change";
  ttfbMs?: number | null;
}) {
  logFrontendPerformance("frontend.page.completed", {
    service: "frontend",
    route: fields.route,
    source: fields.source,
    durationMs: Math.round(fields.durationMs),
    ttfbMs: fields.ttfbMs == null ? null : Math.round(fields.ttfbMs),
  });
}

export async function fetchWithFrontendTiming(
  input: RequestInfo | URL,
  init: RequestInit = {},
  timing: FrontendApiTiming
): Promise<Response> {
  const startedAt = now();
  const method = timing.method ?? init.method ?? "GET";
  const requestId = createFrontendRequestId();
  const headers = new Headers(init.headers);
  headers.set(REQUEST_ID_HEADER, requestId);
  headers.set(CORRELATION_ID_HEADER, requestId);

  let status: number | null = null;
  let result = "network_error";

  try {
    const response = await fetch(input, {
      ...init,
      headers,
    });
    status = response.status;
    result = response.ok ? "success" : response.status >= 500 ? "server_error" : "client_error";
    return response;
  } finally {
    logFrontendPerformance("frontend.api.completed", {
      service: "frontend",
      method,
      pathPattern: timing.pathPattern,
      route: timing.route ?? currentRoute(),
      status,
      result,
      durationMs: Math.round(now() - startedAt),
      requestId,
    });
  }
}

function logFrontendPerformance(
  event: "frontend.page.completed" | "frontend.api.completed",
  fields: Record<string, FrontendLogValue>
) {
  if (process.env.NODE_ENV === "production") {
    return;
  }

  if (typeof console === "undefined") {
    return;
  }

  console.info(
    [
      `event=${event}`,
      ...Object.entries(fields)
        .filter(isPresentLogEntry)
        .map(([key, value]) => `${key}=${formatLogValue(value)}`),
    ].join(" ")
  );
}

function isPresentLogEntry(
  entry: [string, FrontendLogValue]
): entry is PresentLogEntry {
  return entry[1] !== undefined && entry[1] !== null;
}

function formatLogValue(value: Exclude<FrontendLogValue, null | undefined>) {
  if (typeof value === "number" || typeof value === "boolean") {
    return String(value);
  }
  return value.replaceAll(/\s+/g, "_");
}

function now() {
  if (typeof performance === "undefined") {
    return Date.now();
  }
  return performance.now();
}

function currentRoute() {
  if (typeof window === "undefined") {
    return "server";
  }
  return window.location.pathname || "/";
}
