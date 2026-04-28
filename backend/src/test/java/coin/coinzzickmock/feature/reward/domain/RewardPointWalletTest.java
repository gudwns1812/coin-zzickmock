package coin.coinzzickmock.feature.reward.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RewardPointWalletTest {
    @Test
    void grantsAndRefundsWholePoints() {
        RewardPointWallet wallet = new RewardPointWallet("demo-member", 7);

        assertEquals(10, wallet.grant(3).rewardPoint());
        assertEquals(12, wallet.refund(5).rewardPoint());
    }

    @Test
    void rejectsInvalidAddsAndOverflow() {
        RewardPointWallet wallet = new RewardPointWallet("demo-member", Integer.MAX_VALUE);

        assertThrows(IllegalArgumentException.class, () -> wallet.grant(1));
        assertThrows(IllegalArgumentException.class, () -> wallet.refund(1));
        assertThrows(IllegalArgumentException.class, () -> wallet.grant(0));
        assertThrows(IllegalArgumentException.class, () -> wallet.refund(0));
        assertThrows(IllegalArgumentException.class, () -> new RewardPointWallet("demo-member", -1));
    }
}
