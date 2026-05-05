package coin.coinzzickmock.feature.order.web;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import java.math.BigDecimal;

public record ModifyOrderRequest(BigDecimal limitPrice) {
    public BigDecimal requireLimitPrice() {
        if (limitPrice == null || limitPrice.signum() <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, "주문 가격을 확인해주세요.");
        }
        return limitPrice;
    }
}
