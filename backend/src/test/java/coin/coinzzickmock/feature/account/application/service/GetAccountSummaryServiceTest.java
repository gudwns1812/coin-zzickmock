package coin.coinzzickmock.feature.account.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import coin.coinzzickmock.feature.account.application.query.GetAccountSummaryQuery;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.application.result.AccountSummaryResult;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.result.OpenPositionCandidate;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointRepository;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.providers.auth.Actor;
import coin.coinzzickmock.providers.auth.AuthProvider;
import coin.coinzzickmock.providers.connector.ConnectorProvider;
import coin.coinzzickmock.providers.connector.MarketDataGateway;
import coin.coinzzickmock.providers.featureflag.FeatureFlagProvider;
import coin.coinzzickmock.providers.telemetry.TelemetryProvider;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetAccountSummaryServiceTest {
    @Test
    void calculatesEquityFromReadTimeMarkToMarketWithoutPersistingPosition() {
        ReadOnlyPositionRepository positionRepository = new ReadOnlyPositionRepository(PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                2,
                100,
                100
        ).withVersion(5));
        GetAccountSummaryService service = new GetAccountSummaryService(
                new AccountRepository() {
                    @Override
                    public Optional<TradingAccount> findByMemberId(String memberId) {
                        return Optional.of(new TradingAccount(memberId, "demo@coinzzickmock.dev", "Demo", 100000, 95000));
                    }

                    @Override
                    public TradingAccount save(TradingAccount account) {
                        fail("account summary read must not save account");
                        return account;
                    }
                },
                new RewardPointRepository() {
                    @Override
                    public Optional<RewardPointWallet> findByMemberId(String memberId) {
                        return Optional.of(new RewardPointWallet(memberId, 12));
                    }

                    @Override
                    public RewardPointWallet save(RewardPointWallet rewardPointWallet) {
                        fail("account summary read must not save reward wallet");
                        return rewardPointWallet;
                    }
                },
                positionRepository,
                new FakeProviders(110)
        );

        AccountSummaryResult result = service.execute(new GetAccountSummaryQuery("demo-member"));

        assertEquals(100020, result.usdtBalance(), 0.0001);
        assertEquals(20, result.totalUnrealizedPnl(), 0.0001);
        assertEquals(1.0, result.roi(), 0.0001);
        assertEquals(5, positionRepository.position.version());
        assertEquals(100, positionRepository.position.markPrice(), 0.0001);
    }

    private static class ReadOnlyPositionRepository implements PositionRepository {
        private final PositionSnapshot position;

        private ReadOnlyPositionRepository(PositionSnapshot position) {
            this.position = position;
        }

        @Override
        public List<PositionSnapshot> findOpenPositions(String memberId) {
            return List.of(position);
        }

        @Override
        public Optional<PositionSnapshot> findOpenPosition(String memberId, String symbol, String positionSide, String marginMode) {
            return Optional.of(position);
        }

        @Override
        public List<OpenPositionCandidate> findOpenBySymbol(String symbol) {
            return List.of(new OpenPositionCandidate("demo-member", position));
        }

        @Override
        public PositionSnapshot save(String memberId, PositionSnapshot positionSnapshot) {
            fail("read-time mark-to-market must not save positions");
            return positionSnapshot;
        }

        @Override
        public boolean deleteIfOpen(String memberId, String symbol, String positionSide, String marginMode) {
            fail("read-time mark-to-market must not delete positions");
            return false;
        }

        @Override
        public void delete(String memberId, String symbol, String positionSide, String marginMode) {
            fail("read-time mark-to-market must not delete positions");
        }
    }

    private record FakeProviders(double markPrice) implements Providers {
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
            return () -> new MarketDataGateway() {
                @Override
                public List<MarketSnapshot> loadSupportedMarkets() {
                    return List.of(loadMarket("BTCUSDT"));
                }

                @Override
                public MarketSnapshot loadMarket(String symbol) {
                    return new MarketSnapshot(symbol, "Bitcoin Perpetual", markPrice, markPrice, markPrice, 0.0001, 0.1);
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
