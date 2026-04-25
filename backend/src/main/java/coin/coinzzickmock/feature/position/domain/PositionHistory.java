package coin.coinzzickmock.feature.position.domain;

import java.time.Instant;

public record PositionHistory(
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
    public static final String CLOSE_REASON_MANUAL = "MANUAL";
    public static final String CLOSE_REASON_LIMIT_CLOSE = "LIMIT_CLOSE";
    public static final String CLOSE_REASON_LIQUIDATION = "LIQUIDATION";
}
