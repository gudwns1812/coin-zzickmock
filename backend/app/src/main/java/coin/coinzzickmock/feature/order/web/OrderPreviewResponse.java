package coin.coinzzickmock.feature.order.web;

public record OrderPreviewResponse(
        String feeType,
        double estimatedFee,
        double estimatedInitialMargin,
        Double estimatedLiquidationPrice,
        String estimatedLiquidationPriceType,
        double estimatedEntryPrice,
        boolean executable
) {
}
