# BACKEND.md

## Purpose

이 문서는 `coin-zzickmock` 백엔드 작업의 입구 문서다.
상세 규칙을 반복하지 않고, 어떤 문서를 먼저 읽을지와 절대 놓치면 안 되는 기준만 연결한다.

## Authority Order

Backend 작업의 문서 권위 순서는 아래와 같다.

1. [AGENTS.md](/Users/hj.park/projects/coin-zzickmock/AGENTS.md)
2. 이 문서
3. [backend/AGENTS.md](/Users/hj.park/projects/coin-zzickmock/backend/AGENTS.md)
4. 변경 module의 `AGENTS.md`
5. [backend/docs/README.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/README.md) 또는 각 module `docs/`

옛 `docs/design-docs/backend-design/` 경로는 compatibility index다.
새 규칙의 원문은 `backend/docs`와 `backend/<module>/docs`에 둔다.

## Backend Modules

```text
backend/
  core/       business core: common contracts, provider contracts, feature domain/application
  app/        executable Spring Boot app: boot, web/job adapters, assembly and configuration
  storage/    persistence leaf adapter: JPA, QueryDSL, Flyway, DB resources
  stream/     realtime leaf adapter: SSE delivery and stream runtime
  external/   external leaf adapter: Bitget and provider connector implementations
```

`core`, `app`, `storage`, `stream`, `external`은 Gradle module 이름이다.
Java top-level package 이름으로 쓰지 않는다.

## Read Order

### Backend 공통 구조, module boundary, lint

1. [backend/AGENTS.md](/Users/hj.park/projects/coin-zzickmock/backend/AGENTS.md)
2. [backend/docs/README.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/README.md)
3. [backend/docs/architecture/foundations.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/architecture/foundations.md)
4. [backend/docs/architecture/package-and-wiring.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/architecture/package-and-wiring.md)
5. [backend/docs/architecture/testing-and-architecture-lint.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/architecture/testing-and-architecture-lint.md)

### `core`: domain, application, provider contracts

1. [backend/core/AGENTS.md](/Users/hj.park/projects/coin-zzickmock/backend/core/AGENTS.md)
2. [backend/core/docs/README.md](/Users/hj.park/projects/coin-zzickmock/backend/core/docs/README.md)
3. [backend/core/docs/domain-modeling-rules.md](/Users/hj.park/projects/coin-zzickmock/backend/core/docs/domain-modeling-rules.md)
4. [backend/core/docs/application-and-providers.md](/Users/hj.park/projects/coin-zzickmock/backend/core/docs/application-and-providers.md)

제품 공식이나 사용자 동작이 바뀌면 [docs/product-specs/README.md](/Users/hj.park/projects/coin-zzickmock/docs/product-specs/README.md)와 관련 명세도 함께 갱신한다.
거래 계산은 특히 [docs/product-specs/coin-futures-simulation-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/product-specs/coin-futures-simulation-rules.md)를 본다.

### `app`: executable app, web/job adapters, runtime assembly

1. [backend/app/AGENTS.md](/Users/hj.park/projects/coin-zzickmock/backend/app/AGENTS.md)
2. [backend/app/docs/README.md](/Users/hj.park/projects/coin-zzickmock/backend/app/docs/README.md)
3. [backend/app/docs/assembly-and-runtime.md](/Users/hj.park/projects/coin-zzickmock/backend/app/docs/assembly-and-runtime.md)
4. [backend/app/docs/web-and-job-adapters.md](/Users/hj.park/projects/coin-zzickmock/backend/app/docs/web-and-job-adapters.md)

### `storage`: DB, persistence, schema

1. [backend/storage/AGENTS.md](/Users/hj.park/projects/coin-zzickmock/backend/storage/AGENTS.md)
2. [backend/storage/docs/README.md](/Users/hj.park/projects/coin-zzickmock/backend/storage/docs/README.md)
3. [backend/storage/docs/persistence-rules.md](/Users/hj.park/projects/coin-zzickmock/backend/storage/docs/persistence-rules.md)
4. [backend/storage/docs/schema-and-migration.md](/Users/hj.park/projects/coin-zzickmock/backend/storage/docs/schema-and-migration.md)
5. [docs/generated/db-schema.md](/Users/hj.park/projects/coin-zzickmock/docs/generated/db-schema.md)

### `stream`: SSE/realtime delivery

1. [backend/stream/AGENTS.md](/Users/hj.park/projects/coin-zzickmock/backend/stream/AGENTS.md)
2. [backend/stream/docs/README.md](/Users/hj.park/projects/coin-zzickmock/backend/stream/docs/README.md)
3. [backend/stream/docs/realtime-delivery-rules.md](/Users/hj.park/projects/coin-zzickmock/backend/stream/docs/realtime-delivery-rules.md)

### `external`: external adapters, Bitget

1. [backend/external/AGENTS.md](/Users/hj.park/projects/coin-zzickmock/backend/external/AGENTS.md)
2. [backend/external/docs/README.md](/Users/hj.park/projects/coin-zzickmock/backend/external/docs/README.md)
3. [backend/external/docs/external-integration-rules.md](/Users/hj.park/projects/coin-zzickmock/backend/external/docs/external-integration-rules.md)
4. [backend/external/docs/bitget-reference-map.md](/Users/hj.park/projects/coin-zzickmock/backend/external/docs/bitget-reference-map.md)

### 클린 코드, 네이밍, 예외

- [backend/docs/code-quality/clean-code-responsibility.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/code-quality/clean-code-responsibility.md)
- [backend/docs/code-quality/technical-naming-rules.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/code-quality/technical-naming-rules.md)
- [backend/docs/errors/exception-rules.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/errors/exception-rules.md)

## Non-negotiables

- 문서와 다른 동작을 만들면 같은 작업에서 governing document를 갱신한다.
- Backend 공통 규칙은 `backend/docs`, module-specific 규칙은 `backend/<module>/docs`가 원문이다.
- Scoped `AGENTS.md`는 상위 규칙을 override하지 않는다.
- 기능은 `feature/<feature-name>` 아래에서 수직으로 자른다.
- 레이어는 `web`, `job`, `application`, `domain`, `infrastructure`로 고정한다.
- `app`만 executable Spring Boot module이다.
- `core`는 business core이고 backend project dependency를 갖지 않는다.
- `stream`, `storage`, `external`은 leaf adapter이며 backend project module 중 `core`에만 의존한다.
- `app`의 leaf adapter concrete import는 configuration/assembly/config 경계에만 둔다.
- DB 변경은 `backend/storage/src/main/resources/db/migration` 아래 새 Flyway migration과 [docs/generated/db-schema.md](/Users/hj.park/projects/coin-zzickmock/docs/generated/db-schema.md) 갱신을 함께 수행한다.
- 비즈니스/도메인 실패는 `CoreException`과 구조화된 error type으로 표현한다.

## Verification

문서만 바꾼 경우:

```bash
rtk git diff --check
```

Backend source, Gradle, lint, runtime wiring을 바꾼 경우:

```bash
cd backend
./gradlew architectureLint --console=plain
./gradlew check --console=plain
```

Module-specific targeted tests are listed in each module `AGENTS.md`.
