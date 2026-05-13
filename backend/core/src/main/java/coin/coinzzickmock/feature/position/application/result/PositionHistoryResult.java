package coin.coinzzickmock.feature.position.application.result;

import coin.coinzzickmock.feature.position.domain.PositionHistory;
import java.time.Instant;

public record PositionHistoryResult(
        String symbol,
        String positionSide,
        String marginMode,
        int leverage,
        Instant openedAt,
        double averageEntryPrice,
        double averageExitPrice,
        double positionSize,
        double realizedPnl,
        double grossRealizedPnl,
        double openFee,
        double closeFee,
        double totalFee,
        double fundingCost,
        double netRealizedPnl,
        double roi,
        Instant closedAt,
        String closeReason
) {
    public static PositionHistoryResult from(PositionHistory history) {
        return new PositionHistoryResult(
                history.symbol(),
                history.positionSide(),
                history.marginMode(),
                history.leverage(),
                history.openedAt(),
                history.averageEntryPrice(),
                history.averageExitPrice(),
                history.positionSize(),
                history.realizedPnl(),
                history.grossRealizedPnl(),
                history.openFee(),
                history.closeFee(),
                history.totalFee(),
                history.fundingCost(),
                history.netRealizedPnl(),
                history.roi(),
                history.closedAt(),
                history.closeReason()
        );
    }
}
