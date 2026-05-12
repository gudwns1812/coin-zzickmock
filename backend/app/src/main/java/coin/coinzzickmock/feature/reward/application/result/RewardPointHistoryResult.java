package coin.coinzzickmock.feature.reward.application.result;

import coin.coinzzickmock.feature.reward.domain.RewardPointHistory;
import coin.coinzzickmock.feature.reward.domain.RewardPointHistoryType;

public record RewardPointHistoryResult(
        RewardPointHistoryType historyType,
        int amount,
        int balanceAfter,
        String sourceType,
        String sourceReference
) {
    public static RewardPointHistoryResult from(RewardPointHistory history) {
        return new RewardPointHistoryResult(
                history.historyType(),
                history.amount(),
                history.balanceAfter(),
                history.sourceType(),
                history.sourceReference()
        );
    }
}
