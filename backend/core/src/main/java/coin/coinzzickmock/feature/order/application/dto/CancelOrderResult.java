package coin.coinzzickmock.feature.order.application.dto;

import coin.coinzzickmock.feature.order.domain.FuturesOrder;

public record CancelOrderResult(
        String orderId,
        String symbol,
        String status
) {
    public static CancelOrderResult from(FuturesOrder order) {
        return new CancelOrderResult(
                order.orderId(),
                order.symbol(),
                order.status()
        );
    }
}
