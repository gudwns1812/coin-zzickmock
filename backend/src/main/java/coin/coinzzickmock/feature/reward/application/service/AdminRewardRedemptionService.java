package coin.coinzzickmock.feature.reward.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.reward.application.refund.RewardRedemptionRefundProcessor;
import coin.coinzzickmock.feature.reward.application.repository.RewardRedemptionRequestRepository;
import coin.coinzzickmock.feature.reward.application.result.RewardRedemptionResult;
import coin.coinzzickmock.feature.reward.domain.RewardRedemptionRequest;
import coin.coinzzickmock.feature.reward.domain.RewardRedemptionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminRewardRedemptionService {
    private final RewardRedemptionRequestRepository rewardRedemptionRequestRepository;
    private final RewardRedemptionRefundProcessor rewardRedemptionRefundProcessor;

    @Transactional(readOnly = true)
    public List<RewardRedemptionResult> list(RewardRedemptionStatus status) {
        return rewardRedemptionRequestRepository.findByStatus(status).stream()
                .map(RewardRedemptionResult::from)
                .toList();
    }

    @Transactional
    public RewardRedemptionResult markSent(String requestId, Long adminMemberId, String adminMemo) {
        return approve(requestId, adminMemberId, adminMemo);
    }

    @Transactional
    public RewardRedemptionResult approve(String requestId, Long adminMemberId, String adminMemo) {
        int claimed = rewardRedemptionRequestRepository.claimPendingAsApproved(
                requestId,
                adminMemberId,
                adminMemo,
                Instant.now()
        );
        if (claimed == 0) {
            throw mapAdminClaimFailure(requestId);
        }
        return RewardRedemptionResult.from(findRequestForUpdate(requestId));
    }

    @Transactional
    public RewardRedemptionResult cancelAndRefund(String requestId, Long adminMemberId, String adminMemo) {
        return rejectAndRefund(requestId, adminMemberId, adminMemo);
    }

    @Transactional
    public RewardRedemptionResult rejectAndRefund(String requestId, Long adminMemberId, String adminMemo) {
        int claimed = rewardRedemptionRequestRepository.claimPendingAsRejected(
                requestId,
                adminMemberId,
                adminMemo,
                Instant.now()
        );
        if (claimed == 0) {
            throw mapAdminClaimFailure(requestId);
        }
        RewardRedemptionRequest request = findRequestForUpdate(requestId);
        rewardRedemptionRefundProcessor.refundReservation(request);
        return RewardRedemptionResult.from(request);
    }

    private RewardRedemptionRequest findRequestForUpdate(String requestId) {
        return rewardRedemptionRequestRepository.findByRequestIdForUpdate(requestId)
                .orElseThrow(() -> new CoreException(ErrorCode.REWARD_REDEMPTION_NOT_FOUND));
    }

    private CoreException mapAdminClaimFailure(String requestId) {
        return rewardRedemptionRequestRepository.findByRequestId(requestId)
                .map(request -> new CoreException(
                        ErrorCode.REWARD_REDEMPTION_CONFLICT,
                        "대기 중인 교환권 요청만 처리할 수 있습니다."
                ))
                .orElseGet(() -> new CoreException(ErrorCode.REWARD_REDEMPTION_NOT_FOUND));
    }

}
