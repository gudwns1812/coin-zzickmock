package coin.coinzzickmock.feature.order.web;

import coin.coinzzickmock.feature.order.application.dto.ModifyOrderResult;
import java.math.BigDecimal;

public record ModifyOrderResponse(
        String orderId,
        String symbol,
        String status,
        BigDecimal limitPrice,
        String feeType,
        BigDecimal estimatedFee,
        BigDecimal executionPrice
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
