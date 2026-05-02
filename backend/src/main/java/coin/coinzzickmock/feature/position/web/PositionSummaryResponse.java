package coin.coinzzickmock.feature.position.web;

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
        double realizedPnl,
        double margin,
        double roi,
        double accumulatedClosedQuantity,
        double pendingCloseQuantity,
        double closeableQuantity,
        Double takeProfitPrice,
        Double stopLossPrice
) {
}
