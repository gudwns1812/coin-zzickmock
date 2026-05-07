# SSE clientKey/tabId cleanup

## Status

- State: approved for implementation
- Source PRD: `.omx/plans/prd-sse-clientkey-tabid-cleanup.md`
- Source test spec: `.omx/plans/test-spec-sse-clientkey-tabid-cleanup.md`
- Source interview spec: `.omx/specs/deep-interview-sse-clientkey-tabid-cleanup.md`

## Goal

Phase 1 prevents duplicate SSE emitters from accumulating by making current SSE subscriptions client-key aware across market summary, market candle, and trading execution streams.

## Progress

- [x] Deep interview completed and requirements captured.
- [x] Ralplan draft PRD created.
- [x] Ralplan draft test spec created.
- [x] Architect review completed; required revisions applied.
- [x] Critic review approved after observability revision.
- [x] Implementation completed.
- [x] Verification completed.

## Plan

1. Backend common registry: add clientKey-aware storage plus a clientKey-aware open/reserve contract so same-identity replacement cannot be rejected merely because current limits are full.
2. Backend brokers/controllers: normalize/validate/fallback clientKey at web boundary and propagate it across market summary, market candle, and trading execution SSE.
3. Frontend/proxy: create tab-scoped clientKey and require/forward it through current SSE proxy routes.
4. Observability: add `replaced` as a bounded SSE close reason in telemetry tests/code and update `backend-observability-signal-map.md` in the implementation change.
5. Tests: targeted registry/broker/controller/telemetry/proxy/helper tests, then architecture/build checks.
6. Runtime smoke: verify same-tab stability and cross-tab distinct ids without UI behavior changes.

## Notes

Phase 1 must be stream-consolidation-ready but must not start physical stream consolidation. SharedWorker remains phase 4.

Architect review required the plan to make reservation/opening clientKey-aware, not only registration clientKey-aware, to prevent full-limit duplicate reconnect rejection.

Critic review required explicit `replaced` telemetry acceptance coverage and observability signal-map update when implementation adds that close reason.

Ralplan consensus completed: Architect and Critic both approved after revisions. Implementation should start from the PRD and test spec above.

Implementation completed:

- Backend registry now stores subscriptions by `(subscriptionKey, clientKey)` and supports same-client replacement without extra permanent permits.
- Market summary, market candle, and trading execution SSE paths propagate clientKey.
- Frontend stores a tab-scoped clientKey in `sessionStorage`, appends it to current SSE URLs, and Next proxy routes require/forward it.
- SSE telemetry includes bounded `replaced` close reason.

Verification completed:

- `cd backend && ./gradlew test --tests '*Sse*Test' --tests '*MicrometerSseTelemetryTest' --tests '*MarketControllerTest' --tests '*OrderControllerTest' --console=plain`
- `cd backend && ./gradlew architectureLint --console=plain`
- `cd backend && ./gradlew check --console=plain`
- `npm test --workspace frontend`
- `npm run lint --workspace frontend`
- `rm -rf frontend/.next && npm run build --workspace frontend`
