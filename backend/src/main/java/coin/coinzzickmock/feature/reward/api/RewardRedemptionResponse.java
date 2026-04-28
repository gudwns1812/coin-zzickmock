package coin.coinzzickmock.feature.reward.api;

import coin.coinzzickmock.feature.reward.application.result.RewardRedemptionResult;
import coin.coinzzickmock.feature.reward.domain.RewardRedemptionStatus;

import java.time.Instant;

public record RewardRedemptionResponse(
        String requestId,
        String memberId,
        String itemCode,
        String itemName,
        int pointAmount,
        String submittedPhoneNumber,
        RewardRedemptionStatus status,
        Instant requestedAt,
        Instant sentAt,
        Instant cancelledAt,
        String adminMemberId,
        String adminMemo
) {
    public static RewardRedemptionResponse from(RewardRedemptionResult result) {
        return new RewardRedemptionResponse(
                result.requestId(),
                result.memberId(),
                result.itemCode(),
                result.itemName(),
                result.pointAmount(),
                result.submittedPhoneNumber(),
                result.status(),
                result.requestedAt(),
                result.sentAt(),
                result.cancelledAt(),
                result.adminMemberId(),
                result.adminMemo()
        );
    }
}
