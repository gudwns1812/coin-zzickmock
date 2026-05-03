package coin.coinzzickmock.feature.order.application.service;

import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.application.result.AccountMutationResult;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.account.domain.WalletHistorySource;
import coin.coinzzickmock.feature.leaderboard.application.event.WalletBalanceChangedEvent;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketDataStore;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketPriceReader;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketTickerUpdate;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketTradeTick;
import coin.coinzzickmock.feature.order.application.command.CreateOrderCommand;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.result.CreateOrderResult;
import coin.coinzzickmock.feature.order.application.result.PendingOrderCandidate;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.order.domain.OrderPreview;
import coin.coinzzickmock.feature.order.domain.OrderPlacementPolicy;
import coin.coinzzickmock.feature.order.domain.OrderPreviewPolicy;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.result.OpenPositionCandidate;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateOrderServiceTest {
    @Test
    void createsFilledMarketOrderAndUpdatesPosition() {
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();

        CreateOrderService service = service(
                realtimePriceReader(),
                orderRepository,
                accountRepository,
                positionRepository,
                event -> {
                }
        );

        CreateOrderResult result = service.execute(new CreateOrderCommand(
                1L,
                "BTCUSDT",
                "LONG",
                "MARKET",
                "ISOLATED",
                10,
                0.1,
                null
        ));

        assertEquals("FILLED", result.status());
        assertEquals(1, positionRepository.findOpenPositions(1L).size());
        assertEquals(5, positionRepository.findOpenPositions(1L).get(0).accumulatedOpenFee(), 0.0001);
        assertEquals(98995, accountRepository.findByMemberId(1L).orElseThrow().availableMargin(), 0.0001);
    }

    @Test
    void recalculatesLiquidationPriceFromWeightedEntryPriceWhenIncreasingExistingPosition() {
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        positionRepository.save(1L, new PositionSnapshot(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                0.1,
                100000,
                100000,
                90000.0,
                0
        ));

        CreateOrderService service = service(
                realtimePriceReader(110000, 110000),
                orderRepository,
                accountRepository,
                positionRepository,
                event -> {
                }
        );

        service.execute(new CreateOrderCommand(
                1L,
                "BTCUSDT",
                "LONG",
                "MARKET",
                "ISOLATED",
                10,
                0.1,
                null
        ));

        PositionSnapshot updated = positionRepository.findOpenPosition(1L, "BTCUSDT", "LONG", "ISOLATED")
                .orElseThrow();
        assertEquals(105000.0, updated.entryPrice(), 0.0001);
        assertEquals(94500.0, updated.liquidationPrice(), 0.0001);
        assertEquals(5.5, updated.accumulatedOpenFee(), 0.0001);
    }

    @Test
    void previewsMakerLimitOrderWithEntryPriceAndExecutionFlag() {
        CreateOrderService service = service(
                realtimePriceReader(),
                new InMemoryOrderRepository(),
                new InMemoryAccountRepository(),
                new InMemoryPositionRepository(),
                event -> {
                }
        );

        OrderPreview preview = service.preview(new CreateOrderCommand(
                1L,
                "BTCUSDT",
                "LONG",
                "LIMIT",
                "ISOLATED",
                10,
                0.1,
                99900.0
        ));

        assertEquals("MAKER", preview.feeType());
        assertEquals(99900.0, preview.estimatedEntryPrice(), 0.0001);
        assertFalse(preview.executable());
    }

    @Test
    void marketableLongLimitUsesLatestTradePriceForPreviewOrderAccountAndPosition() {
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        CreateOrderService service = service(
                realtimePriceReader(74700, 74700),
                orderRepository,
                accountRepository,
                positionRepository,
                event -> {
                }
        );
        CreateOrderCommand command = new CreateOrderCommand(
                1L,
                "BTCUSDT",
                "LONG",
                "LIMIT",
                "ISOLATED",
                10,
                0.1,
                75000.0
        );

        OrderPreview preview = service.preview(command);
        CreateOrderResult result = service.execute(command);

        assertTrue(preview.executable());
        assertEquals("TAKER", preview.feeType());
        assertEquals(74700.0, preview.estimatedEntryPrice(), 0.0001);
        assertEquals(3.735, preview.estimatedFee(), 0.0001);
        assertEquals(747.0, preview.estimatedInitialMargin(), 0.0001);
        assertEquals(67230.0, preview.estimatedLiquidationPrice(), 0.0001);
        assertEquals("FILLED", result.status());
        assertEquals(74700.0, result.executionPrice(), 0.0001);
        assertEquals(99249.265, accountRepository.findByMemberId(1L).orElseThrow().availableMargin(), 0.0001);
        FuturesOrder saved = orderRepository.findByMemberId(1L).get(0);
        assertEquals(FuturesOrder.STATUS_FILLED, saved.status());
        assertEquals("TAKER", saved.feeType());
        assertEquals(74700.0, saved.executionPrice(), 0.0001);
        assertEquals(75000.0, saved.limitPrice(), 0.0001);
        PositionSnapshot opened = positionRepository.findOpenPosition(1L, "BTCUSDT", "LONG", "ISOLATED")
                .orElseThrow();
        assertEquals(74700.0, opened.entryPrice(), 0.0001);
        assertEquals(67230.0, opened.liquidationPrice(), 0.0001);
        assertEquals(3.735, opened.accumulatedOpenFee(), 0.0001);
    }

    @Test
    void marketableShortLimitEqualityUsesLatestTradePriceForPreviewOrderAccountAndPosition() {
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        CreateOrderService service = service(
                realtimePriceReader(75000, 75000),
                orderRepository,
                accountRepository,
                positionRepository,
                event -> {
                }
        );
        CreateOrderCommand command = new CreateOrderCommand(
                1L,
                "BTCUSDT",
                "SHORT",
                "LIMIT",
                "ISOLATED",
                10,
                0.1,
                75000.0
        );

        OrderPreview preview = service.preview(command);
        CreateOrderResult result = service.execute(command);

        assertTrue(preview.executable());
        assertEquals("TAKER", preview.feeType());
        assertEquals(75000.0, preview.estimatedEntryPrice(), 0.0001);
        assertEquals(3.75, preview.estimatedFee(), 0.0001);
        assertEquals(750.0, preview.estimatedInitialMargin(), 0.0001);
        assertEquals(82500.0, preview.estimatedLiquidationPrice(), 0.0001);
        assertEquals("FILLED", result.status());
        assertEquals(75000.0, result.executionPrice(), 0.0001);
        assertEquals(99246.25, accountRepository.findByMemberId(1L).orElseThrow().availableMargin(), 0.0001);
        FuturesOrder saved = orderRepository.findByMemberId(1L).get(0);
        assertEquals(FuturesOrder.STATUS_FILLED, saved.status());
        assertEquals("TAKER", saved.feeType());
        assertEquals(75000.0, saved.executionPrice(), 0.0001);
        assertEquals(75000.0, saved.limitPrice(), 0.0001);
        PositionSnapshot opened = positionRepository.findOpenPosition(1L, "BTCUSDT", "SHORT", "ISOLATED")
                .orElseThrow();
        assertEquals(75000.0, opened.entryPrice(), 0.0001);
        assertEquals(82500.0, opened.liquidationPrice(), 0.0001);
        assertEquals(3.75, opened.accumulatedOpenFee(), 0.0001);
    }

    @Test
    void walletBalanceChangedEventPublishesOnlyAfterTransactionCommit() {
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        CreateOrderService service = service(
                realtimePriceReader(),
                orderRepository,
                accountRepository,
                positionRepository,
                eventPublisher
        );

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.execute(new CreateOrderCommand(
                    1L,
                    "BTCUSDT",
                    "LONG",
                    "MARKET",
                    "ISOLATED",
                    10,
                    0.1,
                    null
            ));

            assertTrue(eventPublisher.events.isEmpty());

            TransactionSynchronizationUtils.triggerAfterCommit();

            assertEquals(1, eventPublisher.events.size());
            assertEquals(1L, eventPublisher.events.get(0).memberId());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void filledMarketOrderReservesPreviewInitialMarginPrecision() {
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        CreateOrderService service = service(
                realtimePriceReader(100, 100),
                orderRepository,
                accountRepository,
                positionRepository,
                event -> {
                }
        );

        service.execute(new CreateOrderCommand(
                1L,
                "BTCUSDT",
                "LONG",
                "MARKET",
                "ISOLATED",
                3,
                1,
                null
        ));

        assertEquals(
                99_966.61666667,
                accountRepository.findByMemberId(1L).orElseThrow().availableMargin(),
                0.000000001
        );
    }

    @Test
    void previewRejectsUnsupportedOrderType() {
        CreateOrderService service = service(
                realtimePriceReader(),
                new InMemoryOrderRepository(),
                new InMemoryAccountRepository(),
                new InMemoryPositionRepository(),
                event -> {
                }
        );

        CoreException thrown = assertThrows(CoreException.class, () -> service.preview(new CreateOrderCommand(
                1L,
                "BTCUSDT",
                "LONG",
                "LIMT",
                "ISOLATED",
                10,
                0.1,
                75000.0
        )));

        assertEquals(ErrorCode.INVALID_REQUEST, thrown.errorCode());
    }

    @Test
    void previewRejectsDifferentMarginModeWhenSameSidePositionExists() {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        positionRepository.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                0.1,
                100000,
                100000
        ));
        CreateOrderService service = service(
                realtimePriceReader(),
                new InMemoryOrderRepository(),
                new InMemoryAccountRepository(),
                positionRepository,
                event -> {
                }
        );

        CoreException thrown = assertThrows(CoreException.class, () -> service.preview(new CreateOrderCommand(
                1L,
                "BTCUSDT",
                "LONG",
                "LIMIT",
                "CROSS",
                10,
                0.1,
                99900.0
        )));

        assertEquals(ErrorCode.INVALID_REQUEST, thrown.errorCode());
    }

    @Test
    void createRejectsDifferentLeverageWhenSameSidePositionExists() {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        positionRepository.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                0.1,
                100000,
                100000
        ));
        CreateOrderService service = service(
                realtimePriceReader(),
                new InMemoryOrderRepository(),
                new InMemoryAccountRepository(),
                positionRepository,
                event -> {
                }
        );

        CoreException thrown = assertThrows(CoreException.class, () -> service.execute(new CreateOrderCommand(
                1L,
                "BTCUSDT",
                "LONG",
                "MARKET",
                "ISOLATED",
                20,
                0.1,
                null
        )));

        assertEquals(ErrorCode.INVALID_REQUEST, thrown.errorCode());
    }

    private static CreateOrderService service(
            RealtimeMarketPriceReader realtimeMarketPriceReader,
            OrderRepository orderRepository,
            AccountRepository accountRepository,
            PositionRepository positionRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        AfterCommitEventPublisher afterCommitEventPublisher = new AfterCommitEventPublisher(eventPublisher);
        return new CreateOrderService(
                new OrderPreviewPolicy(),
                new OrderPlacementPolicy(),
                realtimeMarketPriceReader,
                orderRepository,
                positionRepository,
                new FilledOpenOrderApplier(accountRepository, positionRepository, afterCommitEventPublisher)
        );
    }

    private static class CapturingEventPublisher implements ApplicationEventPublisher {
        private final List<WalletBalanceChangedEvent> events = new ArrayList<>();

        @Override
        public void publishEvent(Object event) {
            if (event instanceof WalletBalanceChangedEvent walletBalanceChangedEvent) {
                events.add(walletBalanceChangedEvent);
            }
        }
    }

    private static class InMemoryAccountRepository implements AccountRepository {
        private TradingAccount account = new TradingAccount(
                1L,
                "demo@coinzzickmock.dev",
                "Demo",
                100000,
                100000
        );

        @Override
        public Optional<TradingAccount> findByMemberId(Long memberId) {
            return Optional.of(account);
        }

        @Override
        public TradingAccount create(TradingAccount account) {
            if (this.account.memberId().equals(account.memberId())) {
                throw new IllegalStateException("account already exists");
            }
            this.account = account;
            return account;
        }

        @Override
        public AccountMutationResult updateWithVersion(
                TradingAccount expectedAccount,
                TradingAccount nextAccount,
                WalletHistorySource source
        ) {
            if (account.version() != expectedAccount.version()) {
                return AccountMutationResult.staleVersion(account);
            }
            account = nextAccount.withVersion(expectedAccount.version() + 1);
            return AccountMutationResult.updated(1, account);
        }
    }

    private static class InMemoryPositionRepository implements PositionRepository {
        private final List<PositionSnapshot> positions = new ArrayList<>();

        @Override
        public List<PositionSnapshot> findOpenPositions(Long memberId) {
            return List.copyOf(positions);
        }

        @Override
        public Optional<PositionSnapshot> findOpenPosition(Long memberId, String symbol, String positionSide, String marginMode) {
            return positions.stream()
                    .filter(position -> position.symbol().equals(symbol))
                    .filter(position -> position.positionSide().equals(positionSide))
                    .filter(position -> position.marginMode().equals(marginMode))
                    .findFirst();
        }

        @Override
        public List<OpenPositionCandidate> findOpenBySymbol(String symbol) {
            return positions.stream()
                    .filter(position -> position.symbol().equals(symbol))
                    .map(position -> new OpenPositionCandidate(1L, position))
                    .toList();
        }

        @Override
        public PositionSnapshot save(Long memberId, PositionSnapshot positionSnapshot) {
            delete(memberId, positionSnapshot.symbol(), positionSnapshot.positionSide(), positionSnapshot.marginMode());
            positions.add(positionSnapshot);
            return positionSnapshot;
        }

        @Override
        public boolean deleteIfOpen(Long memberId, String symbol, String positionSide, String marginMode) {
            int before = positions.size();
            positions.removeIf(position -> position.symbol().equals(symbol)
                    && position.positionSide().equals(positionSide)
                    && position.marginMode().equals(marginMode));
            return before != positions.size();
        }

        @Override
        public void delete(Long memberId, String symbol, String positionSide, String marginMode) {
            deleteIfOpen(memberId, symbol, positionSide, marginMode);
        }
    }

    private static class InMemoryOrderRepository implements OrderRepository {
        private final List<FuturesOrder> orders = new ArrayList<>();

        @Override
        public FuturesOrder save(Long memberId, FuturesOrder futuresOrder) {
            orders.add(futuresOrder);
            return futuresOrder;
        }

        @Override
        public List<FuturesOrder> findByMemberId(Long memberId) {
            return List.copyOf(orders);
        }

        @Override
        public Optional<FuturesOrder> findByMemberIdAndOrderId(Long memberId, String orderId) {
            return Optional.empty();
        }

        @Override
        public List<PendingOrderCandidate> findPendingBySymbol(String symbol) {
            return List.of();
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

    private static RealtimeMarketPriceReader realtimePriceReader() {
        return realtimePriceReader(100000, 100000);
    }

    private static RealtimeMarketPriceReader realtimePriceReader(double lastPrice, double markPrice) {
        RealtimeMarketDataStore store = new RealtimeMarketDataStore();
        Instant now = Instant.now();
        store.acceptTrade(new RealtimeMarketTradeTick(
                "BTCUSDT",
                "trade-" + lastPrice,
                BigDecimal.valueOf(lastPrice),
                BigDecimal.ONE,
                "buy",
                now,
                now
        ));
        store.acceptTicker(new RealtimeMarketTickerUpdate(
                "BTCUSDT",
                BigDecimal.valueOf(lastPrice),
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
