package coin.coinzzickmock.feature.position.api;

public record UpdatePositionTpslRequest(
        String symbol,
        String positionSide,
        String marginMode,
        Double takeProfitPrice,
        Double stopLossPrice
) {
}
