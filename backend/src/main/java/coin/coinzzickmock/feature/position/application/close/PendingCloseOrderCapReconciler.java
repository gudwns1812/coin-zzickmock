package coin.coinzzickmock.feature.position.application.close;

import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PendingCloseOrderCapReconciler {
    private final OrderRepository orderRepository;

    public void reconcile(String memberId, FuturesOrder submittedOrder, double heldQuantity, double currentPrice) {
        reconcile(
                memberId,
                submittedOrder.symbol(),
                submittedOrder.positionSide(),
                submittedOrder.marginMode(),
                heldQuantity,
                currentPrice
        );
    }

    public void reconcile(String memberId, PositionSnapshot position, double heldQuantity, double currentPrice) {
        reconcile(
                memberId,
                position.symbol(),
                position.positionSide(),
                position.marginMode(),
                heldQuantity,
                currentPrice
        );
    }

    public double pendingCloseQuantity(String memberId, PositionSnapshot position) {
        return pendingCloseOrders(memberId, position.symbol(), position.positionSide(), position.marginMode()).stream()
                .mapToDouble(FuturesOrder::quantity)
                .sum();
    }

    public double closeableQuantity(String memberId, PositionSnapshot position) {
        return Math.max(0, position.quantity() - pendingCloseQuantity(memberId, position));
    }

    public void reconcile(
            String memberId,
            String symbol,
            String positionSide,
            String marginMode,
            double heldQuantity,
            double currentPrice
    ) {
        List<FuturesOrder> pendingCloseOrders = pendingCloseOrders(memberId, symbol, positionSide, marginMode);
        double pendingQuantity = pendingCloseOrders.stream()
                .mapToDouble(FuturesOrder::quantity)
                .sum();
        double excessQuantity = pendingQuantity - Math.max(0, heldQuantity);
        if (excessQuantity <= 0) {
            return;
        }

        for (FuturesOrder order : pendingCloseOrders.stream()
                .sorted(leastLikelyToExecuteFirst(positionSide, currentPrice))
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

    private List<FuturesOrder> pendingCloseOrders(
            String memberId,
            String symbol,
            String positionSide,
            String marginMode
    ) {
        return orderRepository.findPendingCloseOrders(
                memberId,
                symbol,
                positionSide,
                marginMode
        );
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
        return priceComparator
                .thenComparing(Comparator.comparing(FuturesOrder::orderTime).reversed())
                .thenComparing(FuturesOrder::orderId);
    }
}
