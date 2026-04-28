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
    public RewardRedemptionResult cancel(String memberId, String requestId) {
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

    private CoreException mapOwnerClaimFailure(String memberId, String requestId) {
        return rewardRedemptionRequestRepository.findByRequestId(requestId)
                .map(request -> {
                    if (!request.memberId().equals(memberId)) {
                        return new CoreException(ErrorCode.FORBIDDEN);
                    }
                    return new CoreException(
                            ErrorCode.REWARD_REDEMPTION_CONFLICT,
                            "대기 중인 교환권 요청만 취소할 수 있습니다."
                    );
                })
                .orElseGet(() -> new CoreException(ErrorCode.REWARD_REDEMPTION_NOT_FOUND));
    }
}
