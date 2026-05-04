package coin.coinzzickmock.feature.reward.domain;

import coin.coinzzickmock.common.error.CoreException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RewardPointWalletTest {
    @Test
    void grantsAndRefundsWholePoints() {
        RewardPointWallet wallet = new RewardPointWallet(1L, 7);

        assertEquals(10, wallet.grant(3).rewardPoint());
        assertEquals(12, wallet.refund(5).rewardPoint());
    }

    @Test
    void rejectsInvalidAddsAndOverflow() {
        RewardPointWallet wallet = new RewardPointWallet(1L, Integer.MAX_VALUE);

        assertThrows(CoreException.class, () -> wallet.grant(1));
        assertThrows(CoreException.class, () -> wallet.refund(1));
        assertThrows(CoreException.class, () -> wallet.grant(0));
        assertThrows(CoreException.class, () -> wallet.refund(0));
        assertThrows(CoreException.class, () -> new RewardPointWallet(1L, -1));
    }
}
