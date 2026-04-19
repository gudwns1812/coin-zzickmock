package coin.coinzzickmock.feature.position.application.service;

import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.result.ClosePositionResult;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import coin.coinzzickmock.feature.reward.application.command.GrantProfitPointCommand;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClosePositionServiceTest {
    @Test
    void partiallyClosesPositionAndUpdatesAccountAndRewardPoint() {
        InMemoryAccountRepository accountRepository = new InMemoryAccountRepository(
                new TradingAccount("demo-member", "demo@coinzzickmock.dev", "Demo", 100000, 95000)
        );
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
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
                accountRepository,
                new FakeProviders(110000, 110000),
                new RewardPointGrantProcessor(new RewardPointPolicy(), rewardPointRepository)
        );

        ClosePositionResult result = service.close("demo-member", "BTCUSDT", "LONG", "ISOLATED", 0.1);

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
        public PositionSnapshot save(String memberId, PositionSnapshot positionSnapshot) {
            delete(memberId, positionSnapshot.symbol(), positionSnapshot.positionSide(), positionSnapshot.marginMode());
            positions.add(positionSnapshot);
            return positionSnapshot;
        }

        @Override
        public void delete(String memberId, String symbol, String positionSide, String marginMode) {
            positions.removeIf(position -> position.symbol().equals(symbol)
                    && position.positionSide().equals(positionSide)
                    && position.marginMode().equals(marginMode));
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
