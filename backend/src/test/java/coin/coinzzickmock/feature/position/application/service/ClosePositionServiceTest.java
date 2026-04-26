package coin.coinzzickmock.feature.position.application.service;

import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.result.PendingOrderCandidate;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.position.application.repository.PositionHistoryRepository;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.result.OpenPositionCandidate;
import coin.coinzzickmock.feature.position.application.result.ClosePositionResult;
import coin.coinzzickmock.feature.position.domain.PositionHistory;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import coin.coinzzickmock.feature.reward.application.grant.RewardPointGrantProcessor;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointRepository;
import coin.coinzzickmock.feature.reward.domain.RewardPointPolicy;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.providers.auth.Actor;
import coin.coinzzickmock.providers.auth.AuthProvider;
import coin.coinzzickmock.providers.connector.ConnectorProvider;
import coin.coinzzickmock.providers.connector.MarketDataGateway;
import coin.coinzzickmock.providers.featureflag.FeatureFlagProvider;
import coin.coinzzickmock.providers.telemetry.TelemetryProvider;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ClosePositionServiceTest {
    @Test
    void partiallyClosesPositionAndUpdatesAccountAndRewardPoint() {
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository(
                new TradingAccount("demo-member", "demo@coinzzickmock.dev", "Demo", 100000, 95000)
        );
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryPositionHistoryRepository positionHistoryRepository = new InMemoryPositionHistoryRepository();
        InMemoryRewardPointRepository rewardPointRepository = new InMemoryRewardPointRepository();
        positionRepository.save("demo-member", new PositionSnapshot(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                0.2,
                100000,
                100000,
                90000.0,
                0
        ));

        ClosePositionService service = new ClosePositionService(
                positionRepository,
                orderRepository,
                new FakeProviders(110000, 110000),
                new PositionCloseFinalizer(
                        positionRepository,
                        accountRepository,
                        positionHistoryRepository,
                        new RewardPointGrantProcessor(new RewardPointPolicy(), rewardPointRepository),
                        event -> {
                        }
                )
        );

        ClosePositionResult result = service.close("demo-member", "BTCUSDT", "LONG", "ISOLATED", 0.1, "MARKET", null);

        assertEquals(0.1, result.closedQuantity(), 0.0001);
        assertEquals(994.5, result.realizedPnl(), 0.0001);
        assertEquals(20, result.grantedPoint(), 0.0001);

        TradingAccount updatedAccount = accountRepository.findByMemberId("demo-member").orElseThrow();
        assertEquals(100994.5, updatedAccount.walletBalance(), 0.0001);
        assertEquals(96994.5, updatedAccount.availableMargin(), 0.0001);

        PositionSnapshot remaining = positionRepository.findOpenPosition("demo-member", "BTCUSDT", "LONG", "ISOLATED")
                .orElseThrow();
        assertEquals(0.1, remaining.quantity(), 0.0001);
        assertEquals(1000.0, remaining.unrealizedPnl(), 0.0001);
        assertEquals(90000.0, remaining.liquidationPrice(), 0.0001);

        RewardPointWallet wallet = rewardPointRepository.findByMemberId("demo-member").orElseThrow();
        assertEquals(20, wallet.rewardPoint(), 0.0001);
    }

    @Test
    void lossCloseDoesNotGrantPointsAndSettlesAccountWithLoss() {
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository(
                new TradingAccount("demo-member", "demo@coinzzickmock.dev", "Demo", 100000, 95000)
        );
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryPositionHistoryRepository positionHistoryRepository = new InMemoryPositionHistoryRepository();
        InMemoryRewardPointRepository rewardPointRepository = new InMemoryRewardPointRepository();
        positionRepository.save("demo-member", new PositionSnapshot(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                0.2,
                100000,
                100000,
                90000.0,
                0
        ));

        ClosePositionService service = new ClosePositionService(
                positionRepository,
                orderRepository,
                new FakeProviders(90000, 90000),
                new PositionCloseFinalizer(
                        positionRepository,
                        accountRepository,
                        positionHistoryRepository,
                        new RewardPointGrantProcessor(new RewardPointPolicy(), rewardPointRepository),
                        event -> {
                        }
                )
        );

        ClosePositionResult result = service.close("demo-member", "BTCUSDT", "LONG", "ISOLATED", 0.1, "MARKET", null);

        assertEquals(0.1, result.closedQuantity(), 0.0001);
        assertEquals(-1004.5, result.realizedPnl(), 0.0001);
        assertEquals(0, result.grantedPoint(), 0.0001);

        TradingAccount updatedAccount = accountRepository.findByMemberId("demo-member").orElseThrow();
        assertEquals(98995.5, updatedAccount.walletBalance(), 0.0001);
        assertEquals(94995.5, updatedAccount.availableMargin(), 0.0001);

        PositionSnapshot remaining = positionRepository.findOpenPosition("demo-member", "BTCUSDT", "LONG", "ISOLATED")
                .orElseThrow();
        assertEquals(0.1, remaining.quantity(), 0.0001);
        assertEquals(-1000.0, remaining.unrealizedPnl(), 0.0001);

        RewardPointWallet wallet = rewardPointRepository.findByMemberId("demo-member").orElseThrow();
        assertEquals(0, wallet.rewardPoint(), 0.0001);
    }

    @Test
    void fullyClosedPositionIsSavedToPositionHistory() {
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository(
                new TradingAccount("demo-member", "demo@coinzzickmock.dev", "Demo", 100000, 95000)
        );
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryPositionHistoryRepository positionHistoryRepository = new InMemoryPositionHistoryRepository();
        InMemoryRewardPointRepository rewardPointRepository = new InMemoryRewardPointRepository();
        positionRepository.save("demo-member", new PositionSnapshot(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                0.2,
                100000,
                100000,
                90000.0,
                0
        ));

        ClosePositionService service = new ClosePositionService(
                positionRepository,
                orderRepository,
                new FakeProviders(110000, 110000),
                new PositionCloseFinalizer(
                        positionRepository,
                        accountRepository,
                        positionHistoryRepository,
                        new RewardPointGrantProcessor(new RewardPointPolicy(), rewardPointRepository),
                        event -> {
                        }
                )
        );

        ClosePositionResult result = service.close("demo-member", "BTCUSDT", "LONG", "ISOLATED", 0.2, "MARKET", null);

        assertEquals(0.2, result.closedQuantity(), 0.0001);
        assertFalse(positionRepository.findOpenPosition("demo-member", "BTCUSDT", "LONG", "ISOLATED").isPresent());

        List<PositionHistory> histories = positionHistoryRepository.findByMemberId("demo-member", null);
        assertEquals(1, histories.size());
        PositionHistory history = histories.get(0);
        assertEquals("BTCUSDT", history.symbol());
        assertEquals("LONG", history.positionSide());
        assertEquals("ISOLATED", history.marginMode());
        assertEquals(10, history.leverage());
        assertEquals(100000, history.averageEntryPrice(), 0.0001);
        assertEquals(110000, history.averageExitPrice(), 0.0001);
        assertEquals(0.2, history.positionSize(), 0.0001);
        assertEquals(2000, history.realizedPnl(), 0.0001);
        assertEquals(1.0, history.roi(), 0.0001);
    }

    @Test
    void limitCloseCreatesPendingCloseOrderWithoutClosingPosition() {
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository(
                new TradingAccount("demo-member", "demo@coinzzickmock.dev", "Demo", 100000, 95000)
        );
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryPositionHistoryRepository positionHistoryRepository = new InMemoryPositionHistoryRepository();
        InMemoryRewardPointRepository rewardPointRepository = new InMemoryRewardPointRepository();
        positionRepository.save("demo-member", new PositionSnapshot(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                0.2,
                100000,
                100000,
                90000.0,
                0
        ));

        ClosePositionService service = new ClosePositionService(
                positionRepository,
                orderRepository,
                new FakeProviders(110000, 110000),
                new PositionCloseFinalizer(
                        positionRepository,
                        accountRepository,
                        positionHistoryRepository,
                        new RewardPointGrantProcessor(new RewardPointPolicy(), rewardPointRepository),
                        event -> {
                        }
                )
        );

        ClosePositionResult result = service.close("demo-member", "BTCUSDT", "LONG", "ISOLATED", 0.1, "LIMIT", 112000.0);

        assertEquals(0, result.closedQuantity(), 0.0001);
        assertEquals(0, positionHistoryRepository.findByMemberId("demo-member", null).size());
        assertEquals(0.2, positionRepository.findOpenPosition("demo-member", "BTCUSDT", "LONG", "ISOLATED")
                .orElseThrow()
                .quantity(), 0.0001);

        FuturesOrder order = orderRepository.findByMemberId("demo-member").get(0);
        assertEquals(FuturesOrder.STATUS_PENDING, order.status());
        assertEquals(FuturesOrder.PURPOSE_CLOSE_POSITION, order.orderPurpose());
        assertEquals("LIMIT", order.orderType());
        assertEquals(112000, order.limitPrice(), 0.0001);
    }

    private static class InMemoryAccountRepository implements AccountRepository {
        private TradingAccount account;

        private InMemoryAccountRepository(TradingAccount account) {
            this.account = account;
        }

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

    private static class InMemoryOrderRepository implements OrderRepository {
        private final Map<String, PendingOrderCandidate> orders = new LinkedHashMap<>();

        @Override
        public FuturesOrder save(String memberId, FuturesOrder futuresOrder) {
            orders.put(key(memberId, futuresOrder.orderId()), new PendingOrderCandidate(memberId, futuresOrder));
            return futuresOrder;
        }

        @Override
        public List<FuturesOrder> findByMemberId(String memberId) {
            return orders.values().stream()
                    .filter(candidate -> candidate.memberId().equals(memberId))
                    .map(PendingOrderCandidate::order)
                    .toList();
        }

        @Override
        public Optional<FuturesOrder> findByMemberIdAndOrderId(String memberId, String orderId) {
            return Optional.ofNullable(orders.get(key(memberId, orderId)))
                    .map(PendingOrderCandidate::order);
        }

        @Override
        public List<PendingOrderCandidate> findPendingBySymbol(String symbol) {
            return orders.values().stream()
                    .filter(candidate -> candidate.symbol().equals(symbol))
                    .filter(candidate -> candidate.order().isPending())
                    .toList();
        }

        @Override
        public Optional<FuturesOrder> claimPendingFill(
                String memberId,
                String orderId,
                double executionPrice,
                String feeType,
                double estimatedFee
        ) {
            PendingOrderCandidate candidate = orders.get(key(memberId, orderId));
            if (candidate == null || !candidate.order().isPending()) {
                return Optional.empty();
            }
            FuturesOrder filled = candidate.order().fill(executionPrice, feeType, estimatedFee);
            orders.put(key(memberId, orderId), new PendingOrderCandidate(memberId, filled));
            return Optional.of(filled);
        }

        @Override
        public FuturesOrder updateStatus(String memberId, String orderId, String status) {
            PendingOrderCandidate candidate = orders.get(key(memberId, orderId));
            FuturesOrder updated = status.equals(FuturesOrder.STATUS_CANCELLED)
                    ? candidate.order().cancel()
                    : candidate.order();
            orders.put(key(memberId, orderId), new PendingOrderCandidate(memberId, updated));
            return updated;
        }

        private String key(String memberId, String orderId) {
            return memberId + ":" + orderId;
        }
    }

    private static class InMemoryPositionRepository implements PositionRepository {
        private final List<PositionSnapshot> positions = new ArrayList<>();

        @Override
        public List<PositionSnapshot> findOpenPositions(String memberId) {
            return List.copyOf(positions);
        }

        @Override
        public Optional<PositionSnapshot> findOpenPosition(String memberId, String symbol, String positionSide, String marginMode) {
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
                    .map(position -> new OpenPositionCandidate("demo-member", position))
                    .toList();
        }

        @Override
        public PositionSnapshot save(String memberId, PositionSnapshot positionSnapshot) {
            delete(memberId, positionSnapshot.symbol(), positionSnapshot.positionSide(), positionSnapshot.marginMode());
            positions.add(positionSnapshot);
            return positionSnapshot;
        }

        @Override
        public boolean deleteIfOpen(String memberId, String symbol, String positionSide, String marginMode) {
            int before = positions.size();
            delete(memberId, symbol, positionSide, marginMode);
            return before != positions.size();
        }

        @Override
        public void delete(String memberId, String symbol, String positionSide, String marginMode) {
            positions.removeIf(position -> position.symbol().equals(symbol)
                    && position.positionSide().equals(positionSide)
                    && position.marginMode().equals(marginMode));
        }
    }

    private static class InMemoryPositionHistoryRepository implements PositionHistoryRepository {
        private final Map<String, List<PositionHistory>> histories = new LinkedHashMap<>();

        @Override
        public PositionHistory save(String memberId, PositionHistory positionHistory) {
            histories.computeIfAbsent(memberId, ignored -> new ArrayList<>()).add(positionHistory);
            return positionHistory;
        }

        @Override
        public List<PositionHistory> findByMemberId(String memberId, String symbol) {
            return histories.getOrDefault(memberId, List.of()).stream()
                    .filter(history -> symbol == null || history.symbol().equals(symbol))
                    .toList();
        }
    }

    private static class InMemoryRewardPointRepository implements RewardPointRepository {
        private RewardPointWallet wallet = new RewardPointWallet("demo-member", 0);

        @Override
        public Optional<RewardPointWallet> findByMemberId(String memberId) {
            return wallet.memberId().equals(memberId) ? Optional.of(wallet) : Optional.empty();
        }

        @Override
        public RewardPointWallet save(RewardPointWallet rewardPointWallet) {
            this.wallet = rewardPointWallet;
            return rewardPointWallet;
        }
    }

    private static class FakeProviders implements Providers {
        private final double lastPrice;
        private final double markPrice;

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
