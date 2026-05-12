package coin.coinzzickmock.feature.position.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.application.result.AccountMutationResult;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketDataStore;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketPriceReader;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketTickerUpdate;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketTradeTick;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.result.PendingOrderCandidate;
import coin.coinzzickmock.feature.order.application.service.AccountOrderMutationLock;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.position.application.close.PendingCloseOrderCapReconciler;
import coin.coinzzickmock.feature.position.application.query.PositionSnapshotResultAssembler;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.result.OpenPositionCandidate;
import coin.coinzzickmock.feature.position.application.result.PositionSnapshotResult;
import coin.coinzzickmock.feature.position.domain.LiquidationPolicy;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UpdatePositionTpslServiceTest {
    @Test
    void createsTakeProfitAndStopLossOrdersWhenPricesAreNotAlreadyBreached() {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        positionRepository.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                100,
                100
        ));
        UpdatePositionTpslService service = service(positionRepository, orderRepository, 101);

        PositionSnapshotResult result = service.update(
                1L,
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                110.0,
                95.0
        );

        assertEquals(110, result.takeProfitPrice(), 0.0001);
        assertEquals(95, result.stopLossPrice(), 0.0001);
        PositionSnapshot persisted = positionRepository.findOpenPosition(1L, "BTCUSDT", "LONG", "ISOLATED")
                .orElseThrow();
        assertEquals(null, persisted.takeProfitPrice());
        assertEquals(null, persisted.stopLossPrice());
        assertEquals(0, persisted.version());
        List<FuturesOrder> orders = orderRepository.findPendingConditionalCloseOrders(
                1L,
                "BTCUSDT",
                "LONG",
                "ISOLATED"
        );
        assertEquals(2, orders.size());
        assertEquals(1, orders.stream().map(FuturesOrder::ocoGroupId).distinct().count());
    }

    @Test
    void updatesExistingTpslOrdersInPlaceWithoutCreatingHistoryRows() {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        positionRepository.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                100,
                100
        ));
        orderRepository.save(1L, FuturesOrder.conditionalClose(
                "tp",
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                110,
                FuturesOrder.TRIGGER_TYPE_TAKE_PROFIT,
                "oco-1"
        ));
        orderRepository.save(1L, FuturesOrder.conditionalClose(
                "sl",
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                95,
                FuturesOrder.TRIGGER_TYPE_STOP_LOSS,
                "oco-1"
        ));
        UpdatePositionTpslService service = service(positionRepository, orderRepository, 101);

        PositionSnapshotResult result = service.update(
                1L,
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                115.0,
                96.0
        );

        assertEquals(115, result.takeProfitPrice(), 0.0001);
        assertEquals(96, result.stopLossPrice(), 0.0001);
        assertEquals(2, orderRepository.findByMemberId(1L).size());
        FuturesOrder takeProfit = orderRepository.findByMemberIdAndOrderId(1L, "tp").orElseThrow();
        FuturesOrder stopLoss = orderRepository.findByMemberIdAndOrderId(1L, "sl").orElseThrow();
        assertEquals(FuturesOrder.STATUS_PENDING, takeProfit.status());
        assertEquals(FuturesOrder.STATUS_PENDING, stopLoss.status());
        assertEquals(115, takeProfit.triggerPrice(), 0.0001);
        assertEquals(96, stopLoss.triggerPrice(), 0.0001);
        assertEquals("oco-1", takeProfit.ocoGroupId());
        assertEquals("oco-1", stopLoss.ocoGroupId());
    }

    @Test
    void savingSameTpslValueIsNoopForExistingOrder() {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        positionRepository.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                100,
                100
        ));
        orderRepository.save(1L, FuturesOrder.conditionalClose(
                "tp",
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                110,
                FuturesOrder.TRIGGER_TYPE_TAKE_PROFIT,
                null
        ));
        UpdatePositionTpslService service = service(positionRepository, orderRepository, 101);

        PositionSnapshotResult result = service.update(1L, "BTCUSDT", "LONG", "ISOLATED", 110.0, null);

        assertEquals(110, result.takeProfitPrice(), 0.0001);
        assertEquals(null, result.stopLossPrice());
        assertEquals(1, orderRepository.findByMemberId(1L).size());
        FuturesOrder takeProfit = orderRepository.findByMemberIdAndOrderId(1L, "tp").orElseThrow();
        assertEquals(FuturesOrder.STATUS_PENDING, takeProfit.status());
        assertEquals(110, takeProfit.triggerPrice(), 0.0001);
        assertEquals(null, takeProfit.ocoGroupId());
    }

    @Test
    void removingOneTpslSideCancelsOnlyThatSideAndKeepsOtherOrderId() {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        positionRepository.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                100,
                100
        ));
        orderRepository.save(1L, FuturesOrder.conditionalClose(
                "tp",
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                110,
                FuturesOrder.TRIGGER_TYPE_TAKE_PROFIT,
                "oco-1"
        ));
        orderRepository.save(1L, FuturesOrder.conditionalClose(
                "sl",
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                95,
                FuturesOrder.TRIGGER_TYPE_STOP_LOSS,
                "oco-1"
        ));
        UpdatePositionTpslService service = service(positionRepository, orderRepository, 101);

        PositionSnapshotResult result = service.update(1L, "BTCUSDT", "LONG", "ISOLATED", 112.0, null);

        assertEquals(112, result.takeProfitPrice(), 0.0001);
        assertEquals(null, result.stopLossPrice());
        FuturesOrder takeProfit = orderRepository.findByMemberIdAndOrderId(1L, "tp").orElseThrow();
        FuturesOrder stopLoss = orderRepository.findByMemberIdAndOrderId(1L, "sl").orElseThrow();
        assertEquals(FuturesOrder.STATUS_PENDING, takeProfit.status());
        assertEquals(112, takeProfit.triggerPrice(), 0.0001);
        assertEquals(null, takeProfit.ocoGroupId());
        assertEquals(FuturesOrder.STATUS_CANCELLED, stopLoss.status());
        assertTrue(orderRepository.findPendingConditionalCloseOrders(1L, "BTCUSDT", "LONG", "ISOLATED")
                .stream()
                .allMatch(FuturesOrder::isTakeProfitOrder));
    }

    @Test
    void savingTpslWithFullSizeManualCloseKeepsProtectiveOrdersPendingAndManualCloseableOnly() {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        positionRepository.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                100,
                100
        ));
        orderRepository.save(1L, FuturesOrder.place(
                "manual-close",
                "BTCUSDT",
                "LONG",
                "LIMIT",
                FuturesOrder.PURPOSE_CLOSE_POSITION,
                "ISOLATED",
                10,
                1,
                120.0,
                false,
                "MAKER",
                0,
                120
        ));
        UpdatePositionTpslService service = service(positionRepository, orderRepository, 101);

        PositionSnapshotResult result = service.update(
                1L,
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                110.0,
                95.0
        );

        assertEquals(1, result.pendingCloseQuantity(), 0.0001);
        assertEquals(0, result.closeableQuantity(), 0.0001);
        assertEquals(110, result.takeProfitPrice(), 0.0001);
        assertEquals(95, result.stopLossPrice(), 0.0001);
        assertEquals(2, orderRepository.findPendingConditionalCloseOrders(
                1L,
                "BTCUSDT",
                "LONG",
                "ISOLATED"
        ).size());
        assertEquals(FuturesOrder.STATUS_PENDING,
                orderRepository.findByMemberIdAndOrderId(1L, "manual-close").orElseThrow().status());
    }

    @Test
    void clearingTpslCancelsProtectiveOrdersWithoutCancellingManualCloseOrders() {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        positionRepository.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                100,
                100
        ));
        orderRepository.save(1L, FuturesOrder.place(
                "manual-close",
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
        ));
        orderRepository.save(1L, FuturesOrder.conditionalClose(
                "tp",
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                110,
                FuturesOrder.TRIGGER_TYPE_TAKE_PROFIT,
                "oco-1"
        ));
        orderRepository.save(1L, FuturesOrder.conditionalClose(
                "sl",
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                95,
                FuturesOrder.TRIGGER_TYPE_STOP_LOSS,
                "oco-1"
        ));
        UpdatePositionTpslService service = service(positionRepository, orderRepository, 101);

        PositionSnapshotResult result = service.update(1L, "BTCUSDT", "LONG", "ISOLATED", null, null);

        assertEquals(null, result.takeProfitPrice());
        assertEquals(null, result.stopLossPrice());
        assertEquals(0.4, result.pendingCloseQuantity(), 0.0001);
        assertEquals(0.6, result.closeableQuantity(), 0.0001);
        assertEquals(FuturesOrder.STATUS_PENDING,
                orderRepository.findByMemberIdAndOrderId(1L, "manual-close").orElseThrow().status());
        assertEquals(FuturesOrder.STATUS_CANCELLED,
                orderRepository.findByMemberIdAndOrderId(1L, "tp").orElseThrow().status());
        assertEquals(FuturesOrder.STATUS_CANCELLED,
                orderRepository.findByMemberIdAndOrderId(1L, "sl").orElseThrow().status());
    }

    @Test
    void rejectsAlreadyBreachedLongTakeProfit() {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        positionRepository.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                100,
                100
        ));

        assertThrows(CoreException.class, () -> service(positionRepository, new InMemoryOrderRepository(), 101).update(
                1L,
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                100.0,
                null
        ));
    }

    @Test
    void rejectsAlreadyBreachedShortStopLoss() {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        positionRepository.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "SHORT",
                "ISOLATED",
                10,
                1,
                100,
                100
        ));

        assertThrows(CoreException.class, () -> service(positionRepository, new InMemoryOrderRepository(), 101).update(
                1L,
                "BTCUSDT",
                "SHORT",
                "ISOLATED",
                null,
                100.0
        ));
    }

    @Test
    void crossTpslResponseIncludesDynamicLiquidationEstimate() {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        positionRepository.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "CROSS",
                10,
                1,
                100,
                100
        ));

        PositionSnapshotResult result = service(positionRepository, orderRepository, 100, 50)
                .update(1L, "BTCUSDT", "LONG", "CROSS", 120.0, null);

        assertEquals(50.2512562814, result.liquidationPrice(), 0.0001);
        assertEquals("EXACT", result.liquidationPriceType());
    }

    private UpdatePositionTpslService service(
            InMemoryPositionRepository positionRepository,
            InMemoryOrderRepository orderRepository,
            double markPrice
    ) {
        return service(positionRepository, orderRepository, markPrice, 100_000);
    }

    private UpdatePositionTpslService service(
            InMemoryPositionRepository positionRepository,
            InMemoryOrderRepository orderRepository,
            double markPrice,
            double walletBalance
    ) {
        AccountRepository accountRepository = accountRepository(walletBalance);
        return new UpdatePositionTpslService(
                positionRepository,
                orderRepository,
                new PendingCloseOrderCapReconciler(orderRepository),
                realtimePriceReader(markPrice),
                new AccountOrderMutationLock(accountRepository),
                accountRepository,
                new PositionSnapshotResultAssembler(
                        positionRepository,
                        orderRepository,
                        accountRepository,
                        new PendingCloseOrderCapReconciler(orderRepository),
                        new LiquidationPolicy()
                )
        );
    }

    private static AccountRepository accountRepository() {
        return accountRepository(100_000);
    }

    private static AccountRepository accountRepository(double walletBalance) {
        return new coin.coinzzickmock.testsupport.TestAccountRepository() {
            @Override
            public Optional<TradingAccount> findByMemberId(Long memberId) {
                return Optional.of(new TradingAccount(
                        memberId,
                        "demo@coinzzickmock.dev",
                        "Demo",
                        walletBalance,
                        100_000
                ));
            }

            @Override
            public Optional<TradingAccount> findByMemberIdForUpdate(Long memberId) {
                return findByMemberId(memberId);
            }

            @Override
            public TradingAccount create(TradingAccount account) {
                return account;
            }

            @Override
            public AccountMutationResult updateWithVersion(
                    TradingAccount expectedAccount,
                    TradingAccount nextAccount
            ) {
                return AccountMutationResult.updated(1, nextAccount.withVersion(expectedAccount.version() + 1));
            }
        };
    }

    private static class InMemoryPositionRepository extends coin.coinzzickmock.testsupport.TestPositionRepository {
        private final List<OpenPositionCandidate> positions = new ArrayList<>();

        @Override
        public List<PositionSnapshot> findOpenPositions(Long memberId) {
            return positions.stream()
                    .filter(candidate -> candidate.memberId().equals(memberId))
                    .map(OpenPositionCandidate::position)
                    .toList();
        }

        @Override
        public Optional<PositionSnapshot> findOpenPosition(
                Long memberId,
                String symbol,
                String positionSide,
                String marginMode
        ) {
            return positions.stream()
                    .filter(candidate -> candidate.memberId().equals(memberId))
                    .map(OpenPositionCandidate::position)
                    .filter(position -> position.symbol().equals(symbol))
                    .filter(position -> position.positionSide().equals(positionSide))
                    .filter(position -> position.marginMode().equals(marginMode))
                    .findFirst();
        }

        @Override
        public List<OpenPositionCandidate> findOpenBySymbol(String symbol) {
            return positions.stream()
                    .filter(candidate -> candidate.symbol().equals(symbol))
                    .toList();
        }

        @Override
        public PositionSnapshot save(Long memberId, PositionSnapshot positionSnapshot) {
            delete(memberId, positionSnapshot.symbol(), positionSnapshot.positionSide(), positionSnapshot.marginMode());
            positions.add(new OpenPositionCandidate(memberId, positionSnapshot));
            return positionSnapshot;
        }

        @Override
        public boolean deleteIfOpen(Long memberId, String symbol, String positionSide, String marginMode) {
            int before = positions.size();
            positions.removeIf(candidate -> candidate.memberId().equals(memberId)
                    && candidate.symbol().equals(symbol)
                    && candidate.positionSide().equals(positionSide)
                    && candidate.marginMode().equals(marginMode));
            return before != positions.size();
        }

        @Override
        public void delete(Long memberId, String symbol, String positionSide, String marginMode) {
            deleteIfOpen(memberId, symbol, positionSide, marginMode);
        }
    }

    private static class InMemoryOrderRepository extends coin.coinzzickmock.testsupport.TestOrderRepository {
        private final List<FuturesOrder> orders = new ArrayList<>();

        @Override
        public FuturesOrder save(Long memberId, FuturesOrder futuresOrder) {
            orders.add(futuresOrder);
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
            FuturesOrder order = findByMemberIdAndOrderId(memberId, orderId).orElseThrow();
            orders.remove(order);
            FuturesOrder updated = FuturesOrder.STATUS_CANCELLED.equals(status) ? order.cancel() : order;
            orders.add(updated);
            return updated;
        }

        @Override
        public FuturesOrder updatePendingConditionalCloseOrder(
                Long memberId,
                String orderId,
                int leverage,
                double quantity,
                double triggerPrice,
                String ocoGroupId
        ) {
            FuturesOrder order = findByMemberIdAndOrderId(memberId, orderId).orElseThrow();
            orders.remove(order);
            FuturesOrder updated = order.withConditionalCloseTarget(leverage, quantity, triggerPrice, ocoGroupId);
            orders.add(updated);
            return updated;
        }

        @Override
        public FuturesOrder updateQuantityAndStatus(Long memberId, String orderId, double quantity, String status) {
            FuturesOrder order = findByMemberIdAndOrderId(memberId, orderId).orElseThrow();
            orders.remove(order);
            FuturesOrder updated = order.withQuantity(quantity);
            if (FuturesOrder.STATUS_CANCELLED.equals(status)) {
                updated = updated.cancel();
            }
            orders.add(updated);
            return updated;
        }
    }

    private static RealtimeMarketPriceReader realtimePriceReader(double markPrice) {
        RealtimeMarketDataStore store = new RealtimeMarketDataStore();
        Instant now = Instant.now();
        store.acceptTrade(new RealtimeMarketTradeTick(
                "BTCUSDT",
                "trade-" + markPrice,
                BigDecimal.valueOf(markPrice),
                BigDecimal.ONE,
                "buy",
                now,
                now
        ));
        store.acceptTicker(new RealtimeMarketTickerUpdate(
                "BTCUSDT",
                BigDecimal.valueOf(markPrice),
                BigDecimal.valueOf(markPrice),
                BigDecimal.valueOf(markPrice),
                BigDecimal.valueOf(0.0001),
                now.plusSeconds(3600),
                now,
                now
        ));
        return new RealtimeMarketPriceReader(store);
    }
}
