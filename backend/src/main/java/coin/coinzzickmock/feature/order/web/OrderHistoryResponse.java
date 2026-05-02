package coin.coinzzickmock.feature.order.web;

import coin.coinzzickmock.feature.order.application.result.OrderHistoryResult;
import java.time.Instant;

public record OrderHistoryResponse(
        String orderId,
        String symbol,
        String positionSide,
        String orderType,
        String orderPurpose,
        String marginMode,
        int leverage,
        double quantity,
        Double limitPrice,
        String status,
        String feeType,
        double estimatedFee,
        double executionPrice,
        Instant orderTime,
        Double triggerPrice,
        String triggerType,
        String triggerSource,
        String ocoGroupId
) {
    public static OrderHistoryResponse from(OrderHistoryResult result) {
        return new OrderHistoryResponse(
                result.orderId(),
                result.symbol(),
                result.positionSide(),
                result.orderType(),
                result.orderPurpose(),
                result.marginMode(),
                result.leverage(),
                result.quantity(),
                result.limitPrice(),
                result.status(),
                result.feeType(),
                result.estimatedFee(),
                result.executionPrice(),
                result.orderTime(),
                result.triggerPrice(),
                result.triggerType(),
                result.triggerSource(),
                result.ocoGroupId()
        );
    }
}
