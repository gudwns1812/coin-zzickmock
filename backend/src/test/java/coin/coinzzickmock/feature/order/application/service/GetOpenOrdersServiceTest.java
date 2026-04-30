package coin.coinzzickmock.feature.order.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.result.PendingOrderCandidate;
import coin.coinzzickmock.feature.order.application.result.OpenOrderResult;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetOpenOrdersServiceTest {
    @Test
    void returnsOnlyPendingLimitOrdersForRequestedSymbol() {
        GetOpenOrdersService service = new GetOpenOrdersService(new InMemoryOrderRepository(
                new FuturesOrder("1", "BTCUSDT", "LONG", "LIMIT", "ISOLATED", 10, 0.1, 99000.0, "PENDING", "MAKER", 1.5, 99000.0),
                new FuturesOrder("2", "BTCUSDT", "SHORT", "LIMIT", "ISOLATED", 10, 0.2, 101000.0, "CANCELLED", "MAKER", 1.5, 101000.0),
                new FuturesOrder("3", "ETHUSDT", "LONG", "LIMIT", "ISOLATED", 10, 0.3, 3000.0, "PENDING", "MAKER", 1.5, 3000.0)
        ));

        List<OpenOrderResult> results = service.getOpenOrders(1L, "BTCUSDT");

        assertEquals(1, results.size());
        assertEquals("1", results.get(0).orderId());
        assertEquals("BTCUSDT", results.get(0).symbol());
    }

    private static class InMemoryOrderRepository implements OrderRepository {
        private final List<FuturesOrder> orders;

        private InMemoryOrderRepository(FuturesOrder... orders) {
            this.orders = List.of(orders);
        }

        @Override
        public FuturesOrder save(Long memberId, FuturesOrder futuresOrder) {
            return futuresOrder;
        }

        @Override
        public List<FuturesOrder> findByMemberId(Long memberId) {
            return orders;
        }

        @Override
        public Optional<FuturesOrder> findByMemberIdAndOrderId(Long memberId, String orderId) {
            return orders.stream().filter(order -> order.orderId().equals(orderId)).findFirst();
        }

        @Override
        public List<PendingOrderCandidate> findPendingBySymbol(String symbol) {
            return orders.stream()
                    .filter(FuturesOrder::isPending)
                    .filter(order -> order.symbol().equals(symbol))
                    .map(order -> new PendingOrderCandidate(1L, order))
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
            throw new UnsupportedOperationException();
        }

        @Override
        public FuturesOrder updateStatus(Long memberId, String orderId, String status) {
            throw new UnsupportedOperationException();
        }
    }
}
