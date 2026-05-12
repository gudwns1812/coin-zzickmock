package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RewardRedemptionRequestEntityRepository extends JpaRepository<RewardRedemptionRequestEntity, Long> {
    Optional<RewardRedemptionRequestEntity> findByRequestId(String requestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RewardRedemptionRequestEntity> findWithLockingByRequestId(String requestId);

    List<RewardRedemptionRequestEntity> findByMemberIdOrderByRequestedAtDesc(Long memberId);

    List<RewardRedemptionRequestEntity> findByStatusOrderByRequestedAtDesc(String status);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update RewardRedemptionRequestEntity request
               set request.adminMemberId = null
             where request.adminMemberId = :adminMemberId
            """)
    int clearAdminMemberId(@Param("adminMemberId") Long adminMemberId);

    void deleteAllByMemberId(Long memberId);
}
