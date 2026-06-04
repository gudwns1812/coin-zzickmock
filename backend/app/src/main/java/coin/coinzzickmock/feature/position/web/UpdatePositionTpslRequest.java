package coin.coinzzickmock.feature.position.web;

public record UpdatePositionTpslRequest(
        String symbol,
        String positionSide,
        String marginMode,
        Double takeProfitPrice,
        Double stopLossPrice
) {
}
