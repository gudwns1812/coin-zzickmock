# Backend Technical Naming Rules

## Purpose

이 문서는 backend class, interface, field naming 중 기술 세부사항을 이름에 드러내지 않는 규칙만 소유한다.
일반 책임 분리와 클린 코드 기준은 [07-clean-code-responsibility.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/07-clean-code-responsibility.md)가 소유한다.

먼저 읽어야 하는 문서:

- [README.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/README.md)
- [01-architecture-foundations.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/01-architecture-foundations.md)

## Technical Naming Rule

기술 세부사항은 클래스명보다 패키지와 프레임워크 타입으로 드러내는 것을 우선한다.
동일한 기능을 여러 기술로 구현해야 한다면 기술명을 클래스명에 붙여 구분하지 않고, 먼저 기술 중립적인 계약을 둔다.
계약 이름은 application/domain 언어의 역할을 드러내고, 구현 기술 선택은 `infrastructure` 하위 패키지, Spring configuration, profile, qualifier 같은 조립 정보로 구분한다.
인터페이스는 이 경우처럼 실제 다중 구현이나 외부 경계 격리가 있을 때만 만들며, 단일 구현을 위해 기계적으로 만들지 않는다.

권장:

- `OrderPersistenceRepository`
- `TradingAccountEntity`
- `MarketDataConnector`
- `LeaderboardSnapshotStore`

금지:

- `TradingAccountJpaEntity`
- `LoadPortfolioPort`
- `RedisLeaderboardSnapshotStore`
- `JpaLeaderboardSnapshotStore`
- `OrderManager`
- `OrderHelper`
- `CommonService`

추가 규칙:

- 엔티티, repository 구현, Spring Data 인터페이스 이름에는 `Jpa`, `SpringData`, `MyBatis`, `Redis` 같은 기술명을 넣지 않는다.
- 기술 세부사항은 패키지 경로, annotation, `JpaRepository` 같은 프레임워크 타입으로만 드러내고 클래스명에는 넣지 않는다.
- 같은 역할의 구현이 둘 이상이면 `LeaderboardSnapshotStore`처럼 역할 중심 인터페이스를 두고, 구현체는 `infrastructure/persistence`, `infrastructure/store`, `providers/infrastructure/config` 같은 위치와 bean 조립으로 선택한다.
- 불리언은 반드시 술어형으로 작성한다.

예:

- `isActive`
- `hasPosition`
- `canTrade`
- `shouldLiquidate`

## Related Documents

- [02-package-and-wiring.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/02-package-and-wiring.md)
- [06-persistence-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/06-persistence-rules.md)
- [07-clean-code-responsibility.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/07-clean-code-responsibility.md)
- [08-external-integration-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/08-external-integration-rules.md)
