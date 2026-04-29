package coin.coinzzickmock.feature.order.domain;

public record OrderPlacementRequest(
        String orderPurpose,
        String positionSide,
        String orderType,
        Double limitPrice,
        double quantity,
        int leverage
) {
}
