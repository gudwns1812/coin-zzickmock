package coin.coinzzickmock.feature.position.api;

public record ClosePositionRequest(
        String symbol,
        String positionSide,
        String marginMode,
        double quantity
) {
}
