package coin.coinzzickmock.feature.market.application.realtime;

public enum MarketPriceMovementDirection {
    UP,
    DOWN,
    UNCHANGED;

    public static MarketPriceMovementDirection between(Double previousPrice, double currentPrice) {
        if (previousPrice == null || Double.compare(previousPrice, currentPrice) == 0) {
            return UNCHANGED;
        }
        return currentPrice > previousPrice ? UP : DOWN;
    }
}
