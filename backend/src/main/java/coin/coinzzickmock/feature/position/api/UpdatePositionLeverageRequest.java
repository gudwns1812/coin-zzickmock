package coin.coinzzickmock.feature.position.api;

public record UpdatePositionLeverageRequest(
        String symbol,
        String positionSide,
        String marginMode,
        int leverage
) {
}
