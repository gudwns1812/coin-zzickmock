package coin.coinzzickmock.feature.order.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.CoinZzickmockApplication;
import coin.coinzzickmock.feature.account.application.dto.AccountMutationResult;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.account.infrastructure.persistence.AccountPersistenceRepository;
import coin.coinzzickmock.feature.market.catalog.application.dto.MarketSummaryResult;
import coin.coinzzickmock.feature.market.catalog.application.dto.MarketSummaryUpdatedEvent;
import coin.coinzzickmock.feature.market.quote.application.dto.MarketPriceMovementDirection;
import coin.coinzzickmock.feature.market.quote.application.dto.MarketTradePriceMovedEvent;
import coin.coinzzickmock.feature.market.quote.application.dto.RealtimeMarketTickerUpdate;
import coin.coinzzickmock.feature.market.quote.application.dto.RealtimeMarketTradeTick;
import coin.coinzzickmock.feature.market.quote.application.implement.RealtimeMarketDataStore;
import coin.coinzzickmock.feature.order.application.implement.OrderPendingFillProcessor;
import coin.coinzzickmock.feature.order.application.implement.OrderPendingLimitOrderBook;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.position.application.implement.OpenPositionBookHydrator;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import coin.coinzzickmock.testsupport.MySqlIntegrationTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * 체결 워커(W)와 시세 갱신 스케줄러(S)가 같은 두 회원의 계좌 행을 서로 반대 순서로 잠그던 데드락의 회귀 테스트.
 *
 * <p>회원 A는 ETH 교차 마진 포지션과 BTC 지정가 롱, 회원 B는 BTC/ETH 지정가 롱을 가진다.
 * W는 BTC 하락 체결에서 높은 지정가 순으로 A → B를 잠근다.
 * S는 ETH 시세 갱신 한 트랜잭션에서 대기 주문 체결로 B를 잠근 뒤 교차 마진 청산을 판정한다.
 * 예전에는 청산 조건과 무관하게 교차 포지션 보유자 A의 계좌를 잠가 B → A 순서가 되었고 InnoDB 데드락이 났다.
 *
 * <p>락은 실제 MySQL 행 락이다. 래치는 "W가 A를 잡은 뒤에 S가 시작하고, S가 B를 잡은 뒤에 W가 B를 요청한다"는
 * 타이밍만 고정한다.
 */
@MySqlIntegrationTest
@SpringBootTest(classes = CoinZzickmockApplication.class)
@ActiveProfiles("test")
@Import(MarketExecutionAccountLockDeadlockTest.LockOrderingConfiguration.class)
class MarketExecutionAccountLockDeadlockTest {
    private static final String TRADE_WORKER = "trade-worker";
    private static final String MARKET_REFRESH = "market-refresh";
    private static final List<String> LOCK_LOG = new CopyOnWriteArrayList<>();
    private static final CountDownLatch WORKER_HOLDS_A = new CountDownLatch(1);
    private static final CountDownLatch REFRESH_HOLDS_B = new CountDownLatch(1);
    private static volatile Long memberA;
    private static volatile Long memberB;

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private PositionRepository positionRepository;
    @Autowired
    private OrderPendingLimitOrderBook pendingLimitOrderBook;
    @Autowired
    private OpenPositionBookHydrator openPositionBookHydrator;
    @Autowired
    private RealtimeMarketDataStore realtimeMarketDataStore;
    @Autowired
    private OrderPendingFillProcessor pendingFillProcessor;
    @Autowired
    private MarketOrderExecutionService marketOrderExecutionService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void marketRefreshDoesNotLockUnbreachedCrossHolderWhileTradeWorkerHoldsIt() throws InterruptedException {
        memberA = createMember();
        memberB = createMember();
        positionRepository.save(memberA, PositionSnapshot.open("ETHUSDT", "LONG", "CROSS", 10, 1.0, 2000.0, 2000.0));
        FuturesOrder aBtc = pendingLong("BTCUSDT", 98.5);
        FuturesOrder bBtc = pendingLong("BTCUSDT", 98.0);
        FuturesOrder bEth = pendingLong("ETHUSDT", 1990.0);
        orderRepository.save(memberA, aBtc);
        orderRepository.save(memberB, bBtc);
        orderRepository.save(memberB, bEth);
        openPositionBookHydrator.hydrate();
        pendingLimitOrderBook.add(memberA, aBtc);
        pendingLimitOrderBook.add(memberB, bBtc);
        seedRealtimeMarket("BTCUSDT", 97.0);
        seedRealtimeMarket("ETHUSDT", 1980.0);

        Instant now = Instant.now().plusMillis(50);
        MarketTradePriceMovedEvent btcDrop = new MarketTradePriceMovedEvent(
                "BTCUSDT", 99.0, 97.0, MarketPriceMovementDirection.DOWN, now, now
        );
        MarketSummaryUpdatedEvent ethDrop = new MarketSummaryUpdatedEvent(
                new MarketSummaryResult("ETHUSDT", "ETHUSDT", 1980.0, 1980.0, 1980.0, 0.0001, 0, 0),
                2000.0,
                MarketPriceMovementDirection.DOWN
        );

        AtomicReference<Throwable> workerFailure = new AtomicReference<>();
        AtomicReference<Throwable> refreshFailure = new AtomicReference<>();
        Thread worker = new Thread(
                () -> runCapturing(() -> pendingFillProcessor.fillExecutablePendingOrders(btcDrop), workerFailure),
                TRADE_WORKER
        );
        Thread refresh = new Thread(
                () -> runCapturing(() -> marketOrderExecutionService.onMarketUpdated(ethDrop), refreshFailure),
                MARKET_REFRESH
        );
        worker.start();
        refresh.start();
        worker.join(TimeUnit.SECONDS.toMillis(30));
        refresh.join(TimeUnit.SECONDS.toMillis(30));

        assertThat(workerFailure.get()).as("trade worker failure, lock log %s", LOCK_LOG).isNull();
        assertThat(refreshFailure.get()).as("market refresh failure, lock log %s", LOCK_LOG).isNull();
        assertThat(LOCK_LOG).doesNotContain(MARKET_REFRESH + " locks A");
        assertThat(status(memberA, aBtc)).isEqualTo(FuturesOrder.STATUS_FILLED);
        assertThat(status(memberB, bBtc)).isEqualTo(FuturesOrder.STATUS_FILLED);
        assertThat(status(memberB, bEth)).isEqualTo(FuturesOrder.STATUS_FILLED);
    }

