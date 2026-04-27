package coin.coinzzickmock.feature.position.api;

public record PositionSummaryResponse(
        String symbol,
        String positionSide,
        String marginMode,
        int leverage,
        double quantity,
        double entryPrice,
        double markPrice,
        Double liquidationPrice,
        double unrealizedPnl,
        double margin,
        double roi,
        double pendingCloseQuantity,
        double closeableQuantity,
        Double takeProfitPrice,
        Double stopLossPrice
) {
}
