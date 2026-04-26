package coin.coinzzickmock.feature.position.application.close;

import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PendingCloseOrderCapReconciler {
    private final OrderRepository orderRepository;

    public void reconcile(String memberId, FuturesOrder submittedOrder, double heldQuantity, double currentPrice) {
        List<FuturesOrder> pendingCloseOrders = orderRepository.findPendingCloseOrders(
                memberId,
                submittedOrder.symbol(),
                submittedOrder.positionSide(),
                submittedOrder.marginMode()
        );

        double pendingQuantity = pendingCloseOrders.stream()
                .mapToDouble(FuturesOrder::quantity)
                .sum();
        double excessQuantity = pendingQuantity - heldQuantity;
        if (excessQuantity <= 0) {
            return;
        }

        for (FuturesOrder order : pendingCloseOrders.stream()
                .sorted(leastLikelyToExecuteFirst(submittedOrder.positionSide(), currentPrice))
                .toList()) {
            if (excessQuantity <= 0) {
                return;
            }
            double reduction = Math.min(order.quantity(), excessQuantity);
            double nextQuantity = order.quantity() - reduction;
            if (nextQuantity <= 0) {
                orderRepository.updateStatus(memberId, order.orderId(), FuturesOrder.STATUS_CANCELLED);
            } else {
                orderRepository.updateQuantityAndStatus(
                        memberId,
                        order.orderId(),
                        nextQuantity,
                        FuturesOrder.STATUS_PENDING
                );
            }
            excessQuantity -= reduction;
        }
    }

    private Comparator<FuturesOrder> leastLikelyToExecuteFirst(String positionSide, double currentPrice) {
        Comparator<FuturesOrder> priceComparator;
        if ("LONG".equalsIgnoreCase(positionSide)) {
            priceComparator = Comparator.comparing(
                    (FuturesOrder order) -> order.limitPrice() == null ? currentPrice : order.limitPrice()
            ).reversed();
        } else {
            priceComparator = Comparator.comparing(
                    order -> order.limitPrice() == null ? currentPrice : order.limitPrice()
            );
        }
        return priceComparator.thenComparing(Comparator.comparing(FuturesOrder::orderTime).reversed());
    }
}
