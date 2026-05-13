# Realtime Delivery Rules

## Purpose

This document owns `stream` module rules for realtime and SSE delivery.

## Ownership

`stream` owns:

- SSE emitter delivery mechanics.
- Subscriber lifecycle and stream fan-out.
- Stream-specific telemetry.
- Realtime delivery DTOs/events that are independent from external provider raw DTOs and persistence models.

`stream` does not own:

- Bitget raw WebSocket DTOs or external connector runtime.
- JPA entities, QueryDSL, Flyway, persistence adapters.
- Product/domain source-of-truth rules.

## Subscriber Lifecycle Rule

Repeated per-key subscriber maps, semaphores, register/unregister flows, and stale cleanup should be implemented through shared stream runtime mechanisms instead of duplicated in feature brokers.

Feature brokers should focus on:

- stream key selection,
- stream-specific error messages,
- telemetry stream names,
- event listener wiring,
- payload conversion and fan-out.

## Isolation Rule

`stream` must not import `storage` or `external` concrete types.
`app` is the composition boundary that translates external provider updates or storage reads into stream-facing events.

## Related Documents

- [../../docs/architecture/package-and-wiring.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/architecture/package-and-wiring.md)
- [../../app/docs/assembly-and-runtime.md](/Users/hj.park/projects/coin-zzickmock/backend/app/docs/assembly-and-runtime.md)

