package coin.coinzzickmock.feature.order.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import coin.coinzzickmock.CoinZzickmockApplication;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketDataStore;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketPriceReader;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.order.application.command.ModifyOrderCommand;
import coin.coinzzickmock.feature.order.application.fill.MarketableEditedOrderFiller;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.realtime.PendingLimitOrderBook;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.order.domain.OrderPlacementPolicy;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = CoinZzickmockApplication.class)
@ActiveProfiles("test")
class ModifyOrderServiceTransactionTest {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private MarketableEditedOrderFiller marketableEditedOrderFiller;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void fillsPersistedOrderWhenPostUpdateMarketCheckBecomesMarketable() {
        Long memberId = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
        String orderId = "marketable-edit-" + UUID.randomUUID();
        saveAccount(memberId);
        orderRepository.save(memberId, FuturesOrder.place(
                orderId,
                "BTCUSDT",
                "LONG",
                FuturesOrder.TYPE_LIMIT,
                "ISOLATED",
                10,
                0.1,
                90.0,
                false,
                "MAKER",
                0.00135,
                90.0
        ));
        ModifyOrderService service = serviceWithMarketSequence(94.0, 94.0);

        transactionTemplate.executeWithoutResult(status ->
                service.modify(new ModifyOrderCommand(memberId, orderId, BigDecimal.valueOf(95.0)))
        );

        FuturesOrder persisted = orderRepository.findByMemberIdAndOrderId(memberId, orderId).orElseThrow();
        assertEquals(FuturesOrder.STATUS_FILLED, persisted.status());
        assertEquals(95, persisted.limitPrice(), 0.0001);
        assertEquals("TAKER", persisted.feeType());
        assertEquals(0.0047, persisted.estimatedFee(), 0.000001);
        assertEquals(94, persisted.executionPrice(), 0.0001);
        assertEquals(1, positionRepository.findOpenPositions(memberId).size());
    }

    @Test
    void fillsPendingCloseOrderWhenEditedPriceIsMarketable() {
        Long memberId = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
        String orderId = "marketable-close-edit-" + UUID.randomUUID();
        saveAccount(memberId);
        positionRepository.save(memberId, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                0.2,
                90.0,
                100.0,
                0.0018
        ));
        orderRepository.save(memberId, FuturesOrder.place(
                orderId,
                "BTCUSDT",
                "LONG",
                FuturesOrder.TYPE_LIMIT,
                FuturesOrder.PURPOSE_CLOSE_POSITION,
                "ISOLATED",
                10,
                0.2,
                110.0,
                false,
                "MAKER",
                0,
                110.0
        ));
        ModifyOrderService service = serviceWithMarketSequence(100.0, 100.0);

        transactionTemplate.executeWithoutResult(status ->
                service.modify(new ModifyOrderCommand(memberId, orderId, BigDecimal.valueOf(99.0)))
        );

        FuturesOrder persisted = orderRepository.findByMemberIdAndOrderId(memberId, orderId).orElseThrow();
        assertEquals(FuturesOrder.STATUS_FILLED, persisted.status());
        assertEquals(99, persisted.limitPrice(), 0.0001);
        assertEquals("TAKER", persisted.feeType());
        assertEquals(0.01, persisted.estimatedFee(), 0.000001);
        assertEquals(100, persisted.executionPrice(), 0.0001);
        assertEquals(0, positionRepository.findOpenPositions(memberId).size());
    }

    private ModifyOrderService serviceWithMarketSequence(double firstLastPrice, double secondLastPrice) {
        OrderPlacementPolicy orderPlacementPolicy = new OrderPlacementPolicy();
        return new ModifyOrderService(
                orderRepository,
                new SequencedMarketPriceReader(firstLastPrice, secondLastPrice),
                orderPlacementPolicy,
                new AccountOrderMutationLock(accountRepository),
                marketableEditedOrderFiller,
                new PendingLimitOrderBook()
        );
    }

    private void saveAccount(Long memberId) {
        jdbcTemplate.update("""
                INSERT INTO member_credentials (
                    id, account, password_hash, member_name, nickname, member_email,
                    phone_number, zip_code, address, address_detail, invest_score, role
                )
                VALUES (?, ?, 'hash', ?, ?, ?, '010-0000-0000', '00000', '서울', '', 0, 'USER')
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
    }

    private static class SequencedMarketPriceReader extends RealtimeMarketPriceReader {
        private final double firstLastPrice;
        private final double secondLastPrice;
        private final AtomicInteger calls = new AtomicInteger();

        private SequencedMarketPriceReader(double firstLastPrice, double secondLastPrice) {
            super(new RealtimeMarketDataStore());
            this.firstLastPrice = firstLastPrice;
            this.secondLastPrice = secondLastPrice;
        }

        @Override
        public MarketSnapshot requireFreshMarket(String symbol) {
            double lastPrice = calls.incrementAndGet() == 1 ? firstLastPrice : secondLastPrice;
            return new MarketSnapshot(symbol, symbol, lastPrice, lastPrice, lastPrice, 0, 0);
        }
    }
}
