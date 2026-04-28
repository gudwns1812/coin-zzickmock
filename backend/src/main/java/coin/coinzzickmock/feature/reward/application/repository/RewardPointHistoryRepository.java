package coin.coinzzickmock.feature.reward.application.repository;

import coin.coinzzickmock.feature.reward.domain.RewardPointHistory;

public interface RewardPointHistoryRepository {
    RewardPointHistory save(RewardPointHistory history);
}
