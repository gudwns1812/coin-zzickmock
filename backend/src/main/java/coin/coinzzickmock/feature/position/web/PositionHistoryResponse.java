package coin.coinzzickmock.feature.position.web;

import coin.coinzzickmock.feature.position.application.result.PositionHistoryResult;
import java.time.Instant;

public record PositionHistoryResponse(
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
    public static PositionHistoryResponse from(PositionHistoryResult result) {
        return new PositionHistoryResponse(
                result.symbol(),
                result.positionSide(),
                result.marginMode(),
                result.leverage(),
                result.openedAt(),
                result.averageEntryPrice(),
                result.averageExitPrice(),
                result.positionSize(),
                result.realizedPnl(),
                result.grossRealizedPnl(),
                result.openFee(),
                result.closeFee(),
                result.totalFee(),
                result.fundingCost(),
                result.netRealizedPnl(),
                result.roi(),
                result.closedAt(),
                result.closeReason()
        );
    }
}
