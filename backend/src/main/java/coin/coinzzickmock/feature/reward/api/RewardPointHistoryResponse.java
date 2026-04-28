package coin.coinzzickmock.feature.reward.api;

import coin.coinzzickmock.feature.reward.application.result.RewardPointHistoryResult;
import coin.coinzzickmock.feature.reward.domain.RewardPointHistoryType;

public record RewardPointHistoryResponse(
        RewardPointHistoryType historyType,
        int amount,
        int balanceAfter,
        String sourceType,
        String sourceReference
) {
    public static RewardPointHistoryResponse from(RewardPointHistoryResult result) {
        return new RewardPointHistoryResponse(
                result.historyType(),
                result.amount(),
                result.balanceAfter(),
                result.sourceType(),
                result.sourceReference()
        );
    }
}
