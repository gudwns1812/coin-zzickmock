package coin.coinzzickmock.feature.order.application.result;

public record CancelOrderResult(
        String orderId,
        String symbol,
        String status
) {
}
