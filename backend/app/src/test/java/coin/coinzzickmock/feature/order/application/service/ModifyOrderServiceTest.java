package coin.coinzzickmock.feature.order.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketDataStore;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketPriceReader;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketTickerUpdate;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketTradeTick;
import coin.coinzzickmock.feature.order.application.command.ModifyOrderCommand;
import coin.coinzzickmock.feature.order.application.implement.OrderEditFillHandler;
import coin.coinzzickmock.feature.order.application.implement.OrderMutationLock;
import coin.coinzzickmock.feature.order.application.implement.OrderEditPlanner;
import coin.coinzzickmock.feature.order.application.realtime.PendingLimitOrderBook;
import coin.coinzzickmock.feature.order.application.result.ModifyOrderResult;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.order.domain.OrderPlacementDecision;
import coin.coinzzickmock.feature.order.domain.OrderPlacementPolicy;
import coin.coinzzickmock.testsupport.TestAccountRepository;
import coin.coinzzickmock.testsupport.TestOrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ModifyOrderServiceTest {
    @Test
    void modifiesPendingOpenLimitOrderPrice() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        orderRepository.save(1L, pendingOpenOrder("open-order", 90));

        ModifyOrderResult result = service(orderRepository, 100).modify(modifyCommand(1L, "open-order", 95));

        FuturesOrder updated = orderRepository.findByMemberIdAndOrderId(1L, "open-order").orElseThrow();
        assertEquals("open-order", result.orderId());
        assertEquals(FuturesOrder.STATUS_PENDING, result.status());
        assertBigDecimalEquals(95, result.limitPrice());
        assertEquals(95, updated.limitPrice(), 0.0001);
        assertEquals("MAKER", updated.feeType());
        assertEquals(0.001425, updated.estimatedFee(), 0.000001);
        assertEquals(95, updated.executionPrice(), 0.0001);
    }

    @Test
    void modifiesPendingCloseLimitOrderPriceAndKeepsEstimatedFeeZero() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        orderRepository.save(1L, FuturesOrder.place(
                "close-order",
                "BTCUSDT",
                "LONG",
                "LIMIT",
                FuturesOrder.PURPOSE_CLOSE_POSITION,
                "ISOLATED",
                10,
                0.2,
                110.0,
                false,
                "MAKER",
                0,
                110
        ));

        service(orderRepository, 100).modify(modifyCommand(1L, "close-order", 108));

        FuturesOrder updated = orderRepository.findByMemberIdAndOrderId(1L, "close-order").orElseThrow();
        assertEquals(108, updated.limitPrice(), 0.0001);
        assertEquals(0, updated.estimatedFee(), 0.0001);
        assertEquals(108, updated.executionPrice(), 0.0001);
    }

    @Test
    void rejectsNonPendingOrderModification() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        orderRepository.save(1L, pendingOpenOrder("filled-order", 90).fill(90, "MAKER", 0.0135));

        assertThrows(CoreException.class, () -> service(orderRepository, 100)
                .modify(modifyCommand(1L, "filled-order", 95)));
    }

    @Test
    void rejectsConditionalOrderModification() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        orderRepository.save(1L, FuturesOrder.conditionalClose(
                "take-profit",
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                0.2,
                120,
                FuturesOrder.TRIGGER_TYPE_TAKE_PROFIT,
                "oco-1"
        ));

        assertThrows(CoreException.class, () -> service(orderRepository, 100)
                .modify(modifyCommand(1L, "take-profit", 118)));
    }

    @Test
    void fillsMarketableLimitPriceModification() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        orderRepository.save(1L, pendingOpenOrder("open-order", 90));

        ModifyOrderResult result = service(orderRepository, 100)
                .modify(modifyCommand(1L, "open-order", 101));

        FuturesOrder filled = orderRepository.findByMemberIdAndOrderId(1L, "open-order").orElseThrow();
        assertEquals(FuturesOrder.STATUS_FILLED, result.status());
        assertEquals(FuturesOrder.STATUS_FILLED, filled.status());
        assertEquals(101, filled.limitPrice(), 0.0001);
        assertEquals("TAKER", filled.feeType());
        assertEquals(0.005, filled.estimatedFee(), 0.000001);
        assertEquals(100, filled.executionPrice(), 0.0001);
    }

    @Test
    void fillsWhenLatestPriceMovesIntoEditedLimitAfterUpdate() {
        RealtimeMarketDataStore marketDataStore = realtimeMarketStore(100);
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository() {
            @Override
            public Optional<FuturesOrder> updatePendingLimitPrice(
                    Long memberId,
                    String orderId,
                    double limitPrice,
                    String feeType,
                    double estimatedFee,
                    double executionPrice
            ) {
                Optional<FuturesOrder> updated = super.updatePendingLimitPrice(
                        memberId,
                        orderId,
                        limitPrice,
                        feeType,
                        estimatedFee,
                        executionPrice
                );
                if (updated.isPresent() && Double.compare(limitPrice, 95.0) == 0) {
                    seedRealtimeMarket(marketDataStore, 94);
                }
                return updated;
            }
        };
        orderRepository.save(1L, pendingOpenOrder("open-order", 90));

        ModifyOrderResult result = service(orderRepository, marketDataStore)
                .modify(modifyCommand(1L, "open-order", 95));

        FuturesOrder filled = orderRepository.findByMemberIdAndOrderId(1L, "open-order").orElseThrow();
        assertEquals(FuturesOrder.STATUS_FILLED, result.status());
        assertEquals(FuturesOrder.STATUS_FILLED, filled.status());
        assertEquals(95, filled.limitPrice(), 0.0001);
        assertEquals("TAKER", filled.feeType());
        assertEquals(0.0047, filled.estimatedFee(), 0.000001);
        assertEquals(94, filled.executionPrice(), 0.0001);
    }

    @Test
    void rejectsNullOrNonPositiveLimitPriceModification() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        orderRepository.save(1L, pendingOpenOrder("open-order", 90));

        assertThrows(CoreException.class, () -> service(orderRepository, 100)
                .modify(new ModifyOrderCommand(1L, "open-order", null)));
        assertThrows(CoreException.class, () -> service(orderRepository, 100)
                .modify(modifyCommand(1L, "open-order", 0)));
        assertThrows(CoreException.class, () -> service(orderRepository, 100)
                .modify(modifyCommand(1L, "open-order", -1)));
        assertThrows(CoreException.class, () -> service(orderRepository, 100)
                .modify(new ModifyOrderCommand(1L, "open-order", new BigDecimal("1e-4000"))));
    }

    @Test
    void rejectsModificationForDifferentMember() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        orderRepository.save(1L, pendingOpenOrder("member-order", 90));

        assertThrows(CoreException.class, () -> service(orderRepository, 100)
                .modify(modifyCommand(2L, "member-order", 95)));

        FuturesOrder unchanged = orderRepository.findByMemberIdAndOrderId(1L, "member-order").orElseThrow();
        assertEquals(90, unchanged.limitPrice(), 0.0001);
    }

    private static ModifyOrderCommand modifyCommand(Long memberId, String orderId, double limitPrice) {
        return new ModifyOrderCommand(memberId, orderId, BigDecimal.valueOf(limitPrice));
    }

    private static void assertBigDecimalEquals(double expected, BigDecimal actual) {
        assertEquals(0, BigDecimal.valueOf(expected).compareTo(actual));
    }

    private static FuturesOrder pendingOpenOrder(String orderId, double limitPrice) {
        return FuturesOrder.place(
                orderId,
                "BTCUSDT",
                "LONG",
                "LIMIT",
                FuturesOrder.PURPOSE_OPEN_POSITION,
                "ISOLATED",
                10,
                0.1,
                limitPrice,
                false,
                "MAKER",
                limitPrice * 0.1 * 0.00015,
                limitPrice
        );
    }

    private static ModifyOrderService service(InMemoryOrderRepository orderRepository, double lastPrice) {
        return service(orderRepository, realtimeMarketStore(lastPrice));
    }

    private static ModifyOrderService service(
            InMemoryOrderRepository orderRepository,
            RealtimeMarketDataStore realtimeMarketDataStore
    ) {
        OrderPlacementPolicy orderPlacementPolicy = new OrderPlacementPolicy();
        return new ModifyOrderService(
                orderRepository,
                new RealtimeMarketPriceReader(realtimeMarketDataStore),
                orderPlacementPolicy,
                new OrderMutationLock(new LockingAccountRepository()),
                new OrderEditPlanner(),
                new ClaimOnlyOrderEditFillHandler(orderRepository),
                new PendingLimitOrderBook()
        );
    }

    private static RealtimeMarketDataStore realtimeMarketStore(double lastPrice) {
        RealtimeMarketDataStore store = new RealtimeMarketDataStore();
        seedRealtimeMarket(store, lastPrice);
        return store;
    }

    private static void seedRealtimeMarket(RealtimeMarketDataStore store, double lastPrice) {
        Instant now = Instant.now();
        store.acceptTrade(new RealtimeMarketTradeTick(
                "BTCUSDT",
                "trade-" + lastPrice + "-" + System.nanoTime(),
                BigDecimal.valueOf(lastPrice),
                BigDecimal.ONE,
                "buy",
                now,
                now
        ));
        store.acceptTicker(new RealtimeMarketTickerUpdate(
                "BTCUSDT",
                BigDecimal.valueOf(lastPrice),
                BigDecimal.valueOf(lastPrice),
                BigDecimal.valueOf(lastPrice),
                BigDecimal.valueOf(0.0001),
                now.plusSeconds(3600),
                now,
                now
        ));
    }

    private static class InMemoryOrderRepository extends TestOrderRepository {
        private final List<MemberOrder> orders = new ArrayList<>();

        @Override
        public FuturesOrder save(Long memberId, FuturesOrder futuresOrder) {
            orders.removeIf(order -> order.memberId().equals(memberId)
                    && order.futuresOrder().orderId().equals(futuresOrder.orderId()));
            orders.add(new MemberOrder(memberId, futuresOrder));
            return futuresOrder;
        }

        @Override
        public List<FuturesOrder> findByMemberId(Long memberId) {
            return orders.stream()
                    .filter(order -> order.memberId().equals(memberId))
                    .map(MemberOrder::futuresOrder)
                    .toList();
        }

        @Override
        public Optional<FuturesOrder> findByMemberIdAndOrderId(Long memberId, String orderId) {
            return orders.stream()
                    .filter(order -> order.memberId().equals(memberId))
                    .map(MemberOrder::futuresOrder)
                    .filter(order -> order.orderId().equals(orderId))
                    .findFirst();
        }

        @Override
        public Optional<FuturesOrder> updatePendingLimitPrice(
                Long memberId,
                String orderId,
                double limitPrice,
                String feeType,
                double estimatedFee,
                double executionPrice
        ) {
            FuturesOrder order = findByMemberIdAndOrderId(memberId, orderId)
                    .filter(FuturesOrder::isPending)
                    .filter(found -> FuturesOrder.TYPE_LIMIT.equalsIgnoreCase(found.orderType()))
                    .filter(found -> !found.isConditionalOrder())
                    .orElse(null);
            if (order == null) {
                return Optional.empty();
            }
            FuturesOrder updated = order.withLimitPrice(limitPrice, feeType, estimatedFee, executionPrice);
            save(memberId, updated);
            return Optional.of(updated);
        }

        @Override
        public Optional<FuturesOrder> claimPendingLimitFill(
                Long memberId,
                String orderId,
                double expectedLimitPrice,
                double executionPrice,
                String feeType,
                double estimatedFee
        ) {
            FuturesOrder order = findByMemberIdAndOrderId(memberId, orderId)
                    .filter(FuturesOrder::isPending)
                    .filter(found -> FuturesOrder.TYPE_LIMIT.equalsIgnoreCase(found.orderType()))
                    .filter(found -> !found.isConditionalOrder())
                    .filter(found -> Double.compare(found.limitPrice(), expectedLimitPrice) == 0)
                    .orElse(null);
            if (order == null) {
                return Optional.empty();
            }
            FuturesOrder filled = order.fill(executionPrice, feeType, estimatedFee);
            save(memberId, filled);
            return Optional.of(filled);
        }

        private record MemberOrder(Long memberId, FuturesOrder futuresOrder) {
        }
    }

    private static class ClaimOnlyOrderEditFillHandler extends OrderEditFillHandler {
        private final InMemoryOrderRepository orderRepository;

        private ClaimOnlyOrderEditFillHandler(InMemoryOrderRepository orderRepository) {
            super(null, null, null, null, null, null, null, new PendingLimitOrderBook());
            this.orderRepository = orderRepository;
        }

        @Override
        public FuturesOrder fill(
                Long memberId,
                FuturesOrder order,
                MarketSnapshot market,
                OrderPlacementDecision decision
        ) {
            return orderRepository.claimPendingLimitFill(
                    memberId,
                    order.orderId(),
                    order.limitPrice(),
                    decision.executionPrice(),
                    decision.feeType(),
                    decision.estimatedFee(order.quantity())
            ).orElseThrow(() -> new CoreException(coin.coinzzickmock.common.error.ErrorCode.INVALID_REQUEST));
        }
    }

    private static class LockingAccountRepository extends TestAccountRepository {
        @Override
        public Optional<TradingAccount> findByMemberIdForUpdate(Long memberId) {
            return Optional.of(new TradingAccount(memberId, "member@example.com", "member", 100000, 100000));
        }
    }
}
