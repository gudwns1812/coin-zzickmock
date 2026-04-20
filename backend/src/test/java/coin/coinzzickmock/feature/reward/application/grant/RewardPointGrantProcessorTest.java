package coin.coinzzickmock.feature.reward.application.grant;

import coin.coinzzickmock.feature.reward.application.command.GrantProfitPointCommand;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointRepository;
import coin.coinzzickmock.feature.reward.application.result.RewardPointResult;
import coin.coinzzickmock.feature.reward.domain.RewardPointPolicy;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RewardPointGrantProcessorTest {
    @Test
    void doesNotPersistWalletWhenGrantedPointIsZero() {
        InMemoryRewardPointRepository repository = new InMemoryRewardPointRepository(new RewardPointWallet("demo-member", 7));
        RewardPointGrantProcessor processor = new RewardPointGrantProcessor(new RewardPointPolicy(), repository);

        RewardPointResult result = processor.grant(new GrantProfitPointCommand("demo-member", -5));

        assertEquals(7, result.rewardPoint(), 0.0001);
        assertEquals("NONE", result.tierLabel());
        assertEquals(0, repository.saveCount);
    }

    private static class InMemoryRewardPointRepository implements RewardPointRepository {
        private RewardPointWallet wallet;
        private int saveCount;

        private InMemoryRewardPointRepository(RewardPointWallet wallet) {
            this.wallet = wallet;
        }

        @Override
        public Optional<RewardPointWallet> findByMemberId(String memberId) {
            return Optional.ofNullable(wallet).filter(current -> current.memberId().equals(memberId));
        }

        @Override
        public RewardPointWallet save(RewardPointWallet rewardPointWallet) {
            saveCount++;
            wallet = rewardPointWallet;
            return rewardPointWallet;
        }
    }
}
