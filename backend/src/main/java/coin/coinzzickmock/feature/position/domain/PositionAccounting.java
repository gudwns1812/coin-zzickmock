package coin.coinzzickmock.feature.position.domain;

public record PositionAccounting(
        double originalQuantity,
        double accumulatedClosedQuantity,
        double accumulatedExitNotional,
        double accumulatedRealizedPnl,
        double accumulatedOpenFee,
        double accumulatedCloseFee,
        double accumulatedFundingCost
) {
    public double netRealizedPnl() {
        if (originalQuantity <= 0 || accumulatedClosedQuantity <= 0) {
            return 0;
        }
        double closedRatio = Math.min(1, accumulatedClosedQuantity / originalQuantity);
        return accumulatedRealizedPnl
                - accumulatedCloseFee
                - (accumulatedOpenFee * closedRatio)
                - (accumulatedFundingCost * closedRatio);
    }
}
