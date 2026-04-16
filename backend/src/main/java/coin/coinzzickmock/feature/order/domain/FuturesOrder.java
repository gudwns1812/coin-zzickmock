package coin.coinzzickmock.feature.order.domain;

public record FuturesOrder(
        String orderId,
        String symbol,
        String positionSide,
        String orderType,
        String marginMode,
        int leverage,
        double quantity,
        Double limitPrice,
        String status,
        String feeType,
        double estimatedFee,
        double executionPrice
) {
}
