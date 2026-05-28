# App AGENTS.md

이 파일은 `backend/app/` scoped routing 문서다.
상위 [../AGENTS.md](/Users/hj.park/projects/coin-zzickmock/backend/AGENTS.md)를 대체하지 않는다.

## Scope

`app`은 primary executable Spring Boot module이다.
Boot runtime, component scan, profile configuration, user-facing web/job adapters, and leaf adapter assembly를 소유한다.
Redis Stream-backed SSE relay executable은 `../push-app`이 소유한다.

## Required Reads

1. [../AGENTS.md](/Users/hj.park/projects/coin-zzickmock/backend/AGENTS.md)
2. [docs/README.md](/Users/hj.park/projects/coin-zzickmock/backend/app/docs/README.md)
3. [../docs/architecture/foundations.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/architecture/foundations.md)
4. [../docs/architecture/package-and-wiring.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/architecture/package-and-wiring.md)

## Rules

- `app` and `push-app` are the only Spring Boot executable modules; `app` remains the primary API/write runtime.
- `app` may depend on `core`, `stream`, `storage`, and `external`.
- Leaf adapter concrete imports are allowed only in configuration/assembly/config package boundaries.
- `web` and `job` adapters call core use cases and application DTO/query/result contracts; they do not directly depend on storage/external/stream concrete implementation.
- API/SSE URL behavior must not change during assembly refactors unless product docs are updated in the same change.

## Verification

For source or build changes:

```bash
cd backend
./gradlew :app:bootJar --console=plain
./gradlew architectureLint --console=plain
./gradlew check --console=plain
```
