import assert from "node:assert/strict";
import test from "node:test";

const policyModule: typeof import("../hooks/resilientEventSourcePolicy") =
  await import(new URL("../hooks/resilientEventSourcePolicy.ts", import.meta.url).href);

const {
  EVENT_SOURCE_VISIBLE_RECONNECT_AFTER_HIDDEN_MS,
  getEventSourceReconnectDelayMs,
  shouldDeferEventSourceReconnect,
  shouldForceReconnectOnVisible,
} = policyModule;

test("event source reconnect backoff grows and caps", () => {
  assert.equal(
    getEventSourceReconnectDelayMs({
      attempt: 0,
      baseDelayMs: 100,
      maxDelayMs: 1_000,
    }),
    100
  );
  assert.equal(
    getEventSourceReconnectDelayMs({
      attempt: 3,
      baseDelayMs: 100,
      maxDelayMs: 1_000,
    }),
    800
  );
  assert.equal(
    getEventSourceReconnectDelayMs({
      attempt: 8,
      baseDelayMs: 100,
      maxDelayMs: 1_000,
    }),
    1_000
  );
});

test("hidden tabs defer background reconnect loops", () => {
  assert.equal(
    shouldDeferEventSourceReconnect({ documentVisibilityState: "hidden" }),
    true
  );
  assert.equal(
    shouldDeferEventSourceReconnect({ documentVisibilityState: "visible" }),
    false
  );
});

test("visible tab return keeps a healthy stream open after a short tab switch", () => {
  assert.equal(
    shouldForceReconnectOnVisible({
      hiddenDurationMs: EVENT_SOURCE_VISIBLE_RECONNECT_AFTER_HIDDEN_MS - 1,
      status: "open",
    }),
    false
  );
});

test("visible tab return reconnects degraded or long-hidden streams", () => {
  assert.equal(
    shouldForceReconnectOnVisible({
      hiddenDurationMs: 1,
      status: "degraded",
    }),
    true
  );
  assert.equal(
    shouldForceReconnectOnVisible({
      hiddenDurationMs: EVENT_SOURCE_VISIBLE_RECONNECT_AFTER_HIDDEN_MS,
      status: "open",
    }),
    true
  );
});
