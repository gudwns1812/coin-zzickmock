package coin.coinzzickmock.feature.position.domain;

public record PositionCloseOutcome(
        double closedQuantity,
        double realizedPnl,
        double closeFee,
        double releasedMargin,
        PositionSnapshot remainingPosition
) {
    public double netRealizedPnl() {
        return realizedPnl - closeFee;
    }
}
