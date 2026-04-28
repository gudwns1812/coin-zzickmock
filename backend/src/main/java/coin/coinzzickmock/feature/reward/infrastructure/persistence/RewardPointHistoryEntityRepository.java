package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RewardPointHistoryEntityRepository extends JpaRepository<RewardPointHistoryEntity, Long> {
    List<RewardPointHistoryEntity> findByMemberIdOrderByIdDesc(String memberId);

    long countByMemberId(String memberId);
}
