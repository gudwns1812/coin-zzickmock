package coin.coinzzickmock.feature.reward.application.service;

import coin.coinzzickmock.feature.reward.application.repository.RewardPointRepository;
import coin.coinzzickmock.feature.reward.application.result.RewardPointResult;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GetRewardPointServiceTest {
    @Test
    void returnsZeroWalletWhenMemberHasNoRewardWallet() {
        GetRewardPointService service = new GetRewardPointService(new InMemoryRewardPointRepository());

        RewardPointResult result = service.get(1L);

        assertEquals(0, result.rewardPoint());
        assertEquals("POINT_WALLET", result.tierLabel());
    }

    private static class InMemoryRewardPointRepository implements RewardPointRepository {
        @Override
        public Optional<RewardPointWallet> findByMemberId(Long memberId) {
            return Optional.empty();
        }

        @Override
        public RewardPointWallet save(RewardPointWallet rewardPointWallet) {
            return rewardPointWallet;
        }
    }
}
