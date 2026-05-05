package coin.coinzzickmock.feature.position.application.close;

import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StaleProtectiveCloseOrderCanceller {
    private final OrderRepository orderRepository;

    public void cancel(Long memberId, PositionSnapshot position) {
        cancel(
                memberId,
                position.symbol(),
                position.positionSide(),
                position.marginMode()
        );
    }

    public void cancel(
            Long memberId,
            String symbol,
            String positionSide,
            String marginMode
    ) {
        orderRepository.findPendingConditionalCloseOrders(
                        memberId,
                        symbol,
                        positionSide,
                        marginMode
                ).stream()
                .filter(order -> order.isTakeProfitOrder() || order.isStopLossOrder())
                .map(FuturesOrder::orderId)
                .forEach(orderId -> orderRepository.updateStatus(
                        memberId,
                        orderId,
                        FuturesOrder.STATUS_CANCELLED
                ));
    }
}
