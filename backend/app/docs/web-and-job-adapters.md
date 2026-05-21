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


## App-owned security adapter

`backend/app` owns the Spring Security servlet/resource-server boundary for `/api/futures/**`.
Spring Security types stay in app runtime/config/provider implementation code and must not leak into `core`, `storage`, `stream`, or `external`.

The auth migration uses three ordered chains:

1. `OPTIONS /api/futures/**` preflight is stateless and `permitAll`.
2. Auth recovery endpoints (`POST /api/futures/auth/register`, `/duplicate`, `/login`, `/logout`) are stateless, keep the unsafe-method Origin guard, but do not resolve the `accessToken` cookie. This preserves recovery from malformed, expired, or stale cookies.
3. The remaining `/api/futures/**` API chain resolves the existing `accessToken` cookie as the resource-server bearer token. Public reads permit anonymous requests when the cookie is absent, authenticate to a DB-backed `Actor` when the cookie is valid, and return the standard `UNAUTHORIZED` JSON when the cookie is malformed, expired, non-ACCESS, or points to an inactive member.

JWT claims are compatibility input only. The authenticated principal and authorities are derived from active member lookup at the request boundary, and admin access uses the DB-backed actor role. Existing controller-level admin guards may remain as defense-in-depth during the migration window.

Security failures must use the same public JSON error contract as application failures: `ErrorResponse(code, message)` with `UNAUTHORIZED` or `FORBIDDEN`. Token/cookie raw values and raw member ids must not be logged or emitted as metric labels.

## Related Documents

- [../../docs/architecture/foundations.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/architecture/foundations.md)
- [../../docs/code-quality/clean-code-responsibility.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/code-quality/clean-code-responsibility.md)

