package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RewardPointHistoryEntityRepository extends JpaRepository<RewardPointHistoryEntity, Long> {
    List<RewardPointHistoryEntity> findByMemberIdOrderByIdDesc(Long memberId);

    long countByMemberId(Long memberId);

    void deleteAllByMemberId(Long memberId);
}
