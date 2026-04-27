package coin.coinzzickmock.feature.order.application.result;

import java.time.Instant;

public record OpenOrderResult(
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
}
