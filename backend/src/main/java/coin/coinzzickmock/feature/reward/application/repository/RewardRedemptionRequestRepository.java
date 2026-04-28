package coin.coinzzickmock.feature.reward.application.repository;

import coin.coinzzickmock.feature.reward.domain.RewardRedemptionRequest;
import coin.coinzzickmock.feature.reward.domain.RewardRedemptionStatus;

import java.util.List;
import java.util.Optional;

public interface RewardRedemptionRequestRepository {
    RewardRedemptionRequest save(RewardRedemptionRequest request);

    Optional<RewardRedemptionRequest> findByRequestIdForUpdate(String requestId);

    List<RewardRedemptionRequest> findByStatus(RewardRedemptionStatus status);
}
