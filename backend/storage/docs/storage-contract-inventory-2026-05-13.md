# Storage Contract Inventory (2026-05-13)

## Purpose

This inventory gates movement from `backend/storage` to `backend/core` for the business-core/app-assembly split. It classifies storage-owned Java sources before moving repository/application/domain contracts so JPA, QueryDSL, Specification, `Pageable`, `@Query`, entity classes, persistence annotations, and persistence query models do not leak into `core`.

## Classification Rule

- Move to `core`: pure feature `domain` files and application contracts/results/repository interfaces that do not import persistence or query framework types.
- Keep in `storage`: `infrastructure` adapters, JPA entities, Spring Data repositories, QueryDSL query code, persistence mappers, Flyway resources, generated query types, and any repository-like type with persistence annotations/framework types.
- If a type looks like a repository but imports JPA, QueryDSL, Specification, `Pageable`, `@Query`, entity classes, persistence annotations, or persistence query models, it stays in `storage`.

## Summary

- Core-movable candidates: 99
- Storage-stay implementation files: 55
- Flyway migrations and storage resources stay in `backend/storage/src/main/resources`.

## Movement status

2026-05-13 continuation slice moved the 99 core-movable candidates listed below from `backend/storage/src/main/java` to `backend/core/src/main/java`, preserving Java package names.
`backend/storage/src/main/java` now keeps the storage-stay implementation files listed in this inventory.

## Core-movable candidates

| File | Classification reason |
| --- | --- |
| `src/main/java/coin/coinzzickmock/common/trading/LiquidationFormula.java` | pure shared trading formula used by multiple feature domains |
| `src/main/java/coin/coinzzickmock/feature/account/application/repository/AccountRefillStateRepository.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/account/application/repository/AccountRepository.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/account/application/repository/WalletHistoryRepository.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/account/application/result/AccountMutationResult.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/account/application/result/AccountRefillCreditResult.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/account/application/result/AccountRefillResult.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/account/application/result/AccountRefillStatusResult.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/account/application/result/AccountSummaryResult.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/account/application/result/WalletHistoryResult.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/account/domain/AccountRefillState.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/account/domain/TradingAccount.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/account/domain/WalletHistoryDate.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/account/domain/WalletHistorySnapshot.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/activity/application/repository/DailyActiveUserSummaryRepository.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/activity/application/repository/MemberDailyActivityRepository.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/activity/domain/ActivityDate.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/activity/domain/ActivitySource.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/activity/domain/DailyActiveUserSummary.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/activity/domain/MemberDailyActivity.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/leaderboard/application/repository/LeaderboardProjectionRepository.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/leaderboard/application/result/LeaderboardEntryResult.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/leaderboard/application/result/LeaderboardMemberRankResult.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/leaderboard/application/result/LeaderboardResult.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/leaderboard/domain/LeaderboardEntry.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/leaderboard/domain/LeaderboardMode.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/leaderboard/domain/LeaderboardSnapshot.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketFundingScheduleLookup.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/market/application/repair/MarketHistoryRepairEvent.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/market/application/repair/MarketHistoryRepairEventRepository.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/market/application/repair/MarketHistoryRepairStatus.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/market/application/repository/MarketHistoryRepository.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/market/application/result/MarketCandleResult.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/market/application/result/MarketSummaryResult.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/market/domain/FundingSchedule.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/market/domain/HourlyMarketCandle.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/market/domain/MarketCandleInterval.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/market/domain/MarketHistoricalCandleSnapshot.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/market/domain/MarketHistoryCandle.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/market/domain/MarketMinuteCandleSnapshot.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/market/domain/MarketSnapshot.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/market/domain/MarketTime.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/member/application/repository/MemberCredentialRepository.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/member/application/repository/MemberPasswordHasher.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/member/application/result/MemberProfileResult.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/member/domain/MemberCredential.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/member/domain/MemberIdentityRules.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/member/domain/MemberRole.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/order/application/repository/OrderRepository.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/order/application/dto/CancelOrderResult.java` | application repository/dto/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/order/application/dto/CreateOrderResult.java` | application repository/dto/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/order/application/dto/ModifyOrderResult.java` | application repository/dto/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/order/application/dto/OpenOrderResult.java` | application repository/dto/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/order/application/dto/OrderHistoryResult.java` | application repository/dto/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/order/application/dto/PendingOrderCandidate.java` | application repository/dto/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/order/domain/FuturesOrder.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/order/domain/OrderPlacementDecision.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/order/domain/OrderPlacementPolicy.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/order/domain/OrderPlacementRequest.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/order/domain/OrderPreview.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/order/domain/OrderPreviewPolicy.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/position/application/repository/PositionHistoryRepository.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/position/application/repository/PositionRepository.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/position/application/result/ClosePositionResult.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/position/application/result/OpenPositionCandidate.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/position/application/result/PositionHistoryResult.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/position/application/result/PositionMutationResult.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/position/application/result/PositionSnapshotResult.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/position/domain/CrossLiquidationAssessment.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/position/domain/CrossLiquidationEstimate.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/position/domain/CrossPositionRisk.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/position/domain/IsolatedLiquidationAssessment.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/position/domain/LiquidationPolicy.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/position/domain/PositionAccounting.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/position/domain/PositionCloseOutcome.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/position/domain/PositionExposure.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/position/domain/PositionHistory.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/position/domain/PositionIdentity.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/position/domain/PositionSnapshot.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/reward/application/repository/RewardPointHistoryRepository.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/reward/application/repository/RewardPointRepository.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/reward/application/repository/RewardRedemptionRequestRepository.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/reward/application/repository/RewardShopItemRepository.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/reward/application/repository/RewardShopMemberItemUsageRepository.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/reward/application/result/AdminShopItemResult.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/reward/application/result/RewardPointHistoryResult.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/reward/application/result/RewardPointResult.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/reward/application/result/RewardRedemptionResult.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/reward/application/result/ShopItemResult.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/reward/application/result/ShopPurchaseResult.java` | application repository/result/contract without persistence framework imports |
| `src/main/java/coin/coinzzickmock/feature/reward/domain/RewardPhoneNumber.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/reward/domain/RewardPointHistory.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/reward/domain/RewardPointHistoryType.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/reward/domain/RewardPointPolicy.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/reward/domain/RewardPointWallet.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/reward/domain/RewardRedemptionRequest.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/reward/domain/RewardRedemptionStatus.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/reward/domain/RewardShopItem.java` | pure feature domain model/policy/value |
| `src/main/java/coin/coinzzickmock/feature/reward/domain/RewardShopMemberItemUsage.java` | pure feature domain model/policy/value |

