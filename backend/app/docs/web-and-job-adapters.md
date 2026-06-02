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

### Request validation

Spring Bean Validation is a web-boundary tool in this project.

Rules:

- Use Bean Validation annotations only on app-owned `web` request DTOs or web controller parameters.
- Add `@Valid` to `@RequestBody` parameters when the request DTO owns shape validation.
- Map Bean Validation failures through the global error contract as `ErrorResponse(INVALID_REQUEST, message)`.
- Do not add `jakarta.validation` annotations to `core` application/domain types, storage entities/repositories, or external adapters.
- Keep business invariants in domain/application even when web DTOs reject malformed HTTP input earlier.

## `job`

`job` owns scheduler, startup, backfill, retry, and background triggers.
Job classes wake core application use cases; they do not implement transaction orchestration or call persistence/external SDKs directly.


## App-owned security adapter

`backend/app` owns the Spring Security servlet/resource-server boundary for `/api/futures/**`.
Spring Security types stay in app runtime/config/provider implementation code and must not leak into `core`, `storage`, `stream`, or `external`.

The auth migration uses ordered chains:

1. `OPTIONS /api/futures/**` preflight is stateless and `permitAll`.
2. OAuth login endpoints (`/oauth2/authorization/google`, `/login/oauth2/code/google`) use Spring OAuth2 Login with a short-lived HttpOnly authorization-request cookie for OAuth state. They are mounted only when both `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` are present in the backend runtime environment; missing credentials make the entrypoint redirect back to `/login?oauth=unavailable` instead of falling through to a 404. Public Nginx must proxy `/oauth2/**` and `/login/oauth2/**` to this app and preserve original Host/Proto/Port forwarded headers so Spring computes the Google redirect URI from the public backend origin. The authorization-request cookie stores only the required OAuth request fields as JSON and is HMAC-signed before the browser sees it; callback removal records the consumed OAuth state in a bounded TTL server-side replay guard. On callback, linked Google identities receive the existing backend `accessToken` cookie and unlinked identities receive only a short-lived HttpOnly pending onboarding cookie before redirecting to the frontend onboarding route.
3. Auth recovery/onboarding endpoints (`POST /api/futures/auth/logout`, `GET/POST /api/futures/auth/google/**`) are stateless, keep the unsafe-method Origin guard for writes, but do not resolve the `accessToken` cookie. Legacy `/register`, `/duplicate`, and `/login` are disabled by default through `app.auth.legacy-password-endpoints-enabled=false` and exist only as non-production fixture compatibility when explicitly enabled.
4. The remaining `/api/futures/**` API chain resolves the existing `accessToken` cookie as the resource-server bearer token. Public reads permit anonymous requests when the cookie is absent, authenticate to a DB-backed `Actor` when the cookie is valid, and return the standard `UNAUTHORIZED` JSON when the cookie is malformed, expired, non-ACCESS, or points to an inactive member.

JWT claims are compatibility input only. The authenticated principal and authorities are derived from active member lookup at the request boundary, and admin access uses the DB-backed actor role. Existing controller-level admin guards may remain as defense-in-depth during the migration window.

Google onboarding status checks read the pending cookie by hashing it server-side without a write lock. Link/signup completion paths consume the same pending token by locking `member_oauth_pending_links.token_hash` with a unique equality predicate. Linking verifies a legacy account/password once, attaches `member_oauth_identities` to the existing member id, and never copies trading/account data. Fresh Google signup requires the submitted email to match the pending Google provider email and persists the provider email as the member email, so browser-side form edits cannot switch the account email. It creates a normal member profile without local `account/password_hash`, provisions the default trading account synchronously, then links the Google identity in the same short DB transaction; terminal identity conflicts soft-consume the pending row and expire the pending cookie without committing partial signup state.

Security failures must use the same public JSON error contract as application failures: `ErrorResponse(code, message)` with `UNAUTHORIZED` or `FORBIDDEN`. Token/cookie raw values and raw member ids must not be logged or emitted as metric labels.

## Related Documents

- [../../docs/architecture/foundations.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/architecture/foundations.md)
- [../../docs/code-quality/clean-code-responsibility.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/code-quality/clean-code-responsibility.md)
