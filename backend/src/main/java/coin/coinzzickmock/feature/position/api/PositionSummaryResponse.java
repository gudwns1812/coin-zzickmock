package coin.coinzzickmock.feature.position.api;

public record PositionSummaryResponse(
        String symbol,
        String positionSide,
        String marginMode,
        int leverage,
        double quantity,
        double entryPrice,
        double markPrice,
        double unrealizedPnl
) {
}
