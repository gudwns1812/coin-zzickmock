package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import coin.coinzzickmock.feature.reward.application.repository.RewardPointHistoryRepository;
import coin.coinzzickmock.feature.reward.domain.RewardPointHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RewardPointHistoryPersistenceRepository implements RewardPointHistoryRepository {
    private final RewardPointHistoryEntityRepository rewardPointHistoryEntityRepository;

    @Override
    @Transactional
    public RewardPointHistory save(RewardPointHistory history) {
        return rewardPointHistoryEntityRepository.save(RewardPointHistoryEntity.from(history)).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RewardPointHistory> findByMemberId(String memberId) {
        return rewardPointHistoryEntityRepository.findByMemberIdOrderByIdDesc(memberId).stream()
                .map(RewardPointHistoryEntity::toDomain)
                .toList();
    }
}
