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
}
