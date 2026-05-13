package coin.coinzzickmock.feature.order.domain;

public record OrderPlacementRequest(
        String orderPurpose,
        String positionSide,
        String orderType,
        String marginMode,
        Double limitPrice,
        double quantity,
        int leverage
) {
}
