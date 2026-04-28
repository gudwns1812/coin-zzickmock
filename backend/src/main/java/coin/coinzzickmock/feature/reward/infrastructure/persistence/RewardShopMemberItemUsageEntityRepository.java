package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RewardShopMemberItemUsageEntityRepository
        extends JpaRepository<RewardShopMemberItemUsageEntity, Long> {
    Optional<RewardShopMemberItemUsageEntity> findByMemberIdAndShopItem_Id(String memberId, Long shopItemId);
}
