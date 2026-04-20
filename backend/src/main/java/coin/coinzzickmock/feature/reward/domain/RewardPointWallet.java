package coin.coinzzickmock.feature.reward.domain;

public record RewardPointWallet(
        String memberId,
        double rewardPoint
) {
    public static RewardPointWallet empty(String memberId) {
        return new RewardPointWallet(memberId, 0);
    }

    public RewardPointWallet grant(int grantedPoint) {
        return new RewardPointWallet(memberId, rewardPoint + grantedPoint);
    }
}
