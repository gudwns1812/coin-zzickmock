package coin.coinzzickmock.feature.position.domain;

import java.time.Instant;

public record PositionCloseOutcome(
        double closedQuantity,
        double grossRealizedPnl,
        double closeFee,
        double releasedMargin,
        PositionSnapshot remainingPosition,
        Instant openedAt,
        double originalQuantity,
        double averageEntryPrice,
        double accumulatedClosedQuantity,
        double accumulatedExitNotional,
        double accumulatedGrossRealizedPnl,
        double accumulatedOpenFee,
        double accumulatedCloseFee,
        double accumulatedFundingCost
) {
    public double netRealizedPnl() {
        return eventNetRealizedPnl();
    }

    public double eventNetRealizedPnl() {
        return grossRealizedPnl - closeFee;
    }

    public double positionNetRealizedPnl() {
        return accumulatedGrossRealizedPnl - totalFee() - accumulatedFundingCost;
    }

    public double totalFee() {
        return accumulatedOpenFee + accumulatedCloseFee;
    }

    public boolean fullyClosed() {
        return remainingPosition == null;
    }

    public double averageExitPrice() {
        if (accumulatedClosedQuantity == 0) {
            return 0;
        }
        return accumulatedExitNotional / accumulatedClosedQuantity;
    }

    public double positionSize() {
        return originalQuantity;
    }

    public double roi(int leverage) {
        double margin = (averageEntryPrice * originalQuantity) / leverage;
        if (margin == 0) {
            return 0;
        }
        return positionNetRealizedPnl() / margin;
    }
}
