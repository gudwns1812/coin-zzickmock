import assert from "node:assert/strict";
import test from "node:test";

const sessionRefreshModule: typeof import("./session-activity-refresh") =
  await import(new URL("./session-activity-refresh.ts", import.meta.url).href);

const {
  SESSION_ACTIVITY_REFRESH_THROTTLE_MS,
  shouldRefreshSessionOnActivity,
} = sessionRefreshModule;

test("does not refresh active sessions that are far from expiry", () => {
  const nowMs = Date.parse("2026-04-28T00:00:00Z");

  assert.equal(
    shouldRefreshSessionOnActivity({
      expiresAt: Math.floor(nowMs / 1000) + 60 * 30,
      lastAttemptedAtMs: 0,
      nowMs,
    }),
    false
  );
});

test("refreshes on activity when the session is near expiry", () => {
  const nowMs = Date.parse("2026-04-28T00:00:00Z");

  assert.equal(
    shouldRefreshSessionOnActivity({
      expiresAt: Math.floor(nowMs / 1000) + 60 * 10,
      lastAttemptedAtMs: 0,
      nowMs,
    }),
    true
  );
});

test("throttles repeated refresh attempts from noisy activity", () => {
  const nowMs = Date.parse("2026-04-28T00:00:00Z");

  assert.equal(
    shouldRefreshSessionOnActivity({
      expiresAt: Math.floor(nowMs / 1000) + 60,
      lastAttemptedAtMs: nowMs - SESSION_ACTIVITY_REFRESH_THROTTLE_MS + 1000,
      nowMs,
    }),
    false
  );
});
