package coin.coinzzickmock.feature.order.application.result;

import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import java.time.Instant;

public record OrderHistoryResult(
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
    public static OrderHistoryResult from(FuturesOrder order) {
        return new OrderHistoryResult(
                order.orderId(),
                order.symbol(),
                order.positionSide(),
                order.orderType(),
                order.orderPurpose(),
                order.marginMode(),
                order.leverage(),
                order.quantity(),
                order.limitPrice(),
                order.status(),
                order.feeType(),
                order.estimatedFee(),
                order.executionPrice(),
                order.orderTime(),
                order.triggerPrice(),
                order.triggerType(),
                order.triggerSource(),
                order.ocoGroupId()
        );
    }
}
