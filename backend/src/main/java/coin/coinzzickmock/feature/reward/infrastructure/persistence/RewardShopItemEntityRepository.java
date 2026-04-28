package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RewardShopItemEntityRepository extends JpaRepository<RewardShopItemEntity, Long> {
    List<RewardShopItemEntity> findByActiveTrueOrderBySortOrderAscCodeAsc();

    Optional<RewardShopItemEntity> findByCode(String code);
}
