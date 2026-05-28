# Push App AGENTS.md

이 파일은 `backend/push-app/` scoped routing 문서다.
상위 [../AGENTS.md](/Users/hj.park/projects/coin-zzickmock/backend/AGENTS.md)를 대체하지 않는다.

## Scope

`push-app`은 Redis Stream-backed SSE relay 전용 executable Spring Boot module이다.
`app`이 발행한 push event envelope를 Redis Stream consumer group으로 읽고, 구독 중인 SSE client로 fan-out한다.

## Required Reads

1. [../AGENTS.md](/Users/hj.park/projects/coin-zzickmock/backend/AGENTS.md)
2. [docs/README.md](/Users/hj.park/projects/coin-zzickmock/backend/push-app/docs/README.md)
3. [../docs/architecture/foundations.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/architecture/foundations.md)
4. [../docs/architecture/package-and-wiring.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/architecture/package-and-wiring.md)
5. [../stream/docs/realtime-delivery-rules.md](/Users/hj.park/projects/coin-zzickmock/backend/stream/docs/realtime-delivery-rules.md)

## Rules

- `push-app` may depend on `core` and `stream` only.
- Do not add datasource, JPA, Flyway, QueryDSL, storage repositories, external provider connectors, or domain write orchestration to this module.
- Inbound SSE routes live under `feature/push/web`; canonical paths are `/api/futures/stream/**`.
- Redis Stream polling triggers live under `feature/push/job` and delegate relay work to `feature/push/application`.
- Event payload compatibility is owned by the `core` push envelope contract and `stream` response DTOs; do not import `app` implementation classes.

## Verification

For source or build changes:

```bash
cd backend
./gradlew :push-app:test :push-app:bootJar --console=plain
./gradlew architectureLint --console=plain
./gradlew check --console=plain
```
