package coin.coinzzickmock.feature.order.api;

import coin.coinzzickmock.feature.order.application.result.OpenOrderResult;
import java.time.Instant;

public record OpenOrderResponse(
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
        Instant orderTime
) {
    public static OpenOrderResponse from(OpenOrderResult result) {
        return new OpenOrderResponse(
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
                result.orderTime()
        );
    }
}
