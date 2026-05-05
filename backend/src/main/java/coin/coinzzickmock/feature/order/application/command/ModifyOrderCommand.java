package coin.coinzzickmock.feature.order.application.command;

public record ModifyOrderCommand(
        Long memberId,
        String orderId,
        double limitPrice
) {
}
