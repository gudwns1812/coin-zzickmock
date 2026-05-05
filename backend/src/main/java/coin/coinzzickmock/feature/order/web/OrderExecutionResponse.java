package coin.coinzzickmock.feature.order.web;

public record OrderExecutionResponse(
        String orderId,
        String status,
        String symbol,
        String feeType,
        double estimatedFee,
        double estimatedInitialMargin,
        Double estimatedLiquidationPrice,
        String estimatedLiquidationPriceType,
        double executionPrice
) {
}
