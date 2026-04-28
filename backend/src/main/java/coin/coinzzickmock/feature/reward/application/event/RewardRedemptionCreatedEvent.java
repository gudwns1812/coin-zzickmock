package coin.coinzzickmock.feature.reward.application.event;

import coin.coinzzickmock.feature.reward.domain.RewardRedemptionRequest;

public record RewardRedemptionCreatedEvent(
        String requestId,
        String memberId,
        String itemCode,
        String itemName,
        int pointAmount,
        String submittedPhoneNumber
) {
    public static RewardRedemptionCreatedEvent from(RewardRedemptionRequest request) {
        return new RewardRedemptionCreatedEvent(
                request.requestId(),
                request.memberId(),
                request.itemCode(),
                request.itemName(),
                request.pointAmount(),
                request.submittedPhoneNumber()
        );
    }
}
