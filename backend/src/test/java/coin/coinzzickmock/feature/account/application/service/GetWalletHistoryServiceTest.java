package coin.coinzzickmock.feature.account.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.account.application.query.GetWalletHistoryQuery;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.application.repository.WalletHistoryRepository;
import coin.coinzzickmock.feature.account.application.result.AccountMutationResult;
import coin.coinzzickmock.feature.account.application.result.WalletHistoryResult;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.account.domain.WalletHistorySnapshot;
import coin.coinzzickmock.feature.account.domain.WalletHistorySource;
import java.time.Duration;
import java.time.Instant;
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
                        99_900,
                        94_900,
                        WalletHistorySource.TYPE_ORDER_FILL,
                        "order:order-1:fill",
                        Instant.now()
                )
        ));
        GetWalletHistoryService service = new GetWalletHistoryService(
                walletHistoryRepository,
                accountRepository()
        );

        List<WalletHistoryResult> results = service.execute(new GetWalletHistoryQuery(1L, null, null));

        assertThat(results).hasSize(1);
        assertThat(Duration.between(walletHistoryRepository.requestedFrom, walletHistoryRepository.requestedTo).toDays())
                .isEqualTo(30);
    }

    @Test
    void returnsCurrentAccountSnapshotWhenHistoryIsEmpty() {
        GetWalletHistoryService service = new GetWalletHistoryService(
                new RecordingWalletHistoryRepository(List.of()),
                accountRepository()
        );

        List<WalletHistoryResult> results = service.execute(new GetWalletHistoryQuery(1L, null, null));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).walletBalance()).isEqualTo(100_000d);
        assertThat(results.get(0).availableMargin()).isEqualTo(95_000d);
        assertThat(results.get(0).sourceType()).isEqualTo("CURRENT_SNAPSHOT");
    }

    @Test
    void throwsDataErrorWhenCurrentAccountSnapshotIsMissing() {
        GetWalletHistoryService service = new GetWalletHistoryService(
                new RecordingWalletHistoryRepository(List.of()),
                new AccountRepository() {
                    @Override
                    public Optional<TradingAccount> findByMemberId(Long memberId) {
                        return Optional.empty();
                    }

                    @Override
                    public AccountMutationResult updateWithVersion(
                            TradingAccount expectedAccount,
                            TradingAccount nextAccount,
                            WalletHistorySource source
                    ) {
                        throw new AssertionError("wallet history read must not mutate account");
                    }
                }
        );

        assertThatThrownBy(() -> service.execute(new GetWalletHistoryQuery(1L, null, null)))
                .isInstanceOf(CoreException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND);
    }

    private AccountRepository accountRepository() {
        return new AccountRepository() {
            @Override
            public Optional<TradingAccount> findByMemberId(Long memberId) {
                return Optional.of(new TradingAccount(memberId, "demo@coinzzickmock.dev", "Demo", 100_000, 95_000));
            }

            @Override
            public AccountMutationResult updateWithVersion(
                    TradingAccount expectedAccount,
                    TradingAccount nextAccount,
                    WalletHistorySource source
            ) {
                throw new AssertionError("wallet history read must not mutate account");
            }
        };
    }

    private static class RecordingWalletHistoryRepository implements WalletHistoryRepository {
        private final List<WalletHistorySnapshot> snapshots;
        private Instant requestedFrom;
        private Instant requestedTo;

        private RecordingWalletHistoryRepository(List<WalletHistorySnapshot> snapshots) {
            this.snapshots = new ArrayList<>(snapshots);
        }

        @Override
        public void record(TradingAccount account, WalletHistorySource source, Instant recordedAt) {
            snapshots.add(new WalletHistorySnapshot(
                    account.memberId(),
                    account.walletBalance(),
                    account.availableMargin(),
                    source.sourceType(),
                    source.sourceReference(),
                    recordedAt
            ));
        }

        @Override
        public List<WalletHistorySnapshot> findByMemberIdBetween(
                Long memberId,
                Instant fromInclusive,
                Instant toInclusive
        ) {
            requestedFrom = fromInclusive;
            requestedTo = toInclusive;
            return snapshots;
        }
    }
}
