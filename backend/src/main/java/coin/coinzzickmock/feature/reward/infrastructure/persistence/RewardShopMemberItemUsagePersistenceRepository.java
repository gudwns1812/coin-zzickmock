package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import coin.coinzzickmock.feature.reward.application.repository.RewardShopMemberItemUsageRepository;
import coin.coinzzickmock.feature.reward.domain.RewardShopMemberItemUsage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RewardShopMemberItemUsagePersistenceRepository implements RewardShopMemberItemUsageRepository {
    private final RewardShopMemberItemUsageEntityRepository usageEntityRepository;
    private final RewardShopItemEntityRepository shopItemEntityRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<RewardShopMemberItemUsage> findByMemberIdAndShopItemId(String memberId, Long shopItemId) {
        return usageEntityRepository.findByMemberIdAndShopItem_Id(memberId, shopItemId)
                .map(RewardShopMemberItemUsageEntity::toDomain);
    }

    @Override
    @Transactional
    public Optional<RewardShopMemberItemUsage> findByMemberIdAndShopItemIdForUpdate(String memberId, Long shopItemId) {
        return usageEntityRepository.findWithLockingByMemberIdAndShopItem_Id(memberId, shopItemId)
                .map(RewardShopMemberItemUsageEntity::toDomain);
    }

    @Override
    @Transactional
    public RewardShopMemberItemUsage save(RewardShopMemberItemUsage usage) {
        RewardShopItemEntity shopItem = shopItemEntityRepository.findById(usage.shopItemId()).orElseThrow();
        RewardShopMemberItemUsageEntity entity = usage.id() == null
                ? RewardShopMemberItemUsageEntity.from(usage, shopItem)
                : usageEntityRepository.findById(usage.id())
                .map(existing -> {
                    existing.apply(usage, shopItem);
                    return existing;
                })
                .orElseGet(() -> RewardShopMemberItemUsageEntity.from(usage, shopItem));
        return usageEntityRepository.save(entity).toDomain();
    }
}
