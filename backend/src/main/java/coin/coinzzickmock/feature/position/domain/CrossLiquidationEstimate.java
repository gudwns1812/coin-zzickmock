package coin.coinzzickmock.feature.position.domain;

public record CrossLiquidationEstimate(
        Double liquidationPrice,
        String liquidationPriceType
) {
    public static final String TYPE_EXACT = "EXACT";
    public static final String TYPE_ESTIMATED = "ESTIMATED";
    public static final String TYPE_UNAVAILABLE = "UNAVAILABLE";

    public static CrossLiquidationEstimate exact(double liquidationPrice) {
        return new CrossLiquidationEstimate(liquidationPrice, TYPE_EXACT);
    }

    public static CrossLiquidationEstimate estimated(double liquidationPrice) {
        return new CrossLiquidationEstimate(liquidationPrice, TYPE_ESTIMATED);
    }

    public static CrossLiquidationEstimate unavailable() {
        return new CrossLiquidationEstimate(null, TYPE_UNAVAILABLE);
    }
}
