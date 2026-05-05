package coin.coinzzickmock.feature.order.application.result;

import java.math.BigDecimal;

public record ModifyOrderResult(
        String orderId,
        String symbol,
        String status,
        BigDecimal limitPrice,
        String feeType,
        BigDecimal estimatedFee,
        BigDecimal executionPrice
) {
}
