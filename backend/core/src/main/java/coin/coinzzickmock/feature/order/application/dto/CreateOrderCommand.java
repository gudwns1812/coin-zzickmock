package coin.coinzzickmock.feature.order.application.dto;

public record CreateOrderCommand(
        Long memberId,
        String symbol,
        String positionSide,
        String orderType,
        String marginMode,
        int leverage,
        double quantity,
        Double limitPrice
) {
}
