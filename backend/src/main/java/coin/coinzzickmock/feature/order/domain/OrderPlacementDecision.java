package coin.coinzzickmock.feature.order.domain;

public record OrderPlacementDecision(
        boolean executable,
        String feeType,
        double feeRate,
        double executionPrice,
        double estimatePrice
) {
    public double estimatedFee(double quantity) {
        return estimatePrice * quantity * feeRate;
    }
}
