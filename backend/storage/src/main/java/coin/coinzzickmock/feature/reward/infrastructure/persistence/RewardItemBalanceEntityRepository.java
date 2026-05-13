package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface RewardItemBalanceEntityRepository extends JpaRepository<RewardItemBalanceEntity, Long> {
    Optional<RewardItemBalanceEntity> findByMemberIdAndShopItem_Id(Long memberId, Long shopItemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RewardItemBalanceEntity> findWithLockingByMemberIdAndShopItem_Id(Long memberId, Long shopItemId);
}
