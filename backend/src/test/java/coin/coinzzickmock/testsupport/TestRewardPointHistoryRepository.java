package coin.coinzzickmock.testsupport;

import coin.coinzzickmock.feature.reward.application.repository.RewardPointHistoryRepository;
import coin.coinzzickmock.feature.reward.domain.RewardPointHistory;
import java.util.List;

public abstract class TestRewardPointHistoryRepository implements RewardPointHistoryRepository {
    @Override
    public RewardPointHistory save(RewardPointHistory history) {
        throw new UnsupportedOperationException("save is not implemented for this test fake");
    }

    @Override
    public List<RewardPointHistory> findByMemberId(Long memberId) {
        return List.of();
    }
}
