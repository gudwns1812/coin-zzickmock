package coin.coinzzickmock.feature.position.application.result;

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
        double roi,
        Instant closedAt,
        String closeReason
) {
}
