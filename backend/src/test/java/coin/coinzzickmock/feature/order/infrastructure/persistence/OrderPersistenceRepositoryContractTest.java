package coin.coinzzickmock.feature.order.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import coin.coinzzickmock.CoinZzickmockApplication;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.result.PendingOrderCandidate;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = CoinZzickmockApplication.class)
@ActiveProfiles("test")
class OrderPersistenceRepositoryContractTest {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findsPendingCandidatesWithOwnerIdentity() {
        Long firstMemberId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        Long secondMemberId = Math.abs(UUID.randomUUID().getLeastSignificantBits());
        saveAccount(firstMemberId);
        saveAccount(secondMemberId);

        orderRepository.save(firstMemberId, new FuturesOrder(
                "pending-" + UUID.randomUUID(),
                "BTCUSDT",
                "LONG",
                "LIMIT",
                "ISOLATED",
                10,
                0.1,
                99000.0,
                FuturesOrder.STATUS_PENDING,
                "MAKER",
                1.5,
                99000.0
        ));
        orderRepository.save(secondMemberId, new FuturesOrder(
                "filled-" + UUID.randomUUID(),
                "BTCUSDT",
                "SHORT",
                "LIMIT",
                "CROSS",
                10,
                0.2,
                101000.0,
                FuturesOrder.STATUS_FILLED,
                "TAKER",
                2.0,
                101000.0
        ));
        orderRepository.save(secondMemberId, new FuturesOrder(
                "pending-" + UUID.randomUUID(),
                "BTCUSDT",
                "SHORT",
                "LIMIT",
                "CROSS",
                10,
                0.2,
                101000.0,
                FuturesOrder.STATUS_PENDING,
                "MAKER",
                2.0,
                101000.0
        ));

        List<PendingOrderCandidate> candidates = orderRepository.findPendingBySymbol("BTCUSDT");

        assertTrue(candidates.stream().map(PendingOrderCandidate::memberId).toList().containsAll(List.of(firstMemberId, secondMemberId)));
        assertTrue(candidates.stream().allMatch(candidate -> candidate.order().isPending()));
    }

    @Test
    void claimsPendingFillExactlyOnce() {
        Long memberId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        String orderId = "order-" + UUID.randomUUID();
        saveAccount(memberId);

        orderRepository.save(memberId, new FuturesOrder(
                orderId,
                "BTCUSDT",
                "LONG",
                "LIMIT",
                "ISOLATED",
                10,
                0.1,
                99000.0,
                FuturesOrder.STATUS_PENDING,
                "MAKER",
                1.5,
                99000.0
        ));

        assertTrue(orderRepository.claimPendingFill(memberId, orderId, 98950.0, "MAKER", 1.25).isPresent());
        assertFalse(orderRepository.claimPendingFill(memberId, orderId, 98900.0, "MAKER", 1.1).isPresent());

        FuturesOrder filled = orderRepository.findByMemberIdAndOrderId(memberId, orderId).orElseThrow();
        assertEquals(FuturesOrder.STATUS_FILLED, filled.status());
        assertEquals(98950.0, filled.executionPrice(), 0.0001);
        assertEquals(1.25, filled.estimatedFee(), 0.0001);
    }

    @Test
    void findsAndAdjustsPendingCloseOrderQuantity() {
        Long memberId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        String orderId = "close-order-" + UUID.randomUUID();
        String secondOrderId = "close-order-" + UUID.randomUUID();
        saveAccount(memberId);

        orderRepository.save(memberId, FuturesOrder.place(
                orderId,
                "BTCUSDT",
                "LONG",
                "LIMIT",
                FuturesOrder.PURPOSE_CLOSE_POSITION,
                "ISOLATED",
                10,
                0.2,
                71000.0,
                false,
                "MAKER",
                0,
                71000.0
        ));
        orderRepository.save(memberId, FuturesOrder.place(
                secondOrderId,
                "BTCUSDT",
                "LONG",
                "LIMIT",
                FuturesOrder.PURPOSE_CLOSE_POSITION,
                "ISOLATED",
                10,
                0.08,
                72000.0,
                false,
                "MAKER",
                0,
                72000.0
        ));

        List<FuturesOrder> pendingCloseOrders = orderRepository.findPendingCloseOrders(
                memberId,
                "BTCUSDT",
                "LONG",
                "ISOLATED"
        );

        assertEquals(2, pendingCloseOrders.size());
        orderRepository.updateQuantityAndStatus(memberId, orderId, 0.05, FuturesOrder.STATUS_PENDING);
        FuturesOrder adjusted = orderRepository.findByMemberIdAndOrderId(memberId, orderId).orElseThrow();
        assertEquals(FuturesOrder.STATUS_PENDING, adjusted.status());
        assertEquals(0.05, adjusted.quantity(), 0.0001);

        int capped = orderRepository.capPendingOrderQuantity(memberId, List.of(orderId, secondOrderId), 0.03);

        assertEquals(2, capped);
        assertEquals(0.03, orderRepository.findByMemberIdAndOrderId(memberId, orderId).orElseThrow().quantity(), 0.0001);
        assertEquals(0.03, orderRepository.findByMemberIdAndOrderId(memberId, secondOrderId).orElseThrow().quantity(), 0.0001);

        int cancelled = orderRepository.cancelPendingOrders(memberId, List.of(orderId, secondOrderId));

        assertEquals(2, cancelled);
        assertEquals(
                FuturesOrder.STATUS_CANCELLED,
                orderRepository.findByMemberIdAndOrderId(memberId, orderId).orElseThrow().status()
        );
        assertEquals(
                FuturesOrder.STATUS_CANCELLED,
                orderRepository.findByMemberIdAndOrderId(memberId, secondOrderId).orElseThrow().status()
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
        accountRepository.save(new TradingAccount(
                memberId,
                memberId + "@coinzzickmock.dev",
                String.valueOf(memberId),
                100000,
                100000
        ));
    }
}
