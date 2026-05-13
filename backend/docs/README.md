# Backend Docs

이 디렉터리는 backend-wide canonical rules를 소유한다.
Module-specific rules는 각 module의 `docs/` 아래에 둔다.

## Authority

- 상위 라우팅: [../../AGENTS.md](/Users/hj.park/projects/coin-zzickmock/AGENTS.md), [../../BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)
- Backend scoped routing: [../AGENTS.md](/Users/hj.park/projects/coin-zzickmock/backend/AGENTS.md)
- 이 디렉터리: backend-wide invariant의 canonical source
- Module docs: module-local rule의 canonical source

## Backend-wide Canonical Documents

### Architecture

- [architecture/foundations.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/architecture/foundations.md)
  Backend target structure, fixed layers, module boundary, and reading guide.
- [architecture/package-and-wiring.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/architecture/package-and-wiring.md)
  Java package shape, concrete-class-first rule, Spring wiring boundary, shared web/runtime mechanisms.
- [architecture/testing-and-architecture-lint.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/architecture/testing-and-architecture-lint.md)
  Backend test layering, `architectureLint` contract, and lint ratchet.

### Code Quality

- [code-quality/clean-code-responsibility.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/code-quality/clean-code-responsibility.md)
  Backend-wide class/method responsibility and clean-code checklists.
- [code-quality/technical-naming-rules.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/code-quality/technical-naming-rules.md)
  Backend naming rules that prevent technology details from leaking into names.

### Errors

- [errors/exception-rules.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/errors/exception-rules.md)
  `CoreException`, `ErrorCode`, exception translation, and HTTP error response rules.

## Module Canonical Documents

- [../core/docs/README.md](/Users/hj.park/projects/coin-zzickmock/backend/core/docs/README.md)
  Business core, domain modeling, application/provider contracts.
- [../app/docs/README.md](/Users/hj.park/projects/coin-zzickmock/backend/app/docs/README.md)
  Executable app, boot/config/assembly, web/job adapters, runtime artifact verification.
- [../storage/docs/README.md](/Users/hj.park/projects/coin-zzickmock/backend/storage/docs/README.md)
  Persistence boundary, JPA/QueryDSL/Flyway, schema/migration workflow.
- [../stream/docs/README.md](/Users/hj.park/projects/coin-zzickmock/backend/stream/docs/README.md)
  SSE/realtime delivery, subscription lifecycle, stream telemetry.
- [../external/docs/README.md](/Users/hj.park/projects/coin-zzickmock/backend/external/docs/README.md)
  External connector boundary, Bitget isolation, reconnect/runtime and failure translation.

## Compatibility Map

The old `docs/design-docs/backend-design/` files are compatibility pointers.
They should not own rule bodies after this restructuring.

| Old path | New canonical path |
| --- | --- |
| `docs/design-docs/backend-design/01-architecture-foundations.md` | `backend/docs/architecture/foundations.md` |
| `docs/design-docs/backend-design/02-package-and-wiring.md` | `backend/docs/architecture/package-and-wiring.md` plus `backend/app/docs/assembly-and-runtime.md` and `backend/stream/docs/realtime-delivery-rules.md` |
| `docs/design-docs/backend-design/03-application-and-providers.md` | `backend/core/docs/application-and-providers.md` |
| `docs/design-docs/backend-design/04-domain-modeling-rules.md` | `backend/core/docs/domain-modeling-rules.md` |
| `docs/design-docs/backend-design/05-testing-and-lint.md` | `backend/docs/architecture/testing-and-architecture-lint.md` plus module docs where verification is module-specific |
| `docs/design-docs/backend-design/06-persistence-rules.md` | `backend/storage/docs/persistence-rules.md` plus `backend/storage/docs/schema-and-migration.md` and `docs/generated/db-schema.md` |
| `docs/design-docs/backend-design/07-clean-code-responsibility.md` | `backend/docs/code-quality/clean-code-responsibility.md` |
| `docs/design-docs/backend-design/08-external-integration-rules.md` | `backend/external/docs/external-integration-rules.md` plus `backend/external/docs/bitget-reference-map.md` |
| `docs/design-docs/backend-design/09-exception-rules.md` | `backend/docs/errors/exception-rules.md` |
| `docs/design-docs/backend-design/10-technical-naming-rules.md` | `backend/docs/code-quality/technical-naming-rules.md` |
| `docs/design-docs/backend-design/storage-contract-inventory-2026-05-13.md` | `backend/storage/docs/storage-contract-inventory-2026-05-13.md` |

## Maintenance Rule

- Backend-wide invariant changes update this directory.
- Module-specific rule changes update the owning module docs.
- Entry documents should link to canonical docs instead of duplicating rule bodies.
- Old compatibility pointers must remain short and must not drift into duplicate documentation.
