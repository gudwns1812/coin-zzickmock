import assert from "node:assert/strict";
import test from "node:test";

const policyModule: typeof import("../hooks/resilientEventSourcePolicy") =
  await import(new URL("../hooks/resilientEventSourcePolicy.ts", import.meta.url).href);

const {
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

test("visible tab return always forces reconnect", () => {
  assert.equal(shouldForceReconnectOnVisible(), true);
});
