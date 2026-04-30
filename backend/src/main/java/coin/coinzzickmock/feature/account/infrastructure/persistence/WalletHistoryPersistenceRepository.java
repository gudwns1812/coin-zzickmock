package coin.coinzzickmock.feature.account.infrastructure.persistence;

import coin.coinzzickmock.feature.account.application.repository.WalletHistoryRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.account.domain.WalletHistorySnapshot;
import coin.coinzzickmock.feature.account.domain.WalletHistorySource;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class WalletHistoryPersistenceRepository implements WalletHistoryRepository {
    private final WalletHistoryEntityRepository walletHistoryEntityRepository;

    @Override
    @Transactional
    public void record(TradingAccount account, WalletHistorySource source, Instant recordedAt) {
        saveIfAbsent(account, source, recordedAt);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletHistorySnapshot> findByMemberIdBetween(
            Long memberId,
            Instant fromInclusive,
            Instant toInclusive
    ) {
        return walletHistoryEntityRepository
                .findAllByMemberIdAndRecordedAtBetweenOrderByRecordedAtAsc(memberId, fromInclusive, toInclusive)
                .stream()
                .map(WalletHistoryEntity::toDomain)
                .toList();
    }

    void saveIfAbsent(TradingAccount account, WalletHistorySource source, Instant recordedAt) {
        if (walletHistoryEntityRepository.existsBySourceTypeAndSourceReference(
                source.sourceType(),
                source.sourceReference()
        )) {
            return;
        }

        try {
            walletHistoryEntityRepository.save(WalletHistoryEntity.from(account, source, recordedAt));
        } catch (DataIntegrityViolationException ignored) {
            // A concurrent transaction recorded the same source; the unique key makes the write idempotent.
        }
    }
}
