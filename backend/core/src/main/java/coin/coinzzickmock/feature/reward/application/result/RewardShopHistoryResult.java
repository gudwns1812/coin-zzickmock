package coin.coinzzickmock.feature.reward.application.result;

import coin.coinzzickmock.feature.reward.domain.RewardRedemptionStatus;
import java.time.Instant;

public record RewardShopHistoryResult(
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
}
