package coin.coinzzickmock.feature.order.application.service;

import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.order.application.command.CreateOrderCommand;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.result.CreateOrderResult;
import coin.coinzzickmock.feature.order.application.result.PendingOrderCandidate;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.order.domain.OrderPreview;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CreateOrderServiceTest {
    @Test
    void createsFilledMarketOrderAndUpdatesPosition() {
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();

        CreateOrderService service = new CreateOrderService(
                new OrderPreviewPolicy(),
                new FakeProviders(),
                orderRepository,
                accountRepository,
                positionRepository
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
                new FakeProviders(110000, 110000),
                orderRepository,
                accountRepository,
                positionRepository
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
    }

    @Test
    void previewsMakerLimitOrderWithEntryPriceAndExecutionFlag() {
        CreateOrderService service = new CreateOrderService(
                new OrderPreviewPolicy(),
                new FakeProviders(),
                new InMemoryOrderRepository(),
                new InMemoryAccountRepository(),
                new InMemoryPositionRepository()
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
        @Override
        public FuturesOrder save(String memberId, FuturesOrder futuresOrder) {
            return futuresOrder;
        }

        @Override
        public List<FuturesOrder> findByMemberId(String memberId) {
            return List.of();
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
