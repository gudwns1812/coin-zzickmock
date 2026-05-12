package coin.coinzzickmock.feature.reward.domain;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;

public record RewardPointHistory(
        Long memberId,
        RewardPointHistoryType historyType,
        int amount,
        int balanceAfter,
        String sourceType,
        String sourceReference
) {
    public RewardPointHistory {
        if (memberId == null) {
            throw invalid();
        }
        if (historyType == null) {
            throw invalid();
        }
        if (amount == 0) {
            throw invalid();
        }
        if (balanceAfter < 0) {
            throw invalid();
        }
    }

    public static RewardPointHistory grant(Long memberId, int amount, int balanceAfter, String sourceReference) {
        if (amount <= 0) {
            throw invalid();
        }
        return new RewardPointHistory(
                memberId,
                RewardPointHistoryType.GRANT,
                amount,
                balanceAfter,
                "POSITION_CLOSE_PROFIT",
                sourceReference
        );
    }

    public static RewardPointHistory redemptionDeduct(Long memberId, int amount, int balanceAfter, String requestId) {
        if (amount <= 0) {
            throw invalid();
        }
        return new RewardPointHistory(
                memberId,
                RewardPointHistoryType.REDEMPTION_DEDUCT,
                -amount,
                balanceAfter,
                "REDEMPTION_REQUEST",
                requestId
        );
    }

    public static RewardPointHistory instantShopPurchaseDeduct(
            Long memberId,
            int amount,
            int balanceAfter,
            String purchaseId
    ) {
        if (amount <= 0) {
            throw invalid();
        }
        return new RewardPointHistory(
                memberId,
                RewardPointHistoryType.REDEMPTION_DEDUCT,
                -amount,
                balanceAfter,
                "INSTANT_SHOP_PURCHASE",
                purchaseId
        );
    }

    public static RewardPointHistory redemptionRefund(Long memberId, int amount, int balanceAfter, String requestId) {
        if (amount <= 0) {
            throw invalid();
        }
        return new RewardPointHistory(
                memberId,
                RewardPointHistoryType.REDEMPTION_REFUND,
                amount,
                balanceAfter,
                "REDEMPTION_REQUEST",
                requestId
        );
    }

    private static CoreException invalid() {
        return new CoreException(ErrorCode.INVALID_REQUEST);
    }
}
