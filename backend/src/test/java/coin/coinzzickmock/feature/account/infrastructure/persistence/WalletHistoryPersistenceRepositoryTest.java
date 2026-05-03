package coin.coinzzickmock.feature.account.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coin.coinzzickmock.CoinZzickmockApplication;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.application.repository.WalletHistoryRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.account.domain.WalletHistorySnapshot;
import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.feature.member.domain.MemberCredential;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = CoinZzickmockApplication.class)
@ActiveProfiles("test")
class WalletHistoryPersistenceRepositoryTest {
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private WalletHistoryRepository walletHistoryRepository;

    @Autowired
    private MemberCredentialRepository memberCredentialRepository;

    @Autowired
    private WalletHistoryEntityRepository walletHistoryEntityRepository;

    @Test
    void createsBaselineOnceAndUpdatesDailySnapshot() {
        MemberCredential member = memberCredentialRepository.save(member("wallet-history-" + System.nanoTime()));
        TradingAccount account = new TradingAccount(
                member.memberId(),
                member.memberEmail(),
                member.memberName(),
                100_000,
                100_000
        );
        LocalDate snapshotDate = LocalDate.of(2026, 5, 3);

        TradingAccount created = accountRepository.create(account);
        walletHistoryRepository.createBaselineIfAbsent(created, snapshotDate);
        walletHistoryRepository.createBaselineIfAbsent(created, snapshotDate);
        walletHistoryRepository.updateCurrentDay(
                created.reserveForFilledOrder(10, 100),
                snapshotDate
        );

        List<WalletHistorySnapshot> snapshots = walletHistoryRepository.findByMemberIdBetween(
                member.memberId(),
                snapshotDate.minusDays(1),
                snapshotDate.plusDays(1)
        );

        assertThat(snapshots).hasSize(1);
        assertThat(snapshots.get(0).snapshotDate()).isEqualTo(snapshotDate);
        assertThat(snapshots.get(0).baselineWalletBalance()).isEqualByComparingTo("100000");
        assertThat(snapshots.get(0).walletBalance()).isEqualByComparingTo("99990");
        assertThat(snapshots.get(0).dailyWalletChange()).isEqualByComparingTo("-10");
    }

    @Test
    void ignoresStaleAccountVersionSnapshotUpdates() {
        MemberCredential member = memberCredentialRepository.save(member("wallet-history-stale-" + System.nanoTime()));
        TradingAccount account = accountRepository.create(new TradingAccount(
                member.memberId(),
                member.memberEmail(),
                member.memberName(),
                100_000,
                100_000
        ));
        LocalDate snapshotDate = LocalDate.of(2026, 5, 3);

        walletHistoryRepository.createBaselineIfAbsent(account, snapshotDate);
        walletHistoryRepository.updateCurrentDay(account.withVersion(2).settlePositionClose(2_000, 0, 0), snapshotDate);
        walletHistoryRepository.updateCurrentDay(account.withVersion(1).settlePositionClose(500, 0, 0), snapshotDate);

        List<WalletHistorySnapshot> snapshots = walletHistoryRepository.findByMemberIdBetween(
                member.memberId(),
                snapshotDate,
                snapshotDate
        );

        assertThat(snapshots).hasSize(1);
        assertThat(snapshots.get(0).walletBalance()).isEqualByComparingTo("102000");
        assertThat(snapshots.get(0).dailyWalletChange()).isEqualByComparingTo("2000");
    }

    @Test
    void rejectsDuplicateMemberSnapshotDate() {
        MemberCredential member = memberCredentialRepository.save(member("wallet-history-unique-" + System.nanoTime()));
        TradingAccount account = accountRepository.create(new TradingAccount(
                member.memberId(),
                member.memberEmail(),
                member.memberName(),
                100_000,
                100_000
        ));
        LocalDate snapshotDate = LocalDate.of(2026, 5, 3);

        walletHistoryEntityRepository.saveAndFlush(WalletHistoryEntity.baseline(account, snapshotDate, java.time.Instant.now()));

        assertThatThrownBy(() -> walletHistoryEntityRepository.saveAndFlush(
                WalletHistoryEntity.baseline(account, snapshotDate, java.time.Instant.now())
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    private MemberCredential member(String account) {
        return MemberCredential.register(
                account,
                "hashed-password",
                "Wallet",
                "Wallet",
                account + "@coinzzickmock.dev",
                "010-0000-0000",
                "00000",
                "Seoul",
                "101",
                0
        );
    }
}
