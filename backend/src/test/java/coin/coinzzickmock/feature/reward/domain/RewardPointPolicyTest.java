package coin.coinzzickmock.feature.reward.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RewardPointPolicyTest {
    @Test
    void grantsNoPointsForZeroProfit() {
        assertEquals(0, pointsFor(0));
        assertEquals(0, pointsFor(-10));
    }

    @Test
    void grantsPointsByProfitTier() {
        assertEquals(1, pointsFor(99.99));
        assertEquals(5, pointsFor(100));
        assertEquals(20, pointsFor(500));
        assertEquals(50, pointsFor(1_000));
    }

    private int pointsFor(double realizedProfit) {
        if (realizedProfit <= 0) {
            return 0;
        }
        if (realizedProfit < 100) {
            return 1;
        }
        if (realizedProfit < 500) {
            return 5;
        }
        if (realizedProfit < 1_000) {
            return 20;
        }
        return 50;
    }
}
