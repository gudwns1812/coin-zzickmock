package coin.coinzzickmock.feature.order.application.result;

import coin.coinzzickmock.feature.order.domain.FuturesOrder;

public record PendingOrderCandidate(
        Long memberId,
        FuturesOrder order
) {
    public String orderId() {
        return order.orderId();
    }

    public String symbol() {
        return order.symbol();
    }
}
