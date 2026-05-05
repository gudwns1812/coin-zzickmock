package coin.coinzzickmock.feature.account.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.account.application.query.GetWalletHistoryQuery;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.application.repository.WalletHistoryRepository;
import coin.coinzzickmock.feature.account.application.result.AccountMutationResult;
import coin.coinzzickmock.feature.account.application.result.WalletHistoryResult;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.account.domain.WalletHistoryDate;
import coin.coinzzickmock.feature.account.domain.WalletHistorySnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetWalletHistoryServiceTest {
    @Test
    void defaultsToOneMonthWindowFromCurrentDate() {
        RecordingWalletHistoryRepository walletHistoryRepository = new RecordingWalletHistoryRepository(List.of(
                new WalletHistorySnapshot(
                        1L,
                        LocalDate.now(),
                        money("100000"),
                        money("99900"),
                        money("-100"),
                        Instant.now()
                )
        ));
        GetWalletHistoryService service = new GetWalletHistoryService(
                walletHistoryRepository,
                accountRepository()
        );

        List<WalletHistoryResult> results = service.execute(new GetWalletHistoryQuery(1L, null, null));

        assertThat(results).hasSize(1);
        assertThat(walletHistoryRepository.requestedFrom)
                .isEqualTo(walletHistoryRepository.requestedTo.minusDays(29));
    }

    @Test
    void returnsCurrentProvisionalSnapshotWhenHistoryIsEmpty() {
        GetWalletHistoryService service = new GetWalletHistoryService(
                new RecordingWalletHistoryRepository(List.of()),
                accountRepository()
        );

        List<WalletHistoryResult> results = service.execute(new GetWalletHistoryQuery(1L, null, null));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).walletBalance()).isEqualByComparingTo("100000");
        assertThat(results.get(0).dailyWalletChange()).isEqualByComparingTo("0");
    }

    @Test
    void returnsEmptyResultWhenExplicitPastRangeHasNoSnapshots() {
        GetWalletHistoryService service = new GetWalletHistoryService(
                new RecordingWalletHistoryRepository(List.of()),
                accountRepository()
        );

        List<WalletHistoryResult> results = service.execute(new GetWalletHistoryQuery(
                1L,
                Instant.parse("2026-04-01T00:00:00Z"),
                Instant.parse("2026-04-02T00:00:00Z")
        ));

        assertThat(results).isEmpty();
    }

    @Test
    void returnsEmptyResultWhenExplicitFutureRangeDoesNotIncludeToday() {
        GetWalletHistoryService service = new GetWalletHistoryService(
                new RecordingWalletHistoryRepository(List.of()),
                accountRepository()
        );
        LocalDate tomorrow = WalletHistoryDate.from(Instant.now()).plusDays(1);
        Instant tomorrowStart = tomorrow.atStartOfDay(WalletHistoryDate.REPORTING_ZONE).toInstant();

        List<WalletHistoryResult> results = service.execute(new GetWalletHistoryQuery(
                1L,
                tomorrowStart,
                tomorrowStart
        ));

        assertThat(results).isEmpty();
    }

    @Test
    void throwsDataErrorWhenDefaultCurrentSnapshotAccountIsMissing() {
        GetWalletHistoryService service = new GetWalletHistoryService(
                new RecordingWalletHistoryRepository(List.of()),
                missingAccountRepository()
        );

        assertThatThrownBy(() -> service.execute(new GetWalletHistoryQuery(1L, null, null)))
                .isInstanceOf(CoreException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND);
    }

    private AccountRepository accountRepository() {
        return new coin.coinzzickmock.testsupport.TestAccountRepository() {
            @Override
            public Optional<TradingAccount> findByMemberId(Long memberId) {
                return Optional.of(new TradingAccount(memberId, "demo@coinzzickmock.dev", "Demo", 100_000, 95_000));
            }

            @Override
            public TradingAccount create(TradingAccount account) {
                throw new AssertionError("wallet history read must not create account");
            }

            @Override
            public AccountMutationResult updateWithVersion(
                    TradingAccount expectedAccount,
                    TradingAccount nextAccount
            ) {
                throw new AssertionError("wallet history read must not mutate account");
            }
        };
    }

    private AccountRepository missingAccountRepository() {
        return new coin.coinzzickmock.testsupport.TestAccountRepository() {
            @Override
            public Optional<TradingAccount> findByMemberId(Long memberId) {
                return Optional.empty();
            }

            @Override
            public TradingAccount create(TradingAccount account) {
                throw new AssertionError("wallet history read must not create account");
            }

            @Override
            public AccountMutationResult updateWithVersion(
                    TradingAccount expectedAccount,
                    TradingAccount nextAccount
            ) {
                throw new AssertionError("wallet history read must not mutate account");
            }
        };
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    private static class RecordingWalletHistoryRepository implements WalletHistoryRepository {
        private final List<WalletHistorySnapshot> snapshots;
        private LocalDate requestedFrom;
        private LocalDate requestedTo;

        private RecordingWalletHistoryRepository(List<WalletHistorySnapshot> snapshots) {
            this.snapshots = new ArrayList<>(snapshots);
        }

        @Override
        public void createBaselineIfAbsent(TradingAccount account, LocalDate snapshotDate) {
            throw new AssertionError("wallet history read must not create snapshots");
        }

        @Override
        public void updateCurrentDay(TradingAccount account, LocalDate snapshotDate) {
            throw new AssertionError("wallet history read must not mutate snapshots");
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
            requestedFrom = fromInclusive;
            requestedTo = toInclusive;
            return snapshots.stream()
                    .filter(snapshot -> snapshot.memberId().equals(memberId))
                    .filter(snapshot -> !snapshot.snapshotDate().isBefore(fromInclusive))
                    .filter(snapshot -> !snapshot.snapshotDate().isAfter(toInclusive))
                    .toList();
        }
    }
}
