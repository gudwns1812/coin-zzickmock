package coin.coinzzickmock.feature.position.application.close;

import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PendingCloseOrderCapReconciler {
    private final OrderRepository orderRepository;

    public void reconcile(Long memberId, FuturesOrder submittedOrder, double heldQuantity, double currentPrice) {
        reconcile(
                memberId,
                submittedOrder.symbol(),
                submittedOrder.positionSide(),
                submittedOrder.marginMode(),
                heldQuantity,
                currentPrice
        );
    }

    public void reconcile(Long memberId, PositionSnapshot position, double heldQuantity, double currentPrice) {
        reconcile(
                memberId,
                position.symbol(),
                position.positionSide(),
                position.marginMode(),
                heldQuantity,
                currentPrice
        );
    }

    public double pendingCloseQuantity(Long memberId, PositionSnapshot position) {
        return exposureBuckets(pendingCloseOrders(
                memberId,
                position.symbol(),
                position.positionSide(),
                position.marginMode()
        )).stream()
                .mapToDouble(CloseExposureBucket::exposureQuantity)
                .sum();
    }

    public double closeableQuantity(Long memberId, PositionSnapshot position) {
        return Math.max(0, position.quantity() - pendingCloseQuantity(memberId, position));
    }

    public void reconcile(
            Long memberId,
            String symbol,
            String positionSide,
            String marginMode,
            double heldQuantity,
            double currentPrice
    ) {
        List<FuturesOrder> pendingCloseOrders = pendingCloseOrders(memberId, symbol, positionSide, marginMode);
        List<CloseExposureBucket> buckets = exposureBuckets(pendingCloseOrders);
        double pendingQuantity = buckets.stream()
                .mapToDouble(CloseExposureBucket::exposureQuantity)
                .sum();
        double excessQuantity = pendingQuantity - Math.max(0, heldQuantity);
        if (excessQuantity <= 0) {
            return;
        }

        for (CloseExposureBucket bucket : buckets.stream()
                .sorted(bucketComparator(positionSide, currentPrice))
                .toList()) {
            if (excessQuantity <= 0) {
                return;
            }
            double reduction = Math.min(bucket.exposureQuantity(), excessQuantity);
            double nextQuantity = bucket.exposureQuantity() - reduction;
            List<String> orderIds = bucket.orders().stream()
                    .map(FuturesOrder::orderId)
                    .toList();
            if (nextQuantity <= 0) {
                orderRepository.cancelPendingOrders(memberId, orderIds);
            } else {
                orderRepository.capPendingOrderQuantity(memberId, orderIds, nextQuantity);
            }
            excessQuantity -= reduction;
        }
    }

    private List<CloseExposureBucket> exposureBuckets(List<FuturesOrder> orders) {
        List<CloseExposureBucket> buckets = new ArrayList<>();
        Map<String, List<FuturesOrder>> grouped = orders.stream()
                .filter(order -> order.ocoGroupId() != null && !order.ocoGroupId().isBlank())
                .collect(Collectors.groupingBy(FuturesOrder::ocoGroupId));

        orders.stream()
                .filter(order -> order.ocoGroupId() == null || order.ocoGroupId().isBlank())
                .map(order -> new CloseExposureBucket(List.of(order)))
                .forEach(buckets::add);
        grouped.values().stream()
                .map(CloseExposureBucket::new)
                .forEach(buckets::add);
        return buckets;
    }

    private List<FuturesOrder> pendingCloseOrders(
            Long memberId,
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
                    (FuturesOrder order) -> executionReferencePrice(order, currentPrice)
            ).reversed();
        } else {
            priceComparator = Comparator.comparing(
                    order -> executionReferencePrice(order, currentPrice)
            );
        }
        return priceComparator
                .thenComparing(Comparator.comparing(FuturesOrder::orderTime).reversed())
                .thenComparing(FuturesOrder::orderId);
    }

    private Comparator<CloseExposureBucket> bucketComparator(String positionSide, double currentPrice) {
        Comparator<FuturesOrder> orderComparator = leastLikelyToExecuteFirst(positionSide, currentPrice);
        return (left, right) -> {
            int byConditional = Boolean.compare(right.isConditional(), left.isConditional());
            if (byConditional != 0) {
                return byConditional;
            }
            return orderComparator.compare(left.sortOrder(orderComparator), right.sortOrder(orderComparator));
        };
    }

    private double executionReferencePrice(FuturesOrder order, double currentPrice) {
        if (order.limitPrice() != null) {
            return order.limitPrice();
        }
        if (order.triggerPrice() != null) {
            return order.triggerPrice();
        }
        return currentPrice;
    }

    private record CloseExposureBucket(List<FuturesOrder> orders) {
        double exposureQuantity() {
            return orders.stream()
                    .mapToDouble(FuturesOrder::quantity)
                    .max()
                    .orElse(0);
        }

        boolean isConditional() {
            return orders.stream().anyMatch(FuturesOrder::isConditionalCloseOrder);
        }

        FuturesOrder sortOrder(Comparator<FuturesOrder> comparator) {
            return orders.stream().sorted(comparator).findFirst().orElseThrow();
        }
    }
}
