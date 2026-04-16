package coin.coinzzickmock.feature.order.api;

public record OrderPreviewResponse(
        String feeType,
        double estimatedFee,
        double estimatedInitialMargin,
        Double estimatedLiquidationPrice,
        double estimatedEntryPrice,
        boolean executable
) {
}
