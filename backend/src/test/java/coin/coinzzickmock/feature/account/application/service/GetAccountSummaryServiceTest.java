package coin.coinzzickmock.feature.account.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import coin.coinzzickmock.feature.account.application.query.GetAccountSummaryQuery;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.application.result.AccountSummaryResult;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketDataStore;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketPriceReader;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketTickerUpdate;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketTradeTick;
import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.feature.member.domain.MemberCredential;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.result.OpenPositionCandidate;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointRepository;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import java.math.BigDecimal;
import java.time.Instant;
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
                    public Optional<TradingAccount> findByMemberId(Long memberId) {
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
                    public Optional<RewardPointWallet> findByMemberId(Long memberId) {
                        return Optional.of(new RewardPointWallet(memberId, 12));
                    }

                    @Override
                    public RewardPointWallet save(RewardPointWallet rewardPointWallet) {
                        fail("account summary read must not save reward wallet");
                        return rewardPointWallet;
                    }
                },
                positionRepository,
                realtimePriceReader(110),
                new MemberCredentialRepository() {
                    @Override
                    public Optional<MemberCredential> findActiveByMemberId(Long memberId) {
                        return Optional.of(MemberCredential.register(
                                "demo",
                                "hashed",
                                "Demo",
                                "DemoNick",
                                "demo@coinzzickmock.dev",
                                "010-0000-0000",
                                "04524",
                                "서울",
                                "",
                                0
                        ).withMemberId(memberId));
                    }

                    @Override
                    public Optional<MemberCredential> findActiveByAccount(String account) {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<MemberCredential> findByAccountIncludingWithdrawn(String account) {
                        return Optional.empty();
                    }

                    @Override
                    public boolean existsByAccount(String account) {
                        return false;
                    }

                    @Override
                    public MemberCredential save(MemberCredential memberCredential) {
                        fail("account summary read must not save member");
                        return memberCredential;
                    }
                }
        );

        AccountSummaryResult result = service.execute(new GetAccountSummaryQuery(1L));

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
        public List<PositionSnapshot> findOpenPositions(Long memberId) {
            return List.of(position);
        }

        @Override
        public Optional<PositionSnapshot> findOpenPosition(Long memberId, String symbol, String positionSide, String marginMode) {
            return Optional.of(position);
        }

        @Override
        public List<OpenPositionCandidate> findOpenBySymbol(String symbol) {
            return List.of(new OpenPositionCandidate(1L, position));
        }

        @Override
        public PositionSnapshot save(Long memberId, PositionSnapshot positionSnapshot) {
            fail("read-time mark-to-market must not save positions");
            return positionSnapshot;
        }

        @Override
        public boolean deleteIfOpen(Long memberId, String symbol, String positionSide, String marginMode) {
            fail("read-time mark-to-market must not delete positions");
            return false;
        }

        @Override
        public void delete(Long memberId, String symbol, String positionSide, String marginMode) {
            fail("read-time mark-to-market must not delete positions");
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
