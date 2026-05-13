# Backend AGENTS.md

이 파일은 `backend/` 아래에서 작업하는 에이전트용 scoped routing 문서다.
상위 [../AGENTS.md](/Users/hj.park/projects/coin-zzickmock/AGENTS.md)와 [../BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)를 대체하지 않는다.
상위 문서가 공통 규칙을 정하고, 이 파일은 backend module별 추가 읽기 순서를 제공한다.

## Authority Order

1. [../AGENTS.md](/Users/hj.park/projects/coin-zzickmock/AGENTS.md)
2. [../BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)
3. 이 파일
4. 각 module의 `AGENTS.md`
5. [docs/README.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/README.md) 또는 각 module `docs/`

Scoped `AGENTS.md`는 상위 규칙을 override하지 않는다.
충돌하면 더 상위 문서가 우선이며, 문서 구조 변경 자체가 필요하면 같은 작업에서 governing document를 함께 갱신한다.

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
`coin.coinzzickmock.core`, `coin.coinzzickmock.storage` 같은 Java top-level package를 만들지 않는다.

## Required Reads

모든 backend 작업:

1. [../BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)
2. [docs/README.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/README.md)
3. 변경 module의 `AGENTS.md`

공통 구조, layer, module boundary:

- [docs/architecture/foundations.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/architecture/foundations.md)
- [docs/architecture/package-and-wiring.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/architecture/package-and-wiring.md)
- [docs/architecture/testing-and-architecture-lint.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/architecture/testing-and-architecture-lint.md)

클린 코드, 네이밍, 예외:

- [docs/code-quality/clean-code-responsibility.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/code-quality/clean-code-responsibility.md)
- [docs/code-quality/technical-naming-rules.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/code-quality/technical-naming-rules.md)
- [docs/errors/exception-rules.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/errors/exception-rules.md)

## Module Routing

- `core/`: [core/AGENTS.md](/Users/hj.park/projects/coin-zzickmock/backend/core/AGENTS.md)
- `app/`: [app/AGENTS.md](/Users/hj.park/projects/coin-zzickmock/backend/app/AGENTS.md)
- `storage/`: [storage/AGENTS.md](/Users/hj.park/projects/coin-zzickmock/backend/storage/AGENTS.md)
- `stream/`: [stream/AGENTS.md](/Users/hj.park/projects/coin-zzickmock/backend/stream/AGENTS.md)
- `external/`: [external/AGENTS.md](/Users/hj.park/projects/coin-zzickmock/backend/external/AGENTS.md)

## Verification

문서만 바꾼 경우:

```bash
rtk git diff --check
```

backend source, Gradle, lint 규칙, runtime wiring을 바꾼 경우:

```bash
cd backend
./gradlew architectureLint --console=plain
./gradlew check --console=plain
```

