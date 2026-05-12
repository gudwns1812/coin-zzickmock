package coin.coinzzickmock.feature.position.web;

public record ClosePositionRequest(
        String symbol,
        String positionSide,
        String marginMode,
        double quantity,
        String orderType,
        Double limitPrice
) {
}