## Must stay in storage

| File | Classification reason |
| --- | --- |
| `src/main/java/coin/coinzzickmock/common/persistence/AuditableEntity.java` | JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/account/infrastructure/persistence/AccountPersistenceRepository.java` | storage infrastructure adapter |
| `src/main/java/coin/coinzzickmock/feature/account/infrastructure/persistence/AccountRefillStateEntity.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/account/infrastructure/persistence/AccountRefillStateEntityRepository.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/account/infrastructure/persistence/AccountRefillStatePersistenceRepository.java` | storage infrastructure adapter |
| `src/main/java/coin/coinzzickmock/feature/account/infrastructure/persistence/TradingAccountEntity.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/account/infrastructure/persistence/TradingAccountEntityRepository.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/account/infrastructure/persistence/WalletHistoryEntity.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/account/infrastructure/persistence/WalletHistoryEntityRepository.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/account/infrastructure/persistence/WalletHistoryPersistenceRepository.java` | storage infrastructure adapter |
| `src/main/java/coin/coinzzickmock/feature/activity/infrastructure/persistence/DailyActiveUserSummaryEntity.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/activity/infrastructure/persistence/DailyActiveUserSummaryEntityRepository.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/activity/infrastructure/persistence/DailyActiveUserSummaryPersistenceRepository.java` | storage infrastructure adapter |
| `src/main/java/coin/coinzzickmock/feature/activity/infrastructure/persistence/MemberDailyActivityEntity.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/activity/infrastructure/persistence/MemberDailyActivityEntityRepository.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/activity/infrastructure/persistence/MemberDailyActivityPersistenceRepository.java` | storage infrastructure adapter |
| `src/main/java/coin/coinzzickmock/feature/leaderboard/infrastructure/persistence/LeaderboardProjectionPersistenceRepository.java` | storage infrastructure adapter |
| `src/main/java/coin/coinzzickmock/feature/market/infrastructure/persistence/MarketCandle1hEntity.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/market/infrastructure/persistence/MarketCandle1hEntityRepository.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/market/infrastructure/persistence/MarketCandle1mEntity.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/market/infrastructure/persistence/MarketCandle1mEntityRepository.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/market/infrastructure/persistence/MarketFundingSchedulePersistenceLookup.java` | storage infrastructure adapter |
| `src/main/java/coin/coinzzickmock/feature/market/infrastructure/persistence/MarketHistoryPersistenceRepository.java` | storage infrastructure adapter |
| `src/main/java/coin/coinzzickmock/feature/market/infrastructure/persistence/MarketHistoryRepairEventEntity.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/market/infrastructure/persistence/MarketHistoryRepairEventEntityRepository.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/market/infrastructure/persistence/MarketHistoryRepairPersistenceRepository.java` | storage infrastructure adapter |
| `src/main/java/coin/coinzzickmock/feature/market/infrastructure/persistence/MarketSymbolEntity.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/market/infrastructure/persistence/MarketSymbolEntityRepository.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/member/infrastructure/persistence/MemberCredentialEntity.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/member/infrastructure/persistence/MemberCredentialEntityRepository.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/member/infrastructure/persistence/MemberCredentialPersistenceRepository.java` | storage infrastructure adapter |
| `src/main/java/coin/coinzzickmock/feature/order/infrastructure/persistence/FuturesOrderEntity.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/order/infrastructure/persistence/FuturesOrderEntityRepository.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/order/infrastructure/persistence/OrderPersistenceRepository.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/position/infrastructure/persistence/OpenPositionEntity.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/position/infrastructure/persistence/OpenPositionEntityRepository.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/position/infrastructure/persistence/PositionHistoryEntity.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/position/infrastructure/persistence/PositionHistoryEntityRepository.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/position/infrastructure/persistence/PositionHistoryPersistenceRepository.java` | storage infrastructure adapter |
| `src/main/java/coin/coinzzickmock/feature/position/infrastructure/persistence/PositionPersistenceRepository.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/reward/infrastructure/persistence/RewardPointHistoryEntity.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/reward/infrastructure/persistence/RewardPointHistoryEntityRepository.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/reward/infrastructure/persistence/RewardPointHistoryPersistenceRepository.java` | storage infrastructure adapter |
| `src/main/java/coin/coinzzickmock/feature/reward/infrastructure/persistence/RewardPointPersistenceRepository.java` | storage infrastructure adapter |
| `src/main/java/coin/coinzzickmock/feature/reward/infrastructure/persistence/RewardPointWalletEntity.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/reward/infrastructure/persistence/RewardPointWalletEntityRepository.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/reward/infrastructure/persistence/RewardRedemptionRequestEntity.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/reward/infrastructure/persistence/RewardRedemptionRequestEntityRepository.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/reward/infrastructure/persistence/RewardRedemptionRequestPersistenceRepository.java` | storage infrastructure adapter |
| `src/main/java/coin/coinzzickmock/feature/reward/infrastructure/persistence/RewardShopItemEntity.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/reward/infrastructure/persistence/RewardShopItemEntityRepository.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/reward/infrastructure/persistence/RewardShopItemPersistenceRepository.java` | storage infrastructure adapter |
| `src/main/java/coin/coinzzickmock/feature/reward/infrastructure/persistence/RewardShopMemberItemUsageEntity.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/reward/infrastructure/persistence/RewardShopMemberItemUsageEntityRepository.java` | storage infrastructure adapter; JPA/QueryDSL/Spring Data/persistence API usage |
| `src/main/java/coin/coinzzickmock/feature/reward/infrastructure/persistence/RewardShopMemberItemUsagePersistenceRepository.java` | storage infrastructure adapter |

## Next movement rule

Move candidates in feature-sized slices only after compile and architecture lint baselines are green. When a slice moves candidates to `core`, remove or reduce the corresponding app/storage residue allowlist entries in the same change.
