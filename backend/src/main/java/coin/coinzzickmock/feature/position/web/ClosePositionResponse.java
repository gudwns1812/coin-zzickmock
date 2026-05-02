package coin.coinzzickmock.feature.position.web;

public record ClosePositionResponse(
        String symbol,
        double closedQuantity,
        double realizedPnl,
        double grantedPoint
) {
}
