package coin.coinzzickmock.feature.order.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import coin.coinzzickmock.CoinZzickmockApplication;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.dto.PendingOrderCandidate;
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
    void narrowsExecutablePendingLimitCandidatesByPriceRangeAndSide() {
        Long memberId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        saveAccount(memberId);
        String openLongId = "open-long-" + UUID.randomUUID();
        String openShortId = "open-short-" + UUID.randomUUID();
        String closeLongId = "close-long-" + UUID.randomUUID();
        String closeShortId = "close-short-" + UUID.randomUUID();
        String outOfRangeId = "out-of-range-" + UUID.randomUUID();
        String conditionalId = "conditional-" + UUID.randomUUID();

        orderRepository.save(memberId, FuturesOrder.place(
                openLongId, "ETHUSDT", "LONG", "LIMIT", "ISOLATED", 10,
                0.1, 101.0, false, "MAKER", 0, 101.0
        ));
        orderRepository.save(memberId, FuturesOrder.place(
                openShortId, "ETHUSDT", "SHORT", "LIMIT", "ISOLATED", 10,
                0.1, 102.0, false, "MAKER", 0, 102.0
        ));
        orderRepository.save(memberId, FuturesOrder.place(
                closeLongId, "ETHUSDT", "LONG", "LIMIT", FuturesOrder.PURPOSE_CLOSE_POSITION,
                "ISOLATED", 10, 0.1, 103.0, false, "MAKER", 0, 103.0
        ));
        orderRepository.save(memberId, FuturesOrder.place(
                closeShortId, "ETHUSDT", "SHORT", "LIMIT", FuturesOrder.PURPOSE_CLOSE_POSITION,
                "ISOLATED", 10, 0.1, 104.0, false, "MAKER", 0, 104.0
        ));
        orderRepository.save(memberId, FuturesOrder.place(
                outOfRangeId, "ETHUSDT", "SHORT", "LIMIT", "ISOLATED", 10,
                0.1, 120.0, false, "MAKER", 0, 120.0
        ));
        orderRepository.save(memberId, FuturesOrder.conditionalClose(
                conditionalId, "ETHUSDT", "LONG", "ISOLATED", 10, 0.1,
                103.0, FuturesOrder.TRIGGER_TYPE_TAKE_PROFIT, "oco-" + UUID.randomUUID()
        ));

        List<String> upMoveIds = orderRepository.findExecutablePendingLimitOrders("ETHUSDT", 100.0, 110.0, true)
                .stream()
                .map(PendingOrderCandidate::orderId)
                .toList();
        List<String> downMoveIds = orderRepository.findExecutablePendingLimitOrders("ETHUSDT", 100.0, 110.0, false)
                .stream()
                .map(PendingOrderCandidate::orderId)
                .toList();

        assertEquals(List.of(openShortId, closeLongId), upMoveIds);
        assertEquals(List.of(openLongId, closeShortId), downMoveIds);
        assertFalse(upMoveIds.contains(outOfRangeId));
        assertFalse(upMoveIds.contains(conditionalId));
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
    void claimsPendingLimitFillOnlyAtExpectedCurrentPrice() {
        Long memberId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        String orderId = "limit-order-" + UUID.randomUUID();
        saveAccount(memberId);

        orderRepository.save(memberId, FuturesOrder.place(
                orderId,
                "BTCUSDT",
                "LONG",
                FuturesOrder.TYPE_LIMIT,
                "ISOLATED",
                10,
                0.1,
                99.0,
                false,
                "MAKER",
                0.001485,
                99.0
        ));

        List<PendingOrderCandidate> candidates = orderRepository.findExecutablePendingLimitOrders(
                "BTCUSDT",
                95.0,
                100.0,
                false
        );
        assertEquals(List.of(orderId), candidates.stream().map(PendingOrderCandidate::orderId).toList());

        assertTrue(orderRepository.updatePendingLimitPrice(
                memberId,
                orderId,
                96.0,
                "MAKER",
                0.00144,
                96.0
        ).isPresent());
        assertFalse(orderRepository.claimPendingLimitFill(
                memberId,
                orderId,
                candidates.get(0).order().limitPrice(),
                candidates.get(0).order().limitPrice(),
                "MAKER",
                0.001485
        ).isPresent());
        assertTrue(orderRepository.claimPendingLimitFill(
                memberId,
                orderId,
                96.0,
                96.0,
                "MAKER",
                0.00144
        ).isPresent());

        FuturesOrder filled = orderRepository.findByMemberIdAndOrderId(memberId, orderId).orElseThrow();
        assertEquals(FuturesOrder.STATUS_FILLED, filled.status());
        assertEquals(96.0, filled.executionPrice(), 0.0001);
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

    @Test
    void updatesPendingNonConditionalLimitPriceOnlyWhenEditable() {
        Long memberId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        saveAccount(memberId);
        String editableId = "editable-" + UUID.randomUUID();
        String filledId = "filled-" + UUID.randomUUID();
        String conditionalId = "conditional-" + UUID.randomUUID();

        orderRepository.save(memberId, FuturesOrder.place(
                editableId, "BTCUSDT", "LONG", FuturesOrder.TYPE_LIMIT, "ISOLATED", 10,
                0.1, 90.0, false, "MAKER", 0.00135, 90.0
        ));
        orderRepository.save(memberId, FuturesOrder.place(
                filledId, "BTCUSDT", "LONG", FuturesOrder.TYPE_LIMIT, "ISOLATED", 10,
                0.1, 91.0, true, "TAKER", 0.00455, 91.0
        ));
        orderRepository.save(memberId, FuturesOrder.conditionalClose(
                conditionalId, "BTCUSDT", "LONG", "ISOLATED", 10, 0.1,
                120.0, FuturesOrder.TRIGGER_TYPE_TAKE_PROFIT, "oco-" + UUID.randomUUID()
        ));

        assertTrue(orderRepository.updatePendingLimitPrice(
                memberId, editableId, 95.0, "MAKER", 0.001425, 95.0
        ).isPresent());
        assertFalse(orderRepository.updatePendingLimitPrice(
                memberId, filledId, 96.0, "MAKER", 0.00144, 96.0
        ).isPresent());
        assertFalse(orderRepository.updatePendingLimitPrice(
                memberId, conditionalId, 121.0, "MAKER", 0, 121.0
        ).isPresent());

        assertEquals(95.0, orderRepository.findByMemberIdAndOrderId(memberId, editableId).orElseThrow().limitPrice(), 0.0001);
        assertEquals(91.0, orderRepository.findByMemberIdAndOrderId(memberId, filledId).orElseThrow().limitPrice(), 0.0001);
        assertEquals(120.0, orderRepository.findByMemberIdAndOrderId(memberId, conditionalId).orElseThrow().triggerPrice(), 0.0001);
    }

    @Test
    void updatesPendingConditionalCloseOrderInPlace() {
        Long memberId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        String orderId = "tp-order-" + UUID.randomUUID();
        saveAccount(memberId);

        orderRepository.save(memberId, FuturesOrder.conditionalClose(
                orderId,
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                0.2,
                110.0,
                FuturesOrder.TRIGGER_TYPE_TAKE_PROFIT,
                null
        ));

        FuturesOrder updated = orderRepository.updatePendingConditionalCloseOrder(
                memberId,
                orderId,
                20,
                0.15,
                115.0,
                "oco-updated"
        );

        assertEquals(orderId, updated.orderId());
        assertEquals(FuturesOrder.STATUS_PENDING, updated.status());
        assertEquals(20, updated.leverage());
        assertEquals(0.15, updated.quantity(), 0.0001);
        assertEquals(115.0, updated.triggerPrice(), 0.0001);
        assertEquals("oco-updated", updated.ocoGroupId());
        assertEquals(1, orderRepository.findByMemberId(memberId).size());
        FuturesOrder persisted = orderRepository.findByMemberIdAndOrderId(memberId, orderId).orElseThrow();
        assertEquals(115.0, persisted.triggerPrice(), 0.0001);
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
}
