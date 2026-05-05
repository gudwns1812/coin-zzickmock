package coin.coinzzickmock.feature.reward.domain;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;

public record RewardPointWallet(
        Long memberId,
        int rewardPoint
) {
    public RewardPointWallet {
        if (memberId == null) {
            throw invalid();
        }
        if (rewardPoint < 0) {
            throw invalid();
        }
    }

    public static RewardPointWallet empty(Long memberId) {
        return new RewardPointWallet(memberId, 0);
    }

    public RewardPointWallet grant(int grantedPoint) {
        return new RewardPointWallet(memberId, addPositivePoints(grantedPoint, "적립 포인트"));
    }

    public RewardPointWallet deduct(int pointAmount) {
        if (pointAmount <= 0) {
            throw invalid();
        }
        if (rewardPoint < pointAmount) {
            throw invalid();
        }
        return new RewardPointWallet(memberId, rewardPoint - pointAmount);
    }

    public RewardPointWallet refund(int pointAmount) {
        if (pointAmount <= 0) {
            throw invalid();
        }
        return new RewardPointWallet(memberId, addPositivePoints(pointAmount, "환급 포인트"));
    }

    private int addPositivePoints(int pointAmount, String label) {
        if (pointAmount <= 0) {
            throw invalid();
        }
        if (rewardPoint > Integer.MAX_VALUE - pointAmount) {
            throw invalid();
        }
        return rewardPoint + pointAmount;
    }

    private static CoreException invalid() {
        return new CoreException(ErrorCode.INVALID_REQUEST);
    }
}
