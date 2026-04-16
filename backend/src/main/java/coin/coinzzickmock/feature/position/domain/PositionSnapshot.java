package coin.coinzzickmock.feature.position.domain;

public record PositionSnapshot(
        String symbol,
        String positionSide,
        String marginMode,
        int leverage,
        double quantity,
        double entryPrice,
        double markPrice,
        Double liquidationPrice,
        double unrealizedPnl
) {
}
