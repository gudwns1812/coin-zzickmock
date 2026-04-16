package coin.coinzzickmock.feature.order.domain;

public record OrderPreview(
        String feeType,
        double estimatedFee,
        double estimatedInitialMargin,
        Double estimatedLiquidationPrice,
        double estimatedEntryPrice,
        boolean executable
) {
}
