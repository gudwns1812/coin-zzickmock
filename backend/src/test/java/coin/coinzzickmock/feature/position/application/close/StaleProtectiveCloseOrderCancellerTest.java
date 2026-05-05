package coin.coinzzickmock.feature.position.application.close;

import static org.junit.jupiter.api.Assertions.assertEquals;

import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.result.PendingOrderCandidate;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class StaleProtectiveCloseOrderCancellerTest {
    @Test
    void cancelsOnlyExactScopePendingProtectiveCloseOrders() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        orderRepository.save(1L, tpsl("tp", "BTCUSDT", "LONG", "ISOLATED", FuturesOrder.TRIGGER_TYPE_TAKE_PROFIT));
        orderRepository.save(1L, tpsl("sl", "BTCUSDT", "LONG", "ISOLATED", FuturesOrder.TRIGGER_TYPE_STOP_LOSS));
        orderRepository.save(1L, manualClose("manual"));
        orderRepository.save(1L, tpsl("other-side", "BTCUSDT", "SHORT", "ISOLATED", FuturesOrder.TRIGGER_TYPE_TAKE_PROFIT));
        orderRepository.save(2L, tpsl("other-member", "BTCUSDT", "LONG", "ISOLATED", FuturesOrder.TRIGGER_TYPE_TAKE_PROFIT));

        new StaleProtectiveCloseOrderCanceller(orderRepository)
                .cancel(1L, "BTCUSDT", "LONG", "ISOLATED");

        assertEquals(FuturesOrder.STATUS_CANCELLED, orderRepository.order(1L, "tp").status());
        assertEquals(FuturesOrder.STATUS_CANCELLED, orderRepository.order(1L, "sl").status());
        assertEquals(FuturesOrder.STATUS_PENDING, orderRepository.order(1L, "manual").status());
        assertEquals(FuturesOrder.STATUS_PENDING, orderRepository.order(1L, "other-side").status());
        assertEquals(FuturesOrder.STATUS_PENDING, orderRepository.order(2L, "other-member").status());
    }

    @Test
    void isIdempotentWhenNoPendingProtectiveOrdersExist() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();

        new StaleProtectiveCloseOrderCanceller(orderRepository)
                .cancel(1L, "BTCUSDT", "LONG", "ISOLATED");

        assertEquals(0, orderRepository.findByMemberId(1L).size());
    }

    private static FuturesOrder manualClose(String orderId) {
        return FuturesOrder.place(
                orderId,
                "BTCUSDT",
                "LONG",
                "LIMIT",
                FuturesOrder.PURPOSE_CLOSE_POSITION,
                "ISOLATED",
                10,
                0.4,
                120.0,
                false,
                "MAKER",
                0,
                120
        );
    }

    private static FuturesOrder tpsl(
            String orderId,
            String symbol,
            String positionSide,
            String marginMode,
            String triggerType
    ) {
        return FuturesOrder.conditionalClose(
                orderId,
                symbol,
                positionSide,
                marginMode,
                10,
                1,
                120,
                triggerType,
                "oco-" + orderId
        );
    }

    private static class InMemoryOrderRepository implements OrderRepository {
        private final Map<String, PendingOrderCandidate> orders = new LinkedHashMap<>();

        FuturesOrder order(Long memberId, String orderId) {
            return findByMemberIdAndOrderId(memberId, orderId).orElseThrow();
        }

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
            return Optional.ofNullable(orders.get(key(memberId, orderId))).map(PendingOrderCandidate::order);
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
            FuturesOrder updated = FuturesOrder.STATUS_CANCELLED.equals(status)
                    ? order(memberId, orderId).cancel()
                    : order(memberId, orderId);
            orders.put(key(memberId, orderId), new PendingOrderCandidate(memberId, updated));
            return updated;
        }

        private String key(Long memberId, String orderId) {
            return memberId + ":" + orderId;
        }
    }
}
