package coin.coinzzickmock.feature.reward.domain;

public class RewardPointPolicy {
    private static final double MINIMUM_REWARDABLE_PROFIT = 10_000;
    private static final int NO_POINT = 0;
    private static final int POINTS_PER_PROFIT_UNIT = 5;

    public int pointsFor(double realizedProfit) {
        if (realizedProfit < MINIMUM_REWARDABLE_PROFIT) {
            return NO_POINT;
        }
        return (int) Math.floor(realizedProfit / MINIMUM_REWARDABLE_PROFIT) * POINTS_PER_PROFIT_UNIT;
    }

    public String tierLabel(int grantedPoint) {
        if (grantedPoint == NO_POINT) {
            return "NONE";
        }
        return "REALIZED_PROFIT_POINT";
    }
}
