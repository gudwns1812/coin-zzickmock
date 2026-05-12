package coin.coinzzickmock.feature.reward.application.result;

import coin.coinzzickmock.feature.reward.domain.RewardRedemptionRequest;
import coin.coinzzickmock.feature.reward.domain.RewardRedemptionStatus;

import java.time.Instant;

public record RewardRedemptionResult(
        String requestId,
        Long memberId,
        String itemCode,
        String itemName,
        int pointAmount,
        String submittedPhoneNumber,
        RewardRedemptionStatus status,
        Instant requestedAt,
        Instant sentAt,
        Instant cancelledAt,
        Long adminMemberId,
        String adminMemo
) {
    public static RewardRedemptionResult from(RewardRedemptionRequest request) {
        return new RewardRedemptionResult(
                request.requestId(),
                request.memberId(),
                request.itemCode(),
                request.itemName(),
                request.pointAmount(),
                request.submittedPhoneNumber(),
                request.status(),
                request.requestedAt(),
                request.sentAt(),
                request.cancelledAt(),
                request.adminMemberId(),
                request.adminMemo()
        );
    }
}
