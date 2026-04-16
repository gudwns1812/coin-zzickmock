package coin.coinzzickmock.feature.position.api;

public record ClosePositionResponse(
        String symbol,
        double closedQuantity,
        double realizedPnl,
        double grantedPoint
) {
}
