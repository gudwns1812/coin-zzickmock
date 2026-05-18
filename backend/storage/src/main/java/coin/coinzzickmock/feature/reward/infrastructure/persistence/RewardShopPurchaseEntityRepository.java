package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RewardShopPurchaseEntityRepository extends JpaRepository<RewardShopPurchaseEntity, Long> {
    Optional<RewardShopPurchaseEntity> findByPurchaseId(String purchaseId);
}
