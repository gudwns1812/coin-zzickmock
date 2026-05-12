package coin.coinzzickmock.feature.order.domain;

public record OrderPreview(
        String feeType,
        double estimatedFee,
        double estimatedInitialMargin,
        Double estimatedLiquidationPrice,
        String estimatedLiquidationPriceType,
        double estimatedEntryPrice,
        boolean executable
) {
}
