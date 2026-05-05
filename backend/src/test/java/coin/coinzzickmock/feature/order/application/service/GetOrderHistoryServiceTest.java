package coin.coinzzickmock.feature.order.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.result.OrderHistoryResult;
import coin.coinzzickmock.feature.order.application.result.PendingOrderCandidate;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetOrderHistoryServiceTest {
    @Test
    void returnsCreatedOrdersIncludingPendingForRequestedSymbol() {
        GetOrderHistoryService service = new GetOrderHistoryService(new InMemoryOrderRepository(
                order("1", "BTCUSDT", "LONG", "LIMIT", "ISOLATED", 10, 0.1, 99000.0, "PENDING"),
                order("2", "BTCUSDT", "SHORT", "LIMIT", "ISOLATED", 10, 0.2, 101000.0, "CANCELLED"),
                order("3", "BTCUSDT", "LONG", "MARKET", "CROSS", 20, 0.3, null, "FILLED"),
                order("4", "ETHUSDT", "LONG", "MARKET", "ISOLATED", 5, 1.0, null, "FILLED")
        ));

        List<OrderHistoryResult> results = service.getOrderHistory(1L, "BTCUSDT");

        assertEquals(3, results.size());
        assertEquals("1", results.get(0).orderId());
        assertEquals("PENDING", results.get(0).status());
        assertEquals("2", results.get(1).orderId());
        assertEquals("3", results.get(2).orderId());
    }

    private static FuturesOrder order(
            String orderId,
            String symbol,
            String positionSide,
            String orderType,
            String marginMode,
            int leverage,
            double quantity,
            Double limitPrice,
            String status
    ) {
        return new FuturesOrder(
                orderId,
                symbol,
                positionSide,
                orderType,
                marginMode,
                leverage,
                quantity,
                limitPrice,
                status,
                "MAKER",
                1.5,
                limitPrice == null ? 100500.0 : limitPrice
        );
    }

    private static class InMemoryOrderRepository extends coin.coinzzickmock.testsupport.TestOrderRepository {
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
                    .filter(order -> order.symbol().equals(symbol))
                    .filter(FuturesOrder::isPending)
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
            return Optional.empty();
        }

        @Override
        public FuturesOrder updateStatus(Long memberId, String orderId, String status) {
            throw new UnsupportedOperationException();
        }
    }
}
