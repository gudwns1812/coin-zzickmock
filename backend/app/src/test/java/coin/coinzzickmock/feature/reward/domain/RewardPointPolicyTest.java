package coin.coinzzickmock.feature.reward.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RewardPointPolicyTest {
    private final RewardPointPolicy rewardPointPolicy = new RewardPointPolicy();

    @Test
    void grantsNoPointsBelowMinimumRewardableProfit() {
        assertEquals(0, rewardPointPolicy.pointsFor(-1));
        assertEquals(0, rewardPointPolicy.pointsFor(0));
        assertEquals(0, rewardPointPolicy.pointsFor(9_999.99));
    }

    @Test
    void grantsFivePointsForEachFullTenThousandPositiveRealizedProfit() {
        assertEquals(5, rewardPointPolicy.pointsFor(10_000));
        assertEquals(5, rewardPointPolicy.pointsFor(19_999.99));
        assertEquals(10, rewardPointPolicy.pointsFor(20_000));
        assertEquals(15, rewardPointPolicy.pointsFor(30_000));
    }

    @Test
    void labelsGrantedPointsWithoutMisleadingProfitTiers() {
        assertEquals("NONE", rewardPointPolicy.tierLabel(0));
        assertEquals("REALIZED_PROFIT_POINT", rewardPointPolicy.tierLabel(5));
        assertEquals("REALIZED_PROFIT_POINT", rewardPointPolicy.tierLabel(10));
    }
}
