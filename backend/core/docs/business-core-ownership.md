# Business Core Ownership

## Purpose

This document defines what belongs in `backend/core`.
It is module-specific and complements the backend-wide architecture rules in [../../docs/architecture/foundations.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/architecture/foundations.md).

## Ownership

`core` owns:

- `coin.coinzzickmock.common` contracts and pure shared utilities.
- `coin.coinzzickmock.providers` contracts.
- `feature/<feature>/domain`.
- `feature/<feature>/application`.
- Technology-neutral repository, gateway, provider, application DTO/input/output/projection/query, policy, and event contracts.

`core` does not own:

- Spring Boot executable configuration.
- HTTP controller, request/response delivery, or scheduler trigger classes.
- JPA entities, QueryDSL, Flyway migrations, persistence adapters.
- SSE transport and `SseEmitter` delivery implementation unless a backend-wide contract explicitly allows it.
- Bitget raw DTOs, SDK clients, WebSocket runtime, external HTTP adapter implementation.

## Dependency Rule

`core` must not depend on backend project modules.
It may use stable framework contracts only when explicitly allowed by backend-wide architecture rules.

## Migration Guardrail

When moving code into `core`, first classify it:

- Move: pure domain/application contract or implementation without adapter technology imports.
- Keep outside: anything that imports persistence, external SDK, web delivery, scheduler runtime, or concrete adapter implementation.

The storage movement inventory in [../../storage/docs/storage-contract-inventory-2026-05-13.md](/Users/hj.park/projects/coin-zzickmock/backend/storage/docs/storage-contract-inventory-2026-05-13.md) is the current reference for storage-to-core classification.
