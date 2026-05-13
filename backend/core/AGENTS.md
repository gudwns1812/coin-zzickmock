# Core AGENTS.md

이 파일은 `backend/core/` scoped routing 문서다.
상위 [../AGENTS.md](/Users/hj.park/projects/coin-zzickmock/backend/AGENTS.md)를 대체하지 않는다.

## Scope

`core`는 business core library다.
`common` contracts, provider contracts, feature `domain`, feature `application` 구현과 계약을 소유한다.

`core`는 backend project dependency를 갖지 않는다.
`coin.coinzzickmock.core` Java package를 만들지 않는다.

## Required Reads

1. [../AGENTS.md](/Users/hj.park/projects/coin-zzickmock/backend/AGENTS.md)
2. [docs/README.md](/Users/hj.park/projects/coin-zzickmock/backend/core/docs/README.md)
3. [../docs/architecture/foundations.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/architecture/foundations.md)
4. [docs/domain-modeling-rules.md](/Users/hj.park/projects/coin-zzickmock/backend/core/docs/domain-modeling-rules.md) for domain work
5. [docs/application-and-providers.md](/Users/hj.park/projects/coin-zzickmock/backend/core/docs/application-and-providers.md) for use case/provider contract work

## Rules

- Domain code stays framework-free.
- Application code orchestrates use cases and depends on domain/provider/repository contracts, not concrete adapters.
- Repository/gateway/provider contracts may live here only when they are technology-neutral and expressed in application/domain language.
- JPA, QueryDSL, Flyway, Redis client, external SDK, HTTP controller, and SSE transport implementation do not belong here.

## Verification

For source changes:

```bash
cd backend
./gradlew architectureLint --console=plain
./gradlew check --console=plain
```

