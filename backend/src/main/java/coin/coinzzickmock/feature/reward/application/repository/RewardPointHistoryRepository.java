package coin.coinzzickmock.feature.reward.application.repository;

import coin.coinzzickmock.feature.reward.domain.RewardPointHistory;

import java.util.List;

public interface RewardPointHistoryRepository {
    RewardPointHistory save(RewardPointHistory history);

    default List<RewardPointHistory> findByMemberId(String memberId) {
        return List.of();
    }
}
