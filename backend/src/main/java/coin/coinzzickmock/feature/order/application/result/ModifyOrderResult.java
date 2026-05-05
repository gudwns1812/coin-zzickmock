package coin.coinzzickmock.feature.order.application.result;

public record ModifyOrderResult(
        String orderId,
        String symbol,
        String status,
        Double limitPrice,
        String feeType,
        double estimatedFee,
        double executionPrice
) {
}
