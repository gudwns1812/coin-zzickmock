package coin.coinzzickmock.feature.account.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalletHistoryEntityRepository extends JpaRepository<WalletHistoryEntity, Long> {
    Optional<WalletHistoryEntity> findByMemberIdAndSnapshotDate(Long memberId, LocalDate snapshotDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select history
              from WalletHistoryEntity history
             where history.memberId = :memberId
               and history.snapshotDate = :snapshotDate
            """)
    Optional<WalletHistoryEntity> findByMemberIdAndSnapshotDateForUpdate(
            @Param("memberId") Long memberId,
            @Param("snapshotDate") LocalDate snapshotDate
    );

    Optional<WalletHistoryEntity> findTopByMemberIdAndSnapshotDateBeforeOrderBySnapshotDateDesc(
            Long memberId,
            LocalDate snapshotDate
    );

    List<WalletHistoryEntity> findAllByMemberIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
            Long memberId,
            LocalDate fromInclusive,
            LocalDate toInclusive
    );

    void deleteAllByMemberId(Long memberId);
}
