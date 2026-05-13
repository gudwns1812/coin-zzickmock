package coin.coinzzickmock.feature.order.application.result;

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
