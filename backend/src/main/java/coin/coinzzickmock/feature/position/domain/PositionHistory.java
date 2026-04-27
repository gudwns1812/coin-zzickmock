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
    public static final String CLOSE_REASON_MANUAL = "MANUAL";
    public static final String CLOSE_REASON_LIMIT_CLOSE = "LIMIT_CLOSE";
    public static final String CLOSE_REASON_LIQUIDATION = "LIQUIDATION";
    public static final String CLOSE_REASON_TAKE_PROFIT = "TAKE_PROFIT";
    public static final String CLOSE_REASON_STOP_LOSS = "STOP_LOSS";
}
