package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface RewardShopMemberItemUsageEntityRepository
        extends JpaRepository<RewardShopMemberItemUsageEntity, Long> {
    Optional<RewardShopMemberItemUsageEntity> findByMemberIdAndShopItem_Id(String memberId, Long shopItemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RewardShopMemberItemUsageEntity> findWithLockingByMemberIdAndShopItem_Id(String memberId, Long shopItemId);
}
