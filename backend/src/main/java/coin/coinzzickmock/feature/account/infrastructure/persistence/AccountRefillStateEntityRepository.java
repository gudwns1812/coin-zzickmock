package coin.coinzzickmock.feature.account.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRefillStateEntityRepository extends JpaRepository<AccountRefillStateEntity, Long> {
    Optional<AccountRefillStateEntity> findByMemberIdAndRefillDate(Long memberId, LocalDate refillDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select state
            from AccountRefillStateEntity state
            where state.memberId = :memberId
              and state.refillDate = :refillDate
            """)
    Optional<AccountRefillStateEntity> findWithLockingByMemberIdAndRefillDate(
            @Param("memberId") Long memberId,
            @Param("refillDate") LocalDate refillDate
    );

    @Modifying(flushAutomatically = true)
    @Query(value = """
            insert into account_refill_states (member_id, refill_date, remaining_count, version)
            values (:memberId, :refillDate, 1, 0)
            on duplicate key update remaining_count = remaining_count
            """, nativeQuery = true)
    int insertDailyStateIfMissing(
            @Param("memberId") Long memberId,
            @Param("refillDate") LocalDate refillDate
    );
}
