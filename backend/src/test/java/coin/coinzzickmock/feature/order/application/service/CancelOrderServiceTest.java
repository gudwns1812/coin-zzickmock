package coin.coinzzickmock.feature.order.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.result.CancelOrderResult;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CancelOrderServiceTest {
    @Test
    void cancelsPendingOrder() {
        InMemoryOrderRepository repository = new InMemoryOrderRepository();
        repository.save("demo-member", new FuturesOrder(
                "1",
                "BTCUSDT",
                "LONG",
                "LIMIT",
                "ISOLATED",
                10,
                0.1,
                99000.0,
                "PENDING",
                "MAKER",
                1.5,
                99000.0
        ));

        CancelOrderService service = new CancelOrderService(repository);
        CancelOrderResult result = service.cancel("demo-member", "1");

        assertEquals("CANCELLED", result.status());
        assertEquals("CANCELLED", repository.findByMemberIdAndOrderId("demo-member", "1").orElseThrow().status());
    }

    @Test
    void rejectsCancelWhenOrderIsNotPending() {
        InMemoryOrderRepository repository = new InMemoryOrderRepository();
        repository.save("demo-member", new FuturesOrder(
                "1",
                "BTCUSDT",
                "LONG",
                "LIMIT",
                "ISOLATED",
                10,
                0.1,
                99000.0,
                "FILLED",
                "TAKER",
                1.5,
                99000.0
        ));

        CancelOrderService service = new CancelOrderService(repository);

        assertThrows(CoreException.class, () -> service.cancel("demo-member", "1"));
    }

    private static class InMemoryOrderRepository implements OrderRepository {
        private final List<FuturesOrder> orders = new ArrayList<>();

        @Override
        public FuturesOrder save(String memberId, FuturesOrder futuresOrder) {
            orders.removeIf(order -> order.orderId().equals(futuresOrder.orderId()));
            orders.add(futuresOrder);
            return futuresOrder;
        }

        @Override
        public List<FuturesOrder> findByMemberId(String memberId) {
            return List.copyOf(orders);
        }

        @Override
        public Optional<FuturesOrder> findByMemberIdAndOrderId(String memberId, String orderId) {
            return orders.stream().filter(order -> order.orderId().equals(orderId)).findFirst();
        }

        @Override
        public FuturesOrder updateStatus(String memberId, String orderId, String status) {
            FuturesOrder order = findByMemberIdAndOrderId(memberId, orderId).orElseThrow();
            FuturesOrder updated = new FuturesOrder(
                    order.orderId(),
                    order.symbol(),
                    order.positionSide(),
                    order.orderType(),
                    order.marginMode(),
                    order.leverage(),
                    order.quantity(),
                    order.limitPrice(),
                    status,
                    order.feeType(),
                    order.estimatedFee(),
                    order.executionPrice()
            );
            save(memberId, updated);
            return updated;
        }
    }
}
