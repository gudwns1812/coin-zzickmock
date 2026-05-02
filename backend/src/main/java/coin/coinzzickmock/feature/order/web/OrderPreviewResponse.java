package coin.coinzzickmock.feature.order.web;

public record OrderPreviewResponse(
        String feeType,
        double estimatedFee,
        double estimatedInitialMargin,
        Double estimatedLiquidationPrice,
        double estimatedEntryPrice,
        boolean executable
) {
}
