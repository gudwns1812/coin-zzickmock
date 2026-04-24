package coin.coinzzickmock.feature.order.api;

import coin.coinzzickmock.feature.order.application.result.OrderHistoryResult;

public record OrderHistoryResponse(
        String orderId,
        String symbol,
        String positionSide,
        String orderType,
        String marginMode,
        int leverage,
        double quantity,
        Double limitPrice,
        String status,
        String feeType,
        double estimatedFee,
        double executionPrice
) {
    public static OrderHistoryResponse from(OrderHistoryResult result) {
        return new OrderHistoryResponse(
                result.orderId(),
                result.symbol(),
                result.positionSide(),
                result.orderType(),
                result.marginMode(),
                result.leverage(),
                result.quantity(),
                result.limitPrice(),
                result.status(),
                result.feeType(),
                result.estimatedFee(),
                result.executionPrice()
        );
    }
}
