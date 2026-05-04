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
            throw invalid("회원 ID는 필수입니다.");
        }
        if (shopItemId == null) {
            throw invalid("상점 상품 ID는 필수입니다.");
        }
        if (purchaseCount < 0) {
            throw invalid("구매 수량은 음수일 수 없습니다.");
        }
    }

    public RewardShopMemberItemUsage increment() {
        if (purchaseCount == Integer.MAX_VALUE) {
            throw invalid("구매 수량이 허용 범위를 초과합니다.");
        }
        return new RewardShopMemberItemUsage(id, memberId, shopItemId, purchaseCount + 1);
    }

    public RewardShopMemberItemUsage decrement() {
        if (purchaseCount == 0) {
            throw invalid("구매 수량은 음수로 복구할 수 없습니다.");
        }
        return new RewardShopMemberItemUsage(id, memberId, shopItemId, purchaseCount - 1);
    }

    private static CoreException invalid(String message) {
        return new CoreException(ErrorCode.INVALID_REQUEST, message);
    }
}
