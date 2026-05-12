package coin.coinzzickmock.feature.order.web;

import coin.coinzzickmock.feature.order.application.result.CancelOrderResult;

public record CancelOrderResponse(
        String orderId,
        String symbol,
        String status
) {
    public static CancelOrderResponse from(CancelOrderResult result) {
        return new CancelOrderResponse(
                result.orderId(),
                result.symbol(),
                result.status()
        );
    }
}
