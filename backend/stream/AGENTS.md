# Stream AGENTS.md

이 파일은 `backend/stream/` scoped routing 문서다.
상위 [../AGENTS.md](/Users/hj.park/projects/coin-zzickmock/backend/AGENTS.md)를 대체하지 않는다.

## Scope

`stream`은 realtime/SSE leaf adapter module이다.
SSE delivery, subscription lifecycle, realtime fan-out, stream telemetry, stream-owned event/command/result API를 소유한다.

## Required Reads

1. [../AGENTS.md](/Users/hj.park/projects/coin-zzickmock/backend/AGENTS.md)
2. [docs/README.md](/Users/hj.park/projects/coin-zzickmock/backend/stream/docs/README.md)
3. [docs/realtime-delivery-rules.md](/Users/hj.park/projects/coin-zzickmock/backend/stream/docs/realtime-delivery-rules.md)
4. [../docs/architecture/package-and-wiring.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/architecture/package-and-wiring.md)

## Rules

- `stream` depends only on `core` among backend project modules.
- `stream` does not import storage or external adapter implementations.
- Bitget raw DTOs, JPA entities, QueryDSL, Flyway, and persistence adapters do not belong here.
- Subscriber lifecycle should prefer shared stream mechanisms over ad-hoc broker state.

## Verification

For stream source changes:

```bash
cd backend
./gradlew :stream:test --console=plain
./gradlew architectureLint --console=plain
./gradlew check --console=plain
```

