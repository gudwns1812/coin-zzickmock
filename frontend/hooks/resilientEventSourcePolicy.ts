export const DEFAULT_EVENT_SOURCE_BASE_RECONNECT_MS = 500;
export const DEFAULT_EVENT_SOURCE_MAX_RECONNECT_MS = 10_000;
export const EVENT_SOURCE_VISIBLE_RECONNECT_AFTER_HIDDEN_MS = 30_000;

export type EventSourceReconnectStatus =
  | "idle"
  | "connecting"
  | "open"
  | "reconnecting"
  | "degraded";

export type EventSourceReconnectReason =
  | "error"
  | "focus"
  | "manual"
  | "online"
  | "visible";

export function getEventSourceReconnectDelayMs({
  attempt,
  baseDelayMs = DEFAULT_EVENT_SOURCE_BASE_RECONNECT_MS,
  maxDelayMs = DEFAULT_EVENT_SOURCE_MAX_RECONNECT_MS,
}: {
  attempt: number;
  baseDelayMs?: number;
  maxDelayMs?: number;
}) {
  const normalizedAttempt = Math.max(0, attempt);
  const exponentialDelay = baseDelayMs * 2 ** normalizedAttempt;

  return Math.min(exponentialDelay, maxDelayMs);
}

export function shouldDeferEventSourceReconnect({
  documentVisibilityState,
}: {
  documentVisibilityState: DocumentVisibilityState | "unknown";
}) {
  return documentVisibilityState === "hidden";
}

export function shouldForceReconnectOnVisible({
  hiddenDurationMs,
  status,
}: {
  hiddenDurationMs: number | null;
  status: EventSourceReconnectStatus;
}) {
  if (status !== "open") {
    return true;
  }

  return (
    hiddenDurationMs !== null &&
    hiddenDurationMs >= EVENT_SOURCE_VISIBLE_RECONNECT_AFTER_HIDDEN_MS
  );
}
