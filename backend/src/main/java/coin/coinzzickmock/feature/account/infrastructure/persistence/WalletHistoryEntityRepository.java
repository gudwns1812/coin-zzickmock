package coin.coinzzickmock.feature.account.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletHistoryEntityRepository extends JpaRepository<WalletHistoryEntity, Long> {
    boolean existsBySourceTypeAndSourceReference(String sourceType, String sourceReference);

    List<WalletHistoryEntity> findAllByMemberIdAndRecordedAtBetweenOrderByRecordedAtAsc(
            Long memberId,
            Instant fromInclusive,
            Instant toInclusive
    );

    void deleteAllByMemberId(Long memberId);
}
