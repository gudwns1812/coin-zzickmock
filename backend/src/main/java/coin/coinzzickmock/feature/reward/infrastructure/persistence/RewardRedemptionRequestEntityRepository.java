package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
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
               set request.status = 'APPROVED',
                   request.adminMemberId = :adminMemberId,
                   request.adminMemo = :adminMemo,
                   request.sentAt = :approvedAt
             where request.requestId = :requestId
               and request.status = 'PENDING'
            """)
    int claimPendingAsApproved(
            @Param("requestId") String requestId,
            @Param("adminMemberId") Long adminMemberId,
            @Param("adminMemo") String adminMemo,
            @Param("approvedAt") Instant approvedAt
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update RewardRedemptionRequestEntity request
               set request.status = 'REJECTED',
                   request.adminMemberId = :adminMemberId,
                   request.adminMemo = :adminMemo,
                   request.cancelledAt = :rejectedAt
             where request.requestId = :requestId
               and request.status = 'PENDING'
            """)
    int claimPendingAsRejected(
            @Param("requestId") String requestId,
            @Param("adminMemberId") Long adminMemberId,
            @Param("adminMemo") String adminMemo,
            @Param("rejectedAt") Instant rejectedAt
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update RewardRedemptionRequestEntity request
               set request.status = 'CANCELLED',
                   request.cancelledAt = :cancelledAt
             where request.requestId = :requestId
               and request.memberId = :memberId
               and request.status = 'PENDING'
            """)
    int claimPendingAsCancelled(
            @Param("requestId") String requestId,
            @Param("memberId") Long memberId,
            @Param("cancelledAt") Instant cancelledAt
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update RewardRedemptionRequestEntity request
               set request.adminMemberId = null
             where request.adminMemberId = :adminMemberId
            """)
    int clearAdminMemberId(@Param("adminMemberId") Long adminMemberId);

    void deleteAllByMemberId(Long memberId);
}
