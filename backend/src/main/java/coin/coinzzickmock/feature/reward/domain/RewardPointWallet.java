package coin.coinzzickmock.feature.reward.domain;

public record RewardPointWallet(
        Long memberId,
        int rewardPoint
) {
    public RewardPointWallet {
        if (memberId == null) {
            throw new IllegalArgumentException("회원 ID는 필수입니다.");
        }
        if (rewardPoint < 0) {
            throw new IllegalArgumentException("포인트 잔액은 음수일 수 없습니다.");
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
            throw new IllegalArgumentException("차감 포인트는 0보다 커야 합니다.");
        }
        if (rewardPoint < pointAmount) {
            throw new IllegalArgumentException("포인트 잔액이 부족합니다.");
        }
        return new RewardPointWallet(memberId, rewardPoint - pointAmount);
    }

    public RewardPointWallet refund(int pointAmount) {
        if (pointAmount <= 0) {
            throw new IllegalArgumentException("환급 포인트는 0보다 커야 합니다.");
        }
        return new RewardPointWallet(memberId, addPositivePoints(pointAmount, "환급 포인트"));
    }

    private int addPositivePoints(int pointAmount, String label) {
        if (pointAmount <= 0) {
            throw new IllegalArgumentException(label + "는 0보다 커야 합니다.");
        }
        if (rewardPoint > Integer.MAX_VALUE - pointAmount) {
            throw new IllegalArgumentException("포인트 잔액이 허용 범위를 초과합니다.");
        }
        return rewardPoint + pointAmount;
    }
}
