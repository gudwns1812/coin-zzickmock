package coin.coinzzickmock.feature.reward.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RewardPointHistoryTest {
    @Test
    void createsGrantHistoryWithPositiveAmount() {
        RewardPointHistory history = RewardPointHistory.grant("demo-member", 10, 17, "close-order-1");

        assertEquals(RewardPointHistoryType.GRANT, history.historyType());
        assertEquals(10, history.amount());
        assertEquals(17, history.balanceAfter());
    }

    @Test
    void rejectsInvalidHistoryState() {
        assertThrows(IllegalArgumentException.class, () -> RewardPointHistory.grant("demo-member", 0, 17, null));
        assertThrows(IllegalArgumentException.class, () -> new RewardPointHistory(
                "",
                RewardPointHistoryType.GRANT,
                10,
                17,
                "POSITION_CLOSE_PROFIT",
                null
        ));
        assertThrows(IllegalArgumentException.class, () -> new RewardPointHistory(
                "demo-member",
                RewardPointHistoryType.GRANT,
                10,
                -1,
                "POSITION_CLOSE_PROFIT",
                null
        ));
    }
}
