# Storage Schema And Migration

## Purpose

This document owns the storage workflow for schema and migration changes.
The generated schema artifact remains [../../../docs/generated/db-schema.md](/Users/hj.park/projects/coin-zzickmock/docs/generated/db-schema.md) because product, release, and backend agents all read it.

## Source Of Truth

- Flyway migrations under `backend/storage/src/main/resources/db/migration` are the executable schema source.
- [../../../docs/generated/db-schema.md](/Users/hj.park/projects/coin-zzickmock/docs/generated/db-schema.md) is the generated/current schema reference for humans and agents.
- JPA entities and QueryDSL code must match the executable schema.

## Update Rule

When changing DB schema:

1. Add a new Flyway migration under `backend/storage/src/main/resources/db/migration`.
2. Update JPA/QueryDSL/persistence adapters as needed.
3. Update [../../../docs/generated/db-schema.md](/Users/hj.park/projects/coin-zzickmock/docs/generated/db-schema.md).
4. Run storage-targeted tests and backend architecture verification.

If no schema changes are made, do not add a migration.

## Verification

```bash
cd backend
./gradlew :storage:test --console=plain
./gradlew :app:verifyBootJarStorageMigrations --console=plain
./gradlew architectureLint --console=plain
```

