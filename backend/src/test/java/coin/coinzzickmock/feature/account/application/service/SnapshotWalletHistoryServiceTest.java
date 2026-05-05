package coin.coinzzickmock.feature.account.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.application.repository.WalletHistoryRepository;
import coin.coinzzickmock.feature.account.application.result.AccountMutationResult;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.account.domain.WalletHistorySnapshot;
import coin.coinzzickmock.feature.leaderboard.application.event.WalletBalanceChangedEvent;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SnapshotWalletHistoryServiceTest {
    @Test
    void recordsCurrentKstDayFromChangedAccountBalance() {
        InMemoryWalletHistoryRepository walletHistoryRepository = new InMemoryWalletHistoryRepository();
        SnapshotWalletHistoryService service = new SnapshotWalletHistoryService(
                new SingleAccountRepository(account(101_000, 100_500)),
                walletHistoryRepository,
                Clock.fixed(Instant.parse("2026-05-03T12:00:00Z"), ZoneOffset.UTC)
        );
        walletHistoryRepository.createBaselineIfAbsent(account(100_000, 100_000), LocalDate.of(2026, 5, 3));

        service.recordCurrentDay(1L);

        WalletHistorySnapshot snapshot = walletHistoryRepository.snapshots.get(0);
        assertThat(snapshot.snapshotDate()).isEqualTo(LocalDate.of(2026, 5, 3));
        assertThat(snapshot.baselineWalletBalance()).isEqualByComparingTo("100000");
        assertThat(snapshot.walletBalance()).isEqualByComparingTo("101000");
        assertThat(snapshot.dailyWalletChange()).isEqualByComparingTo("1000");
    }

    @Test
    void createsNextDayBaselineWithoutRecomputingYesterdayFromPostMidnightBalance() {
        InMemoryWalletHistoryRepository walletHistoryRepository = new InMemoryWalletHistoryRepository();
        walletHistoryRepository.snapshots.add(new WalletHistorySnapshot(
                1L,
                LocalDate.of(2026, 5, 3),
                money("100000"),
                money("101000"),
                money("1000"),
                Instant.parse("2026-05-03T14:59:59Z")
        ));
        SnapshotWalletHistoryService service = new SnapshotWalletHistoryService(
                new SingleAccountRepository(account(102_000, 101_500)),
                walletHistoryRepository,
                Clock.fixed(Instant.parse("2026-05-03T15:00:01Z"), ZoneOffset.UTC)
        );

        service.createTodayBaselines();

        WalletHistorySnapshot yesterday = walletHistoryRepository.snapshots.get(0);
        WalletHistorySnapshot today = walletHistoryRepository.snapshots.get(1);
        assertThat(yesterday.walletBalance()).isEqualByComparingTo("101000");
        assertThat(yesterday.dailyWalletChange()).isEqualByComparingTo("1000");
        assertThat(today.snapshotDate()).isEqualTo(LocalDate.of(2026, 5, 4));
        assertThat(today.baselineWalletBalance()).isEqualByComparingTo("101000");
        assertThat(today.walletBalance()).isEqualByComparingTo("101000");
        assertThat(today.dailyWalletChange()).isEqualByComparingTo("0");
    }

    @Test
    void recordsSnapshotByEventObservedDateAcrossKstMidnight() {
        InMemoryWalletHistoryRepository walletHistoryRepository = new InMemoryWalletHistoryRepository();
        SnapshotWalletHistoryService service = new SnapshotWalletHistoryService(
                new SingleAccountRepository(account(102_000, 101_500)),
                walletHistoryRepository,
                Clock.fixed(Instant.parse("2026-05-03T15:00:01Z"), ZoneOffset.UTC)
        );
        walletHistoryRepository.createBaselineIfAbsent(account(100_000, 100_000), LocalDate.of(2026, 5, 3));

        service.recordChangedBalance(new WalletBalanceChangedEvent(
                1L,
                101_000d,
                1L,
                Instant.parse("2026-05-03T14:59:59Z")
        ));

        WalletHistorySnapshot snapshot = walletHistoryRepository.snapshots.get(0);
        assertThat(snapshot.snapshotDate()).isEqualTo(LocalDate.of(2026, 5, 3));
        assertThat(snapshot.walletBalance()).isEqualByComparingTo("101000");
        assertThat(snapshot.dailyWalletChange()).isEqualByComparingTo("1000");
    }

    private static TradingAccount account(double walletBalance, double availableMargin) {
        return new TradingAccount(1L, "demo@coinzzickmock.dev", "Demo", walletBalance, availableMargin);
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    private record SingleAccountRepository(TradingAccount account) implements AccountRepository {
        @Override
        public Optional<TradingAccount> findByMemberId(Long memberId) {
            return Optional.of(account);
        }

        @Override
        public Optional<TradingAccount> findByMemberIdForUpdate(Long memberId) {
            return Optional.of(account);
        }

        @Override
        public List<TradingAccount> findAll() {
            return List.of(account);
        }

        @Override
        public TradingAccount create(TradingAccount account) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AccountMutationResult updateWithVersion(TradingAccount expectedAccount, TradingAccount nextAccount) {
            throw new UnsupportedOperationException();
        }
    }

    private static class InMemoryWalletHistoryRepository implements WalletHistoryRepository {
        private final List<WalletHistorySnapshot> snapshots = new ArrayList<>();

        @Override
        public void createBaselineIfAbsent(TradingAccount account, LocalDate snapshotDate) {
            if (snapshots.stream().anyMatch(snapshot -> snapshot.snapshotDate().equals(snapshotDate))) {
                return;
            }
            BigDecimal baselineWalletBalance = findLatestBefore(account.memberId(), snapshotDate)
                    .map(WalletHistorySnapshot::walletBalance)
                    .orElse(BigDecimal.valueOf(account.walletBalance()));
            snapshots.add(new WalletHistorySnapshot(
                    account.memberId(),
                    snapshotDate,
                    baselineWalletBalance,
                    baselineWalletBalance,
                    BigDecimal.ZERO,
                    Instant.now()
            ));
        }

        @Override
        public void updateCurrentDay(TradingAccount account, LocalDate snapshotDate) {
            createBaselineIfAbsent(account, snapshotDate);
            WalletHistorySnapshot current = snapshots.stream()
                    .filter(snapshot -> snapshot.snapshotDate().equals(snapshotDate))
                    .findFirst()
                    .orElseThrow();
            snapshots.remove(current);
            BigDecimal walletBalance = BigDecimal.valueOf(account.walletBalance());
            snapshots.add(new WalletHistorySnapshot(
                    account.memberId(),
                    snapshotDate,
                    current.baselineWalletBalance(),
                    walletBalance,
                    walletBalance.subtract(current.baselineWalletBalance()),
                    Instant.now()
            ));
        }

        @Override
        public Optional<WalletHistorySnapshot> findLatestBefore(Long memberId, LocalDate snapshotDate) {
            return snapshots.stream()
                    .filter(snapshot -> snapshot.memberId().equals(memberId))
                    .filter(snapshot -> snapshot.snapshotDate().isBefore(snapshotDate))
                    .reduce((first, second) -> second);
        }

        @Override
        public List<WalletHistorySnapshot> findByMemberIdBetween(
                Long memberId,
                LocalDate fromInclusive,
                LocalDate toInclusive
        ) {
            return snapshots.stream()
                    .filter(snapshot -> snapshot.memberId().equals(memberId))
                    .filter(snapshot -> !snapshot.snapshotDate().isBefore(fromInclusive))
                    .filter(snapshot -> !snapshot.snapshotDate().isAfter(toInclusive))
                    .toList();
        }
    }
}
