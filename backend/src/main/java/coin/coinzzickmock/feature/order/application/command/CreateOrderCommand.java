package coin.coinzzickmock.feature.order.application.command;

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
