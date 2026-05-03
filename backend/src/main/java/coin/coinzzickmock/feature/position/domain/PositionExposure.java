package coin.coinzzickmock.feature.position.domain;

public record PositionExposure(
        int leverage,
        double quantity,
        double entryPrice,
        double markPrice,
        Double liquidationPrice,
        double unrealizedPnl
) {
    public double notional(double targetMarkPrice) {
        return targetMarkPrice * quantity;
    }

    public double initialMargin() {
        return (entryPrice * quantity) / leverage;
    }

    public double roi() {
        double margin = initialMargin();
        if (margin == 0) {
            return 0;
        }
        return unrealizedPnl / margin;
    }
}
