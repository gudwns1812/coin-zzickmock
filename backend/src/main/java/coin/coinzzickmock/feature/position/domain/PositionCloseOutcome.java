package coin.coinzzickmock.feature.position.domain;

import java.time.Instant;

public record PositionCloseOutcome(
        double closedQuantity,
        double realizedPnl,
        double closeFee,
        double releasedMargin,
        PositionSnapshot remainingPosition,
        Instant openedAt,
        double originalQuantity,
        double averageEntryPrice,
        double accumulatedClosedQuantity,
        double accumulatedExitNotional,
        double accumulatedRealizedPnl,
        double accumulatedCloseFee
) {
    public double netRealizedPnl() {
        return realizedPnl - closeFee;
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
        return accumulatedRealizedPnl / margin;
    }
}
