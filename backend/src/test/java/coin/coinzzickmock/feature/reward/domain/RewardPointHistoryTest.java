package coin.coinzzickmock.feature.reward.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RewardPointHistoryTest {
    @Test
    void createsGrantHistoryWithPositiveAmount() {
        RewardPointHistory history = RewardPointHistory.grant(1L, 10, 17, "close-order-1");

        assertEquals(RewardPointHistoryType.GRANT, history.historyType());
        assertEquals(10, history.amount());
        assertEquals(17, history.balanceAfter());
    }

    @Test
    void rejectsInvalidHistoryState() {
        assertThrows(IllegalArgumentException.class, () -> RewardPointHistory.grant(1L, 0, 17, null));
        assertThrows(IllegalArgumentException.class, () -> new RewardPointHistory(
                null,
                RewardPointHistoryType.GRANT,
                10,
                17,
                "POSITION_CLOSE_PROFIT",
                null
        ));
        assertThrows(IllegalArgumentException.class, () -> new RewardPointHistory(
                1L,
                RewardPointHistoryType.GRANT,
                10,
                -1,
                "POSITION_CLOSE_PROFIT",
                null
        ));
    }
}
