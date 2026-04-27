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
        double unrealizedPnl,
        double margin,
        double roi,
        double pendingCloseQuantity,
        double closeableQuantity,
        Double takeProfitPrice,
        Double stopLossPrice
) {
}
