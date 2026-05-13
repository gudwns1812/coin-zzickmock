# App Web And Job Adapters

## Purpose

This document defines app-owned inbound runtime adapters.

## `web`

`web` owns HTTP/SSE delivery boundaries:

- controllers,
- request/response DTOs,
- request validation,
- authenticated request context parsing,
- response mapping,
- app-level delivery configuration.

`web` must not implement business rules, call repositories directly, or expose persistence/external DTOs.

## `job`

`job` owns scheduler, startup, backfill, retry, and background triggers.
Job classes wake core application use cases; they do not implement transaction orchestration or call persistence/external SDKs directly.

## Related Documents

- [../../docs/architecture/foundations.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/architecture/foundations.md)
- [../../docs/code-quality/clean-code-responsibility.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/code-quality/clean-code-responsibility.md)

