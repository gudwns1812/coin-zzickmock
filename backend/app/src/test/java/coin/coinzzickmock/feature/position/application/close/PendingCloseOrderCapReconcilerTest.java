package coin.coinzzickmock.feature.position.application.close;

import static org.junit.jupiter.api.Assertions.assertEquals;

import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.dto.PendingOrderCandidate;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PendingCloseOrderCapReconcilerTest {
    @Test
    void pendingCloseQuantityCountsManualCloseOrdersOnly() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        PositionSnapshot position = position();
        orderRepository.save(1L, manualClose("manual", 0.4, 120));
        orderRepository.save(1L, tpsl("tp", 1, 130, FuturesOrder.TRIGGER_TYPE_TAKE_PROFIT));
        orderRepository.save(1L, tpsl("sl", 1, 90, FuturesOrder.TRIGGER_TYPE_STOP_LOSS));

        PendingCloseOrderCapReconciler reconciler = new PendingCloseOrderCapReconciler(orderRepository);

        assertEquals(0.4, reconciler.pendingCloseQuantity(1L, position), 0.0001);
        assertEquals(0.6, reconciler.closeableQuantity(1L, position), 0.0001);
    }

    @Test
    void reconcileCapsManualOrdersWithoutCancellingProtectiveOrders() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        orderRepository.save(1L, manualClose("manual", 0.8, 120));
        orderRepository.save(1L, tpsl("tp", 1, 130, FuturesOrder.TRIGGER_TYPE_TAKE_PROFIT));

        new PendingCloseOrderCapReconciler(orderRepository)
                .reconcile(1L, "BTCUSDT", "LONG", "ISOLATED", 0.3, 100);

        FuturesOrder manual = orderRepository.findByMemberIdAndOrderId(1L, "manual").orElseThrow();
        FuturesOrder tp = orderRepository.findByMemberIdAndOrderId(1L, "tp").orElseThrow();
        assertEquals(FuturesOrder.STATUS_PENDING, manual.status());
        assertEquals(0.3, manual.quantity(), 0.0001);
        assertEquals(FuturesOrder.STATUS_PENDING, tp.status());
        assertEquals(1, tp.quantity(), 0.0001);
    }

    @Test
    void reconcileWithZeroHeldQuantityCancelsManualOrdersOnly() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        orderRepository.save(1L, manualClose("manual", 0.8, 120));
        orderRepository.save(1L, tpsl("tp", 1, 130, FuturesOrder.TRIGGER_TYPE_TAKE_PROFIT));

        new PendingCloseOrderCapReconciler(orderRepository)
                .reconcile(1L, "BTCUSDT", "LONG", "ISOLATED", 0, 100);

        assertEquals(FuturesOrder.STATUS_CANCELLED,
                orderRepository.findByMemberIdAndOrderId(1L, "manual").orElseThrow().status());
        assertEquals(FuturesOrder.STATUS_PENDING,
                orderRepository.findByMemberIdAndOrderId(1L, "tp").orElseThrow().status());
    }

    private static PositionSnapshot position() {
        return PositionSnapshot.open("BTCUSDT", "LONG", "ISOLATED", 10, 1, 100, 100);
    }

    private static FuturesOrder manualClose(String orderId, double quantity, double limitPrice) {
        return FuturesOrder.place(
                orderId,
                "BTCUSDT",
                "LONG",
                "LIMIT",
                FuturesOrder.PURPOSE_CLOSE_POSITION,
                "ISOLATED",
                10,
                quantity,
                limitPrice,
                false,
                "MAKER",
                0,
                limitPrice
        );
    }

    private static FuturesOrder tpsl(String orderId, double quantity, double triggerPrice, String triggerType) {
        return FuturesOrder.conditionalClose(
                orderId,
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                quantity,
                triggerPrice,
                triggerType,
                "oco-1"
        );
    }

    private static class InMemoryOrderRepository extends coin.coinzzickmock.testsupport.TestOrderRepository {
        private final Map<String, PendingOrderCandidate> orders = new LinkedHashMap<>();

        @Override
        public FuturesOrder save(Long memberId, FuturesOrder futuresOrder) {
            orders.put(key(memberId, futuresOrder.orderId()), new PendingOrderCandidate(memberId, futuresOrder));
            return futuresOrder;
        }

        @Override
        public List<FuturesOrder> findByMemberId(Long memberId) {
            return orders.values().stream()
                    .filter(candidate -> candidate.memberId().equals(memberId))
                    .map(PendingOrderCandidate::order)
                    .toList();
        }

        @Override
        public Optional<FuturesOrder> findByMemberIdAndOrderId(Long memberId, String orderId) {
            return Optional.ofNullable(orders.get(key(memberId, orderId)))
                    .map(PendingOrderCandidate::order);
        }

        @Override
        public List<PendingOrderCandidate> findPendingBySymbol(String symbol) {
            return orders.values().stream()
                    .filter(candidate -> candidate.symbol().equals(symbol))
                    .toList();
        }

        @Override
        public Optional<FuturesOrder> claimPendingFill(
                Long memberId,
                String orderId,
                double executionPrice,
                String feeType,
                double estimatedFee
        ) {
            return Optional.empty();
        }

        @Override
        public FuturesOrder updateStatus(Long memberId, String orderId, String status) {
            FuturesOrder current = findByMemberIdAndOrderId(memberId, orderId).orElseThrow();
            FuturesOrder updated = FuturesOrder.STATUS_CANCELLED.equals(status)
                    ? current.cancel()
                    : current;
            orders.put(key(memberId, orderId), new PendingOrderCandidate(memberId, updated));
            return updated;
        }

        @Override
        public FuturesOrder updateQuantityAndStatus(Long memberId, String orderId, double quantity, String status) {
            FuturesOrder updated = findByMemberIdAndOrderId(memberId, orderId).orElseThrow().withQuantity(quantity);
            if (FuturesOrder.STATUS_CANCELLED.equals(status)) {
                updated = updated.cancel();
            }
            orders.put(key(memberId, orderId), new PendingOrderCandidate(memberId, updated));
            return updated;
        }

        private String key(Long memberId, String orderId) {
            return memberId + ":" + orderId;
        }
    }
}
