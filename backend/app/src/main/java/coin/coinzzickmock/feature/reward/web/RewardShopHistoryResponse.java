package coin.coinzzickmock.feature.reward.web;

import coin.coinzzickmock.feature.reward.application.result.RewardShopHistoryKind;
import coin.coinzzickmock.feature.reward.application.result.RewardShopHistoryResult;
import coin.coinzzickmock.feature.reward.domain.RewardRedemptionStatus;
import java.time.Instant;

public record RewardShopHistoryResponse(
        RewardShopHistoryKind kind,
        String entryId,
        String itemCode,
        String itemName,
        String itemType,
        int pointAmount,
        int quantity,
        Instant eventAt,
        String submittedPhoneNumber,
        RewardRedemptionStatus status,
        Instant purchasedAt,
        Instant requestedAt,
        Instant sentAt,
        Instant cancelledAt
) {
    public static RewardShopHistoryResponse from(RewardShopHistoryResult result) {
        return new RewardShopHistoryResponse(
                result.kind(),
                result.entryId(),
                result.itemCode(),
                result.itemName(),
                result.itemType(),
                result.pointAmount(),
                result.quantity(),
                result.eventAt(),
                result.submittedPhoneNumber(),
                result.status(),
                result.purchasedAt(),
                result.requestedAt(),
                result.sentAt(),
                result.cancelledAt()
        );
    }
}
