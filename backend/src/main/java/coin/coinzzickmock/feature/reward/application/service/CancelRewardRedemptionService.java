package coin.coinzzickmock.feature.reward.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.reward.application.refund.RewardRedemptionRefundProcessor;
import coin.coinzzickmock.feature.reward.application.repository.RewardRedemptionRequestRepository;
import coin.coinzzickmock.feature.reward.application.result.RewardRedemptionResult;
import coin.coinzzickmock.feature.reward.domain.RewardRedemptionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CancelRewardRedemptionService {
    private final RewardRedemptionRequestRepository rewardRedemptionRequestRepository;
    private final RewardRedemptionRefundProcessor rewardRedemptionRefundProcessor;

    @Transactional
    public RewardRedemptionResult cancel(Long memberId, String requestId) {
        int claimed = rewardRedemptionRequestRepository.claimPendingAsCancelled(
                requestId,
                memberId,
                Instant.now()
        );
        if (claimed == 0) {
            throw mapOwnerClaimFailure(memberId, requestId);
        }
        RewardRedemptionRequest request = rewardRedemptionRequestRepository.findByRequestIdForUpdate(requestId)
                .orElseThrow(() -> new CoreException(ErrorCode.REWARD_REDEMPTION_NOT_FOUND));
        rewardRedemptionRefundProcessor.refundReservation(request);
        return RewardRedemptionResult.from(request);
    }

    private CoreException mapOwnerClaimFailure(Long memberId, String requestId) {
        return rewardRedemptionRequestRepository.findByRequestId(requestId)
                .map(request -> {
                    if (!request.memberId().equals(memberId)) {
                        return new CoreException(ErrorCode.FORBIDDEN);
                    }
                    return new CoreException(ErrorCode.REWARD_REDEMPTION_CONFLICT);
                })
                .orElseGet(() -> new CoreException(ErrorCode.REWARD_REDEMPTION_NOT_FOUND));
    }
}
