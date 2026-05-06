package coin.coinzzickmock.feature.order.application.realtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PendingOrderFillProcessorTest {
    @Test
    void claimsPendingLimitFillEvictsCacheOpensPositionAndPublishesOnce() {
        OrderRealtimeProcessorFixtures.Scenario scenario = OrderRealtimeProcessorFixtures.scenario();
        OrderRealtimeProcessorFixtures.TrackingPendingOrderExecutionCache cache = new OrderRealtimeProcessorFixtures.TrackingPendingOrderExecutionCache();
        scenario.orders.save(1L, scenario.pendingOpenOrderAt(
                "open-long",
                "LONG",
                99,
                Instant.parse("2026-04-27T00:00:00Z")
        ));

        scenario.pendingFillProcessor(cache)
                .fillExecutablePendingOrders(scenario.marketEvent(101, 98, 98));

        FuturesOrder filled = scenario.orders.findByMemberIdAndOrderId(1L, "open-long").orElseThrow();
        assertEquals(FuturesOrder.STATUS_FILLED, filled.status());
        assertEquals(99, filled.executionPrice(), 0.0001);
        assertEquals(1, scenario.orders.limitClaimAttempts().size());
        assertEquals("open-long:99.0:99.0", scenario.orders.limitClaimAttempts().get(0));
        assertTrue(scenario.positions.findOpenPosition(1L, "BTCUSDT", "LONG", "ISOLATED").isPresent());
        assertEquals(1, scenario.events.tradingEvents().size());
        assertEquals("ORDER_FILLED", scenario.events.tradingEvents().get(0).type());
        assertEquals("open-long", scenario.events.tradingEvents().get(0).orderId());
        assertEquals(java.util.List.of("open-long"), cache.evictedOrderIds());
    }

    @Test
    void skipsStaleReloadedOrderWithoutFillAndEvictsCache() {
        OrderRealtimeProcessorFixtures.Scenario scenario = OrderRealtimeProcessorFixtures.scenario();
        OrderRealtimeProcessorFixtures.TrackingPendingOrderExecutionCache cache = new OrderRealtimeProcessorFixtures.TrackingPendingOrderExecutionCache();
        scenario.orders.save(1L, scenario.pendingOpenOrderAt(
                "stale-price",
                "LONG",
                99,
                Instant.parse("2026-04-27T00:00:00Z")
        ));
        scenario.orders.save(1L, scenario.pendingOpenOrderAt(
                "later-candidate",
                "LONG",
                98,
                Instant.parse("2026-04-27T00:00:01Z")
        ));
        scenario.orders = new ReloadingPriceOrderRepository(scenario.orders, "stale-price", 80);

        scenario.pendingFillProcessor(cache)
                .fillExecutablePendingOrders(scenario.marketEvent(101, 97, 97));

        FuturesOrder stale = scenario.orders.findByMemberIdAndOrderId(1L, "stale-price").orElseThrow();
        assertEquals(FuturesOrder.STATUS_PENDING, stale.status());
        assertEquals(80, stale.limitPrice(), 0.0001);
        assertTrue(scenario.events.tradingEvents().isEmpty());
        assertTrue(scenario.positions.findOpenPosition(1L, "BTCUSDT", "LONG", "ISOLATED").isEmpty());
        assertEquals(java.util.List.of("stale-price"), cache.evictedOrderIds());
    }


    @Test
    void skipsCandidateThatDisappearedBeforeReloadAndEvictsCache() {
        OrderRealtimeProcessorFixtures.Scenario scenario = OrderRealtimeProcessorFixtures.scenario();
        OrderRealtimeProcessorFixtures.TrackingPendingOrderExecutionCache cache = new OrderRealtimeProcessorFixtures.TrackingPendingOrderExecutionCache();
        scenario.orders.save(1L, scenario.pendingOpenOrderAt(
                "missing-order",
                "LONG",
                99,
                Instant.parse("2026-04-27T00:00:00Z")
        ));
        scenario.orders = new MissingReloadOrderRepository(scenario.orders, "missing-order");

        scenario.pendingFillProcessor(cache)
                .fillExecutablePendingOrders(scenario.marketEvent(101, 98, 98));

        assertTrue(scenario.events.tradingEvents().isEmpty());
        assertTrue(scenario.positions.findOpenPosition(1L, "BTCUSDT", "LONG", "ISOLATED").isEmpty());
        assertEquals(java.util.List.of("missing-order"), cache.evictedOrderIds());
    }

    @Test
    void fillsCloseOrderThroughCloseBranchAndCancelsProtectiveOrdersWhenPositionIsClosed() {
        OrderRealtimeProcessorFixtures.Scenario scenario = OrderRealtimeProcessorFixtures.scenario();
        scenario.positions.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                100,
                100
        ));
        scenario.orders.save(1L, scenario.pendingCloseOrderAt(
                "close-long",
                "LONG",
                1,
                105,
                Instant.parse("2026-04-27T00:00:00Z")
        ));
        scenario.orders.save(1L, FuturesOrder.conditionalClose(
                "tp-close",
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                110,
                FuturesOrder.TRIGGER_TYPE_TAKE_PROFIT,
                null
        ));

        scenario.pendingFillProcessor()
                .fillExecutablePendingOrders(scenario.marketEvent(100, 105, 105));

        assertEquals(FuturesOrder.STATUS_FILLED, scenario.orders.findByMemberIdAndOrderId(1L, "close-long")
                .orElseThrow()
                .status());
        assertTrue(scenario.positions.findOpenPosition(1L, "BTCUSDT", "LONG", "ISOLATED").isEmpty());
        assertEquals(FuturesOrder.STATUS_CANCELLED, scenario.orders.findByMemberIdAndOrderId(1L, "tp-close")
                .orElseThrow()
                .status());
        assertEquals(1, scenario.events.tradingEvents().size());
        assertEquals("ORDER_FILLED", scenario.events.tradingEvents().get(0).type());
    }

    private static final class MissingReloadOrderRepository extends OrderRealtimeProcessorFixtures.InMemoryOrderRepository {
        private final OrderRealtimeProcessorFixtures.InMemoryOrderRepository delegate;
        private final String missingOrderId;

        private MissingReloadOrderRepository(
                OrderRealtimeProcessorFixtures.InMemoryOrderRepository delegate,
                String missingOrderId
        ) {
            this.delegate = delegate;
            this.missingOrderId = missingOrderId;
        }

        @Override
        public java.util.List<coin.coinzzickmock.feature.order.application.result.PendingOrderCandidate> findExecutablePendingLimitOrders(
                String symbol,
                double lowerPrice,
                double upperPrice,
                boolean sellSide
        ) {
            return delegate.findExecutablePendingLimitOrders(symbol, lowerPrice, upperPrice, sellSide);
        }

        @Override
        public java.util.Optional<FuturesOrder> findByMemberIdAndOrderId(Long memberId, String orderId) {
            if (missingOrderId.equals(orderId)) {
                return java.util.Optional.empty();
            }
            return delegate.findByMemberIdAndOrderId(memberId, orderId);
        }

        @Override
        java.util.List<String> limitClaimAttempts() {
            return delegate.limitClaimAttempts();
        }
    }

    private static final class ReloadingPriceOrderRepository extends OrderRealtimeProcessorFixtures.InMemoryOrderRepository {
        private final OrderRealtimeProcessorFixtures.InMemoryOrderRepository delegate;
        private final String orderId;
        private final double replacementLimitPrice;
        private boolean reloaded;

        private ReloadingPriceOrderRepository(
                OrderRealtimeProcessorFixtures.InMemoryOrderRepository delegate,
                String orderId,
                double replacementLimitPrice
        ) {
            this.delegate = delegate;
            this.orderId = orderId;
            this.replacementLimitPrice = replacementLimitPrice;
        }

        @Override
        public java.util.List<coin.coinzzickmock.feature.order.application.result.PendingOrderCandidate> findExecutablePendingLimitOrders(
                String symbol,
                double lowerPrice,
                double upperPrice,
                boolean sellSide
        ) {
            java.util.List<coin.coinzzickmock.feature.order.application.result.PendingOrderCandidate> candidates = delegate.findExecutablePendingLimitOrders(
                    symbol,
                    lowerPrice,
                    upperPrice,
                    sellSide
            );
            if (!reloaded) {
                reloaded = true;
                delegate.findByMemberIdAndOrderId(1L, orderId)
                        .map(current -> current.withLimitPrice(replacementLimitPrice, current.feeType(), current.estimatedFee(), replacementLimitPrice))
                        .ifPresent(updated -> delegate.save(1L, updated));
            }
            return candidates;
        }

        @Override
        public FuturesOrder save(Long memberId, FuturesOrder futuresOrder) {
            return delegate.save(memberId, futuresOrder);
        }

        @Override
        public java.util.List<FuturesOrder> findByMemberId(Long memberId) {
            return delegate.findByMemberId(memberId);
        }

        @Override
        public java.util.Optional<FuturesOrder> findByMemberIdAndOrderId(Long memberId, String orderId) {
            return delegate.findByMemberIdAndOrderId(memberId, orderId);
        }

        @Override
        public java.util.List<coin.coinzzickmock.feature.order.application.result.PendingOrderCandidate> findPendingBySymbol(String symbol) {
            return delegate.findPendingBySymbol(symbol);
        }

        @Override
        public java.util.Optional<FuturesOrder> claimPendingFill(
                Long memberId,
                String orderId,
                double executionPrice,
                String feeType,
                double estimatedFee
        ) {
            return delegate.claimPendingFill(memberId, orderId, executionPrice, feeType, estimatedFee);
        }

        @Override
        public java.util.Optional<FuturesOrder> claimPendingLimitFill(
                Long memberId,
                String orderId,
                double expectedLimitPrice,
                double executionPrice,
                String feeType,
                double estimatedFee
        ) {
            return delegate.claimPendingLimitFill(memberId, orderId, expectedLimitPrice, executionPrice, feeType, estimatedFee);
        }

        @Override
        public FuturesOrder updateStatus(Long memberId, String orderId, String status) {
            return delegate.updateStatus(memberId, orderId, status);
        }

        @Override
        public FuturesOrder updateQuantityAndStatus(Long memberId, String orderId, double quantity, String status) {
            return delegate.updateQuantityAndStatus(memberId, orderId, quantity, status);
        }

        @Override
        java.util.List<String> limitClaimAttempts() {
            return delegate.limitClaimAttempts();
        }
    }
}
