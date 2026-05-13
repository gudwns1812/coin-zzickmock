package coin.coinzzickmock.feature.positionpeek.application.result;

public record PositionPeekPublicPositionResult(
        String symbol,
        String positionSide,
        int leverage,
        double positionSize,
        double notionalValue,
        double unrealizedPnl,
        double roi
) {
}
