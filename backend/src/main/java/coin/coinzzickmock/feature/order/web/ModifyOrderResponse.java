package coin.coinzzickmock.feature.order.web;

import coin.coinzzickmock.feature.order.application.result.ModifyOrderResult;

public record ModifyOrderResponse(
        String orderId,
        String symbol,
        String status,
        Double limitPrice,
        String feeType,
        double estimatedFee,
        double executionPrice
) {
    public static ModifyOrderResponse from(ModifyOrderResult result) {
        return new ModifyOrderResponse(
                result.orderId(),
                result.symbol(),
                result.status(),
                result.limitPrice(),
                result.feeType(),
                result.estimatedFee(),
                result.executionPrice()
        );
    }
}
