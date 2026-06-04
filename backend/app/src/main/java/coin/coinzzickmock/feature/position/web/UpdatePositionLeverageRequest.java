package coin.coinzzickmock.feature.position.web;

public record UpdatePositionLeverageRequest(
        String symbol,
        String positionSide,
        String marginMode,
        int leverage
) {
}
