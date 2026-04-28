package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RewardRedemptionRequestEntityRepository extends JpaRepository<RewardRedemptionRequestEntity, Long> {
    Optional<RewardRedemptionRequestEntity> findByRequestId(String requestId);
}
