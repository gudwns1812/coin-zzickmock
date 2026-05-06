package coin.coinzzickmock.feature.reward.domain;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;

public record RewardShopMemberItemUsage(
        Long id,
        Long memberId,
        Long shopItemId,
        int purchaseCount
) {
    public static RewardShopMemberItemUsage empty(Long memberId, Long shopItemId) {
        return new RewardShopMemberItemUsage(null, memberId, shopItemId, 0);
    }

    public RewardShopMemberItemUsage {
        if (memberId == null) {
            throw invalid();
        }
        if (shopItemId == null) {
            throw invalid();
        }
        if (purchaseCount < 0) {
            throw invalid();
        }
    }

    public RewardShopMemberItemUsage increment() {
        if (purchaseCount == Integer.MAX_VALUE) {
            throw invalid();
        }
        return new RewardShopMemberItemUsage(id, memberId, shopItemId, purchaseCount + 1);
    }

    public RewardShopMemberItemUsage decrement() {
        if (purchaseCount == 0) {
            throw invalid();
        }
        return new RewardShopMemberItemUsage(id, memberId, shopItemId, purchaseCount - 1);
    }

    private static CoreException invalid() {
        return new CoreException(ErrorCode.INVALID_REQUEST);
    }
}
