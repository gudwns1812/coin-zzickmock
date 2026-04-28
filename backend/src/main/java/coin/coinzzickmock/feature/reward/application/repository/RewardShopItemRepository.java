package coin.coinzzickmock.feature.reward.application.repository;

import coin.coinzzickmock.feature.reward.domain.RewardShopItem;

import java.util.List;
import java.util.Optional;

public interface RewardShopItemRepository {
    List<RewardShopItem> findAllItems();

    List<RewardShopItem> findActiveItems();

    Optional<RewardShopItem> findByCode(String code);

    Optional<RewardShopItem> findByCodeForUpdate(String code);

    Optional<RewardShopItem> findByIdForUpdate(Long id);

    RewardShopItem save(RewardShopItem item);
}
