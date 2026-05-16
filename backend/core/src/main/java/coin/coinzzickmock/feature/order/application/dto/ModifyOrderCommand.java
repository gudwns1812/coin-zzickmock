package coin.coinzzickmock.feature.order.application.dto;

import java.math.BigDecimal;

public record ModifyOrderCommand(
        Long memberId,
        String orderId,
        BigDecimal limitPrice
) {
}
