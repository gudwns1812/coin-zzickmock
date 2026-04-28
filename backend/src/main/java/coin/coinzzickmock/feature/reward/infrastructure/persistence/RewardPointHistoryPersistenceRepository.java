package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import coin.coinzzickmock.feature.reward.application.repository.RewardPointHistoryRepository;
import coin.coinzzickmock.feature.reward.domain.RewardPointHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class RewardPointHistoryPersistenceRepository implements RewardPointHistoryRepository {
    private final RewardPointHistoryEntityRepository rewardPointHistoryEntityRepository;

    @Override
    @Transactional
    public RewardPointHistory save(RewardPointHistory history) {
        return rewardPointHistoryEntityRepository.save(RewardPointHistoryEntity.from(history)).toDomain();
    }
}
