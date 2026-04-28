package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import coin.coinzzickmock.feature.reward.application.repository.RewardShopItemRepository;
import coin.coinzzickmock.feature.reward.domain.RewardShopItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RewardShopItemPersistenceRepository implements RewardShopItemRepository {
    private final RewardShopItemEntityRepository rewardShopItemEntityRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RewardShopItem> findAllItems() {
        return rewardShopItemEntityRepository.findAllByOrderBySortOrderAscCodeAsc().stream()
                .map(RewardShopItemEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RewardShopItem> findActiveItems() {
        return rewardShopItemEntityRepository.findByActiveTrueOrderBySortOrderAscCodeAsc().stream()
                .map(RewardShopItemEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RewardShopItem> findByCode(String code) {
        return rewardShopItemEntityRepository.findByCode(code)
                .map(RewardShopItemEntity::toDomain);
    }

    @Override
    @Transactional
    public Optional<RewardShopItem> findByCodeForUpdate(String code) {
        return rewardShopItemEntityRepository.findWithLockingByCode(code)
                .map(RewardShopItemEntity::toDomain);
    }

    @Override
    @Transactional
    public Optional<RewardShopItem> findByIdForUpdate(Long id) {
        return rewardShopItemEntityRepository.findWithLockingById(id)
                .map(RewardShopItemEntity::toDomain);
    }

    @Override
    @Transactional
    public RewardShopItem save(RewardShopItem item) {
        RewardShopItemEntity entity = item.id() == null
                ? RewardShopItemEntity.fromDomain(item)
                : rewardShopItemEntityRepository.findById(item.id())
                .map(existing -> {
                    existing.apply(item);
                    return existing;
                })
                .orElseGet(() -> RewardShopItemEntity.fromDomain(item));
        return rewardShopItemEntityRepository.save(entity).toDomain();
    }
}
