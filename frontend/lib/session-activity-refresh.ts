export const SESSION_ACTIVITY_REFRESH_WINDOW_SECONDS = 15 * 60;
export const SESSION_ACTIVITY_REFRESH_THROTTLE_MS = 5 * 60 * 1000;
export const SESSION_ACTIVITY_REFRESH_EXTENSION_SECONDS = 60 * 60;

export type SessionActivityRefreshInput = {
  expiresAt?: number;
  lastAttemptedAtMs: number;
  nowMs: number;
};

export function shouldRefreshSessionOnActivity({
  expiresAt,
  lastAttemptedAtMs,
  nowMs,
}: SessionActivityRefreshInput) {
  if (!expiresAt) {
    return false;
  }

  if (nowMs - lastAttemptedAtMs < SESSION_ACTIVITY_REFRESH_THROTTLE_MS) {
    return false;
  }

  const nowSeconds = Math.floor(nowMs / 1000);
  return expiresAt - nowSeconds <= SESSION_ACTIVITY_REFRESH_WINDOW_SECONDS;
}
