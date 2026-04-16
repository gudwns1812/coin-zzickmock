package coin.coinzzickmock.feature.reward.domain;

public class RewardPointPolicy {
    private static final double SMALL_PROFIT_THRESHOLD = 100;
    private static final double STEADY_PROFIT_THRESHOLD = 500;
    private static final double STRONG_PROFIT_THRESHOLD = 1_000;
    private static final int NO_POINT = 0;
    private static final int SMALL_PROFIT_POINT = 1;
    private static final int STEADY_PROFIT_POINT = 5;
    private static final int STRONG_PROFIT_POINT = 20;
    private static final int HIGH_PROFIT_POINT = 50;

    public int pointsFor(double realizedProfit) {
        if (realizedProfit <= 0) {
            return NO_POINT;
        }
        if (realizedProfit < SMALL_PROFIT_THRESHOLD) {
            return SMALL_PROFIT_POINT;
        }
        if (realizedProfit < STEADY_PROFIT_THRESHOLD) {
            return STEADY_PROFIT_POINT;
        }
        if (realizedProfit < STRONG_PROFIT_THRESHOLD) {
            return STRONG_PROFIT_POINT;
        }
        return HIGH_PROFIT_POINT;
    }

    public String tierLabel(int grantedPoint) {
        return switch (grantedPoint) {
            case NO_POINT -> "NONE";
            case SMALL_PROFIT_POINT -> "SMALL_PROFIT";
            case STEADY_PROFIT_POINT -> "STEADY_PROFIT";
            case STRONG_PROFIT_POINT -> "STRONG_PROFIT";
            default -> "HIGH_PROFIT";
        };
    }
}
