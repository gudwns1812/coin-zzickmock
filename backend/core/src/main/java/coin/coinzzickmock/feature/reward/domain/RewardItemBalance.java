package coin.coinzzickmock.feature.reward.domain;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;

public record RewardItemBalance(
        Long id,
        Long memberId,
        Long shopItemId,
        int remainingQuantity
) {
    public static RewardItemBalance empty(Long memberId, Long shopItemId) {
        return new RewardItemBalance(null, memberId, shopItemId, 0);
    }

    public RewardItemBalance {
        if (memberId == null || shopItemId == null || remainingQuantity < 0) {
            throw invalid();
        }
    }

    public RewardItemBalance addOne() {
        if (remainingQuantity == Integer.MAX_VALUE) {
            throw invalid();
        }
        return new RewardItemBalance(id, memberId, shopItemId, remainingQuantity + 1);
    }

    public RewardItemBalance consumeOne() {
        if (remainingQuantity <= 0) {
            throw invalid();
        }
        return new RewardItemBalance(id, memberId, shopItemId, remainingQuantity - 1);
    }

    private static CoreException invalid() {
        return new CoreException(ErrorCode.INVALID_REQUEST);
    }
}
