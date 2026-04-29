package coin.coinzzickmock.feature.order.application.service;

import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.leaderboard.application.event.WalletBalanceChangedEvent;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
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
import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.providers.auth.Actor;
import coin.coinzzickmock.providers.auth.AuthProvider;
import coin.coinzzickmock.providers.connector.ConnectorProvider;
import coin.coinzzickmock.providers.connector.MarketDataGateway;
import coin.coinzzickmock.providers.featureflag.FeatureFlagProvider;
import coin.coinzzickmock.providers.telemetry.TelemetryProvider;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

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

        CreateOrderService service = new CreateOrderService(
                new OrderPreviewPolicy(),
                new OrderPlacementPolicy(),
                new FakeProviders(),
                orderRepository,
                accountRepository,
                positionRepository,
                new AfterCommitEventPublisher(event -> {
                })
        );

        CreateOrderResult result = service.execute(new CreateOrderCommand(
                "demo-member",
                "BTCUSDT",
                "LONG",
                "MARKET",
                "ISOLATED",
                10,
                0.1,
                null
        ));

        assertEquals("FILLED", result.status());
        assertEquals(1, positionRepository.findOpenPositions("demo-member").size());
        assertEquals(5, positionRepository.findOpenPositions("demo-member").get(0).accumulatedOpenFee(), 0.0001);
        assertEquals(98995, accountRepository.findByMemberId("demo-member").orElseThrow().availableMargin(), 0.0001);
    }

    @Test
    void recalculatesLiquidationPriceFromWeightedEntryPriceWhenIncreasingExistingPosition() {
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        positionRepository.save("demo-member", new PositionSnapshot(
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

        CreateOrderService service = new CreateOrderService(
                new OrderPreviewPolicy(),
                new OrderPlacementPolicy(),
                new FakeProviders(110000, 110000),
                orderRepository,
                accountRepository,
                positionRepository,
                new AfterCommitEventPublisher(event -> {
                })
        );

        service.execute(new CreateOrderCommand(
                "demo-member",
                "BTCUSDT",
                "LONG",
                "MARKET",
                "ISOLATED",
                10,
                0.1,
                null
        ));

        PositionSnapshot updated = positionRepository.findOpenPosition("demo-member", "BTCUSDT", "LONG", "ISOLATED")
                .orElseThrow();
        assertEquals(105000.0, updated.entryPrice(), 0.0001);
        assertEquals(94500.0, updated.liquidationPrice(), 0.0001);
        assertEquals(5.5, updated.accumulatedOpenFee(), 0.0001);
    }

    @Test
    void previewsMakerLimitOrderWithEntryPriceAndExecutionFlag() {
        CreateOrderService service = new CreateOrderService(
                new OrderPreviewPolicy(),
                new OrderPlacementPolicy(),
                new FakeProviders(),
                new InMemoryOrderRepository(),
                new InMemoryAccountRepository(),
                new InMemoryPositionRepository(),
                new AfterCommitEventPublisher(event -> {
                })
        );

        OrderPreview preview = service.preview(new CreateOrderCommand(
                "demo-member",
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
        CreateOrderService service = new CreateOrderService(
                new OrderPreviewPolicy(),
                new OrderPlacementPolicy(),
                new FakeProviders(74700, 74700),
                orderRepository,
                accountRepository,
                positionRepository,
                new AfterCommitEventPublisher(event -> {
                })
        );
        CreateOrderCommand command = new CreateOrderCommand(
                "demo-member",
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
        assertEquals(99249.265, accountRepository.findByMemberId("demo-member").orElseThrow().availableMargin(), 0.0001);
        FuturesOrder saved = orderRepository.findByMemberId("demo-member").get(0);
        assertEquals(FuturesOrder.STATUS_FILLED, saved.status());
        assertEquals("TAKER", saved.feeType());
        assertEquals(74700.0, saved.executionPrice(), 0.0001);
        assertEquals(75000.0, saved.limitPrice(), 0.0001);
        PositionSnapshot opened = positionRepository.findOpenPosition("demo-member", "BTCUSDT", "LONG", "ISOLATED")
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
        CreateOrderService service = new CreateOrderService(
                new OrderPreviewPolicy(),
                new OrderPlacementPolicy(),
                new FakeProviders(75000, 75000),
                orderRepository,
                accountRepository,
                positionRepository,
                new AfterCommitEventPublisher(event -> {
                })
        );
        CreateOrderCommand command = new CreateOrderCommand(
                "demo-member",
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
        assertEquals(99246.25, accountRepository.findByMemberId("demo-member").orElseThrow().availableMargin(), 0.0001);
        FuturesOrder saved = orderRepository.findByMemberId("demo-member").get(0);
        assertEquals(FuturesOrder.STATUS_FILLED, saved.status());
        assertEquals("TAKER", saved.feeType());
        assertEquals(75000.0, saved.executionPrice(), 0.0001);
        assertEquals(75000.0, saved.limitPrice(), 0.0001);
        PositionSnapshot opened = positionRepository.findOpenPosition("demo-member", "BTCUSDT", "SHORT", "ISOLATED")
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
        CreateOrderService service = new CreateOrderService(
                new OrderPreviewPolicy(),
                new OrderPlacementPolicy(),
                new FakeProviders(),
                orderRepository,
                accountRepository,
                positionRepository,
                new AfterCommitEventPublisher(eventPublisher)
        );

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.execute(new CreateOrderCommand(
                    "demo-member",
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
            assertEquals("demo-member", eventPublisher.events.get(0).memberId());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void previewRejectsUnsupportedOrderType() {
        CreateOrderService service = new CreateOrderService(
                new OrderPreviewPolicy(),
                new OrderPlacementPolicy(),
                new FakeProviders(),
                new InMemoryOrderRepository(),
                new InMemoryAccountRepository(),
                new InMemoryPositionRepository(),
                new AfterCommitEventPublisher(event -> {
                })
        );

        CoreException thrown = assertThrows(CoreException.class, () -> service.preview(new CreateOrderCommand(
                "demo-member",
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
                "demo-member",
                "demo@coinzzickmock.dev",
                "Demo",
                100000,
                100000
        );

        @Override
        public Optional<TradingAccount> findByMemberId(String memberId) {
            return Optional.of(account);
        }

        @Override
        public TradingAccount save(TradingAccount account) {
            this.account = account;
            return account;
        }
    }

    private static class InMemoryPositionRepository implements PositionRepository {
        private final List<PositionSnapshot> positions = new ArrayList<>();

        @Override
        public List<PositionSnapshot> findOpenPositions(String memberId) {
            return positions;
        }

        @Override
        public Optional<PositionSnapshot> findOpenPosition(String memberId, String symbol, String positionSide, String marginMode) {
            return positions.stream().findFirst();
        }

        @Override
        public List<OpenPositionCandidate> findOpenBySymbol(String symbol) {
            return positions.stream()
                    .filter(position -> position.symbol().equals(symbol))
                    .map(position -> new OpenPositionCandidate("demo-member", position))
                    .toList();
        }

        @Override
        public PositionSnapshot save(String memberId, PositionSnapshot positionSnapshot) {
            positions.clear();
            positions.add(positionSnapshot);
            return positionSnapshot;
        }

        @Override
        public boolean deleteIfOpen(String memberId, String symbol, String positionSide, String marginMode) {
            boolean deleted = !positions.isEmpty();
            positions.clear();
            return deleted;
        }

        @Override
        public void delete(String memberId, String symbol, String positionSide, String marginMode) {
            positions.clear();
        }
    }

    private static class InMemoryOrderRepository implements OrderRepository {
        private final List<FuturesOrder> orders = new ArrayList<>();

        @Override
        public FuturesOrder save(String memberId, FuturesOrder futuresOrder) {
            orders.add(futuresOrder);
            return futuresOrder;
        }

        @Override
        public List<FuturesOrder> findByMemberId(String memberId) {
            return List.copyOf(orders);
        }

        @Override
        public Optional<FuturesOrder> findByMemberIdAndOrderId(String memberId, String orderId) {
            return Optional.empty();
        }

        @Override
        public List<PendingOrderCandidate> findPendingBySymbol(String symbol) {
            return List.of();
        }

        @Override
        public Optional<FuturesOrder> claimPendingFill(
                String memberId,
                String orderId,
                double executionPrice,
                String feeType,
                double estimatedFee
        ) {
            return Optional.empty();
        }

        @Override
        public FuturesOrder updateStatus(String memberId, String orderId, String status) {
            throw new UnsupportedOperationException();
        }
    }

    private static class FakeProviders implements Providers {
        private final double lastPrice;
        private final double markPrice;

        private FakeProviders() {
            this(100000, 100000);
        }

        private FakeProviders(double lastPrice, double markPrice) {
            this.lastPrice = lastPrice;
            this.markPrice = markPrice;
        }

        @Override
        public AuthProvider auth() {
            return new AuthProvider() {
                @Override
                public Actor currentActor() {
                    return new Actor("demo-member", "demo@coinzzickmock.dev", "Demo");
                }

                @Override
                public boolean isAuthenticated() {
                    return true;
                }
            };
        }

        @Override
        public ConnectorProvider connector() {
            return new ConnectorProvider() {
                @Override
                public MarketDataGateway marketDataGateway() {
                    return new MarketDataGateway() {
                        @Override
                        public List<MarketSnapshot> loadSupportedMarkets() {
                            return List.of(loadMarket("BTCUSDT"));
                        }

                        @Override
                        public MarketSnapshot loadMarket(String symbol) {
                            return new MarketSnapshot(symbol, "Bitcoin Perpetual", lastPrice, markPrice, markPrice, 0.0001, 0.1);
                        }
                    };
                }
            };
        }

        @Override
        public TelemetryProvider telemetry() {
            return new TelemetryProvider() {
                @Override
                public void recordUseCase(String useCaseName) {
                }

                @Override
                public void recordFailure(String useCaseName, String reason) {
                }
            };
        }

        @Override
        public FeatureFlagProvider featureFlags() {
            return key -> true;
        }
    }
}
