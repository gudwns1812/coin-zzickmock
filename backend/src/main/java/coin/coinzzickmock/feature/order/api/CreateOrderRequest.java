package coin.coinzzickmock.feature.order.api;

public record CreateOrderRequest(
        String symbol,
        String positionSide,
        String orderType,
        String marginMode,
        int leverage,
        double quantity,
        Double limitPrice
) {
}
