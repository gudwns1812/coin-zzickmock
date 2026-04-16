package coin.coinzzickmock.feature.order.application.result;

public record CreateOrderResult(
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
