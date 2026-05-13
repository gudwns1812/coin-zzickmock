# Storage AGENTS.md

이 파일은 `backend/storage/` scoped routing 문서다.
상위 [../AGENTS.md](/Users/hj.park/projects/coin-zzickmock/backend/AGENTS.md)를 대체하지 않는다.

## Scope

`storage`는 persistence leaf adapter module이다.
JPA entity, Spring Data repository, QueryDSL, Flyway migration, persistence mapper/query/specification, DB resources를 소유한다.

## Required Reads

1. [../AGENTS.md](/Users/hj.park/projects/coin-zzickmock/backend/AGENTS.md)
2. [docs/README.md](/Users/hj.park/projects/coin-zzickmock/backend/storage/docs/README.md)
3. [docs/persistence-rules.md](/Users/hj.park/projects/coin-zzickmock/backend/storage/docs/persistence-rules.md)
4. [docs/schema-and-migration.md](/Users/hj.park/projects/coin-zzickmock/backend/storage/docs/schema-and-migration.md)
5. [../../docs/generated/db-schema.md](/Users/hj.park/projects/coin-zzickmock/docs/generated/db-schema.md) for schema work

## Rules

- `storage` depends only on `core` among backend project modules.
- `storage` does not import `app`, `stream`, or `external`.
- `storage` does not own product/domain rules. It implements persistence for core-owned contracts.
- DB schema changes require a new Flyway migration under `backend/storage/src/main/resources/db/migration` and an update to `docs/generated/db-schema.md`.

## Verification

For storage source/schema changes:

```bash
cd backend
./gradlew :storage:test --console=plain
./gradlew architectureLint --console=plain
./gradlew check --console=plain
```

