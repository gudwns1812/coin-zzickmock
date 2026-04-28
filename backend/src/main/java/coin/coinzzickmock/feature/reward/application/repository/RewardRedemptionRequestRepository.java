package coin.coinzzickmock.feature.reward.application.repository;

import coin.coinzzickmock.feature.reward.domain.RewardRedemptionRequest;
import coin.coinzzickmock.feature.reward.domain.RewardRedemptionStatus;

import java.util.List;
import java.util.Optional;

public interface RewardRedemptionRequestRepository {
    RewardRedemptionRequest save(RewardRedemptionRequest request);

    Optional<RewardRedemptionRequest> findByRequestId(String requestId);

    Optional<RewardRedemptionRequest> findByRequestIdForUpdate(String requestId);

    List<RewardRedemptionRequest> findByMemberId(String memberId);

    List<RewardRedemptionRequest> findByStatus(RewardRedemptionStatus status);

    int claimPendingAsApproved(String requestId, String adminMemberId, String adminMemo, java.time.Instant approvedAt);

    int claimPendingAsRejected(String requestId, String adminMemberId, String adminMemo, java.time.Instant rejectedAt);

    int claimPendingAsCancelled(String requestId, String memberId, java.time.Instant cancelledAt);
}
