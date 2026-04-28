package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface RewardRedemptionRequestEntityRepository extends JpaRepository<RewardRedemptionRequestEntity, Long> {
    Optional<RewardRedemptionRequestEntity> findByRequestId(String requestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RewardRedemptionRequestEntity> findWithLockingByRequestId(String requestId);

    List<RewardRedemptionRequestEntity> findByStatusOrderByRequestedAtDesc(String status);
}
