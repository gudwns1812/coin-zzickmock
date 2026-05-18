# App Assembly And Runtime

## Purpose

This document defines `backend/app` ownership.
It is the canonical module document for executable runtime assembly.

## Ownership

`app` owns:

- `CoinZzickmockApplication`.
- Spring Boot executable plugin and `bootJar`.
- Application profile configuration.
- Component scan and runtime configuration.
- Leaf adapter wiring across `core`, `stream`, `storage`, and `external`.
- Runtime verification that storage Flyway migrations are packaged in the executable app artifact.

## Wiring Rule

`app` is the only module that may compose leaf adapters together.
Concrete leaf adapter imports are limited to `configuration`, `assembly`, or `config` package boundaries.
This applies even when two modules intentionally share the same Java package name; same-package simple-name references to leaf adapter classes are still treated as leaf concrete dependencies.

`web` and `job` packages should call core use cases and application DTO/query/result contracts.
They should not import JPA repositories, Bitget DTOs, external clients, or stream transport concrete classes directly.
If an inbound adapter needs stream delivery, `web` should depend on an app-owned gateway contract and let `configuration` or `config` adapt that contract to the stream module.

## Artifact Rule

The executable artifact is `backend/app/build/libs/app-*.jar`.
When storage migrations move or packaging changes, verify that the `app` boot jar carries Flyway migration resources from `storage`.

## Related Documents

- [../../docs/architecture/package-and-wiring.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/architecture/package-and-wiring.md)
- [../../storage/docs/schema-and-migration.md](/Users/hj.park/projects/coin-zzickmock/backend/storage/docs/schema-and-migration.md)
