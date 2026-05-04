package coin.coinzzickmock.feature.account.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
}
