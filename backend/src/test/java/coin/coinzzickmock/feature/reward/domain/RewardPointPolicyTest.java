package coin.coinzzickmock.feature.reward.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RewardPointPolicyTest {
    private final RewardPointPolicy rewardPointPolicy = new RewardPointPolicy();

    @Test
    void grantsNoPointsForZeroProfit() {
        assertEquals(0, rewardPointPolicy.pointsFor(0));
        assertEquals(0, rewardPointPolicy.pointsFor(-10));
    }

    @Test
    void grantsPointsByProfitTier() {
        assertEquals(1, rewardPointPolicy.pointsFor(99.99));
        assertEquals(5, rewardPointPolicy.pointsFor(100));
        assertEquals(20, rewardPointPolicy.pointsFor(500));
        assertEquals(50, rewardPointPolicy.pointsFor(1_000));
    }
}
