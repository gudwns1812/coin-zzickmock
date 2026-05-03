package coin.coinzzickmock.feature.account.infrastructure.persistence;

import coin.coinzzickmock.feature.account.application.repository.WalletHistoryRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.account.domain.WalletHistorySnapshot;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class WalletHistoryPersistenceRepository implements WalletHistoryRepository {
    private final WalletHistoryEntityRepository walletHistoryEntityRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void createBaselineIfAbsent(TradingAccount account, LocalDate snapshotDate) {
        findOrCreateBaseline(baselineAccount(account, snapshotDate), snapshotDate);
    }

    @Override
    @Transactional
    public void updateCurrentDay(TradingAccount account, LocalDate snapshotDate) {
        WalletHistoryEntity entity = findOrCreateBaseline(baselineAccount(account, snapshotDate), snapshotDate);
        entity.updateFrom(account, Instant.now());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WalletHistorySnapshot> findLatestBefore(Long memberId, LocalDate snapshotDate) {
        return walletHistoryEntityRepository
                .findTopByMemberIdAndSnapshotDateBeforeOrderBySnapshotDateDesc(memberId, snapshotDate)
                .map(WalletHistoryEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletHistorySnapshot> findByMemberIdBetween(
            Long memberId,
            LocalDate fromInclusive,
            LocalDate toInclusive
    ) {
        return walletHistoryEntityRepository
                .findAllByMemberIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(memberId, fromInclusive, toInclusive)
                .stream()
                .map(WalletHistoryEntity::toDomain)
                .toList();
    }

    private TradingAccount baselineAccount(TradingAccount account, LocalDate snapshotDate) {
        return findLatestBefore(account.memberId(), snapshotDate)
                .map(previous -> new TradingAccount(
                        account.memberId(),
                        account.memberEmail(),
                        account.memberName(),
                        previous.walletBalance().doubleValue(),
                        account.availableMargin(),
                        account.version()
                ))
                .orElse(account);
    }

    private WalletHistoryEntity findOrCreateBaseline(TradingAccount account, LocalDate snapshotDate) {
        return walletHistoryEntityRepository.findByMemberIdAndSnapshotDateForUpdate(account.memberId(), snapshotDate)
                .orElseGet(() -> createBaseline(account, snapshotDate));
    }

    private WalletHistoryEntity createBaseline(TradingAccount account, LocalDate snapshotDate) {
        Instant now = Instant.now();
        jdbcTemplate.update(
                """
                        INSERT INTO wallet_history (
                            member_id, snapshot_date, baseline_wallet_balance, wallet_balance,
                            daily_wallet_change, account_version, recorded_at, created_at, updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            updated_at = updated_at
                        """,
                account.memberId(),
                snapshotDate,
                decimal(account.walletBalance()),
                decimal(account.walletBalance()),
                BigDecimal.ZERO,
                -1,
                Timestamp.from(now),
                Timestamp.from(now),
                Timestamp.from(now)
        );
        return walletHistoryEntityRepository.findByMemberIdAndSnapshotDateForUpdate(account.memberId(), snapshotDate)
                .orElseThrow(() -> new IllegalStateException(
                        "wallet history baseline was inserted or already present but cannot be found. memberId="
                                + account.memberId() + " snapshotDate=" + snapshotDate
                ));
    }

    private static BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value);
    }
}
