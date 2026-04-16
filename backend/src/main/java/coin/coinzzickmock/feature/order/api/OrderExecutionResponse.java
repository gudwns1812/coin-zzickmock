package coin.coinzzickmock.feature.order.api;

public record OrderExecutionResponse(
        String orderId,
        String status,
        String symbol,
        String feeType,
        double estimatedFee,
        double estimatedInitialMargin,
        Double estimatedLiquidationPrice,
        double executionPrice
) {
}
