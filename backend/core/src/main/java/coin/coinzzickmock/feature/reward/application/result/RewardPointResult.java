package coin.coinzzickmock.feature.reward.application.result;

import coin.coinzzickmock.feature.reward.domain.RewardPointPolicy;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;

public record RewardPointResult(
        int rewardPoint,
        String tierLabel
) {
    public static RewardPointResult wallet(RewardPointWallet wallet) {
        return new RewardPointResult(wallet.rewardPoint(), "POINT_WALLET");
    }

    public static RewardPointResult fromGrant(
            RewardPointWallet wallet,
            RewardPointPolicy rewardPointPolicy,
            int grantedPoint
    ) {
        return new RewardPointResult(wallet.rewardPoint(), rewardPointPolicy.tierLabel(grantedPoint));
    }
}
