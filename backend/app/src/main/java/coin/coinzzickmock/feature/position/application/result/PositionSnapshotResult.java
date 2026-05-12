package coin.coinzzickmock.feature.position.application.result;

public record PositionSnapshotResult(
        String symbol,
        String positionSide,
        String marginMode,
        int leverage,
        double quantity,
        double entryPrice,
        double markPrice,
        Double liquidationPrice,
        String liquidationPriceType,
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
