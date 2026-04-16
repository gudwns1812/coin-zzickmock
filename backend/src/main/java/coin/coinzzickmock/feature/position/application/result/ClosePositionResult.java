package coin.coinzzickmock.feature.position.application.result;

public record ClosePositionResult(
        String symbol,
        double closedQuantity,
        double realizedPnl,
        double grantedPoint
) {
}
