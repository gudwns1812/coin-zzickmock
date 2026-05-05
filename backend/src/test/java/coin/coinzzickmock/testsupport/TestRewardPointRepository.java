package coin.coinzzickmock.testsupport;

import coin.coinzzickmock.feature.reward.application.repository.RewardPointRepository;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import java.util.Optional;

public abstract class TestRewardPointRepository implements RewardPointRepository {
    @Override
    public Optional<RewardPointWallet> findByMemberId(Long memberId) {
        throw new UnsupportedOperationException("findByMemberId is not implemented for this test fake");
    }

    @Override
    public Optional<RewardPointWallet> findByMemberIdForUpdate(Long memberId) {
        return findByMemberId(memberId);
    }

    @Override
    public RewardPointWallet save(RewardPointWallet rewardPointWallet) {
        throw new UnsupportedOperationException("save is not implemented for this test fake");
    }
}
