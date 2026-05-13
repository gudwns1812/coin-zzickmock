package coin.coinzzickmock.feature.position.application.result;

import coin.coinzzickmock.feature.position.domain.PositionCloseOutcome;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import coin.coinzzickmock.feature.reward.application.result.RewardPointResult;

public record ClosePositionResult(
        String symbol,
        double closedQuantity,
        double realizedPnl,
        double grantedPoint
) {
    public static ClosePositionResult pending(String symbol) {
        return new ClosePositionResult(symbol, 0, 0, 0);
    }

    public static ClosePositionResult from(
            PositionSnapshot position,
            PositionCloseOutcome closeOutcome,
            RewardPointResult rewardPointResult
    ) {
        return new ClosePositionResult(
                position.symbol(),
                closeOutcome.closedQuantity(),
                closeOutcome.netRealizedPnl(),
                rewardPointResult.rewardPoint()
        );
    }
}
