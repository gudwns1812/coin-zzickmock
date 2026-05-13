package coin.coinzzickmock.feature.order.application.result;

import coin.coinzzickmock.feature.order.domain.FuturesOrder;
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
    public static ModifyOrderResult from(FuturesOrder order) {
        return new ModifyOrderResult(
                order.orderId(),
                order.symbol(),
                order.status(),
                BigDecimal.valueOf(order.limitPrice()),
                order.feeType(),
                BigDecimal.valueOf(order.estimatedFee()),
                BigDecimal.valueOf(order.executionPrice())
        );
    }
}