    private static void runCapturing(Runnable body, AtomicReference<Throwable> failure) {
        try {
            body.run();
        } catch (Throwable throwable) {
            failure.set(throwable);
        }
    }

    private Long createMember() {
        Long memberId = (UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE) % 1_000_000_000L + 1;
        jdbcTemplate.update("""
                INSERT INTO member_credentials (
                    id, account, password_hash, member_name, nickname, member_email,
                    phone_number, invest_score, role
                )
                VALUES (?, ?, 'hash', ?, ?, ?, '010-0000-0000', 0, 'USER')
                """,
                memberId,
                "account-" + memberId,
                String.valueOf(memberId),
                String.valueOf(memberId),
                memberId + "@coinzzickmock.dev"
        );
        accountRepository.create(new TradingAccount(
                memberId,
                memberId + "@coinzzickmock.dev",
                String.valueOf(memberId),
                100000,
                100000
        ));
        return memberId;
    }

    private static FuturesOrder pendingLong(String symbol, double limitPrice) {
        return FuturesOrder.place(
                symbol + "-" + UUID.randomUUID(),
                symbol,
                "LONG",
                FuturesOrder.TYPE_LIMIT,
                "ISOLATED",
                10,
                0.1,
                limitPrice,
                false,
                "MAKER",
                0,
                limitPrice
        );
    }

    private void seedRealtimeMarket(String symbol, double price) {
        Instant now = Instant.now();
        realtimeMarketDataStore.acceptTrade(new RealtimeMarketTradeTick(
                symbol, "trade-" + UUID.randomUUID(), BigDecimal.valueOf(price), BigDecimal.ONE, "sell", now, now
        ));
        realtimeMarketDataStore.acceptTicker(new RealtimeMarketTickerUpdate(
                symbol,
                BigDecimal.valueOf(price),
                BigDecimal.valueOf(price),
                BigDecimal.valueOf(price),
                BigDecimal.valueOf(0.0001),
                now.plusSeconds(3600),
                now,
                now
        ));
    }

    private String status(Long memberId, FuturesOrder order) {
        return orderRepository.findByMemberIdAndOrderId(memberId, order.orderId())
                .map(FuturesOrder::status)
                .orElse("missing");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class LockOrderingConfiguration {
        @Bean
        @Primary
        AccountRepository lockOrderingAccountRepository(AccountPersistenceRepository delegate) {
            return new LockOrderingAccountRepository(delegate);
        }
    }

    private static final class LockOrderingAccountRepository implements AccountRepository {
        private final AccountRepository delegate;

        private LockOrderingAccountRepository(AccountRepository delegate) {
            this.delegate = delegate;
        }

        @Override
        public Optional<TradingAccount> findByMemberIdForUpdate(Long memberId) {
            String thread = Thread.currentThread().getName();
            if (MARKET_REFRESH.equals(thread)) {
                await(WORKER_HOLDS_A);
            }
            if (TRADE_WORKER.equals(thread) && memberId.equals(memberB)) {
                await(REFRESH_HOLDS_B);
            }
            Optional<TradingAccount> locked = delegate.findByMemberIdForUpdate(memberId);
            LOCK_LOG.add(thread + " locks " + label(memberId));
            if (TRADE_WORKER.equals(thread) && memberId.equals(memberA)) {
                WORKER_HOLDS_A.countDown();
            }
            if (MARKET_REFRESH.equals(thread) && memberId.equals(memberB)) {
                REFRESH_HOLDS_B.countDown();
            }
            return locked;
        }

        private static String label(Long memberId) {
            if (memberId.equals(memberA)) {
                return "A";
            }
            if (memberId.equals(memberB)) {
                return "B";
            }
            return String.valueOf(memberId);
        }

        private static void await(CountDownLatch latch) {
            try {
                latch.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public Optional<TradingAccount> findByMemberId(Long memberId) {
            return delegate.findByMemberId(memberId);
        }

        @Override
        public Optional<Double> findWalletBalanceByMemberId(Long memberId) {
            return delegate.findWalletBalanceByMemberId(memberId);
        }

        @Override
        public List<TradingAccount> findAll() {
            return delegate.findAll();
        }

        @Override
        public TradingAccount create(TradingAccount account) {
            return delegate.create(account);
        }

        @Override
        public AccountMutationResult updateWithVersion(TradingAccount expectedAccount, TradingAccount nextAccount) {
            return delegate.updateWithVersion(expectedAccount, nextAccount);
        }
    }
}
