package coin.coinzzickmock.feature.leaderboard.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.leaderboard.application.port.LeaderboardProjectionRepository;
import coin.coinzzickmock.feature.leaderboard.application.port.LeaderboardSnapshotStore;
import coin.coinzzickmock.feature.leaderboard.application.result.LeaderboardResult;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardEntry;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardMode;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardSnapshot;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetLeaderboardServiceTest {
    @Test
    void ranksProfitRateFromRealizedWalletBalance() {
        GetLeaderboardService service = new GetLeaderboardService(
                new InMemoryProjectionRepository(List.of(
                        entry(1L, "Flat", 100_000),
                        entry(2L, "Winner", 124_580),
                        entry(3L, "Loss", 95_000)
                )),
                List.of()
        );

        LeaderboardResult result = service.get("profitRate", "3");

        assertEquals("database", result.source());
        assertEquals("Winner", result.entries().get(0).nickname());
        assertEquals(0.2458, result.entries().get(0).profitRate(), 0.000001);
        assertEquals("Flat", result.entries().get(1).nickname());
        assertEquals(0, result.entries().get(1).profitRate(), 0.000001);
        assertEquals("Loss", result.entries().get(2).nickname());
        assertEquals(-0.05, result.entries().get(2).profitRate(), 0.000001);
    }

    @Test
    void ranksWalletBalanceModeByWalletUsdt() {
        GetLeaderboardService service = new GetLeaderboardService(
                new InMemoryProjectionRepository(List.of(
                        entry(1L, "SmallProfitRate", 120_000),
                        entry(2L, "LargestWallet", 150_000)
                )),
                List.of()
        );

        LeaderboardResult result = service.get("walletBalance", "1");

        assertEquals("walletBalance", result.mode());
        assertEquals(1, result.entries().size());
        assertEquals("LargestWallet", result.entries().get(0).nickname());
        assertEquals(150_000, result.entries().get(0).walletBalance(), 0.0001);
    }

    @Test
    void usesRedisStoreWhenSnapshotExists() {
        GetLeaderboardService service = new GetLeaderboardService(
                new InMemoryProjectionRepository(List.of(entry(10L, "Database", 200_000))),
                List.of(new InMemorySnapshotStore(List.of(entry(11L, "Redis", 130_000))))
        );

        LeaderboardResult result = service.get(null, null);

        assertEquals("redis", result.source());
        assertEquals("Redis", result.entries().get(0).nickname());
    }

    @Test
    void fallsBackToDatabaseWhenRedisSnapshotIsEmpty() {
        GetLeaderboardService service = new GetLeaderboardService(
                new InMemoryProjectionRepository(List.of(entry(10L, "Database", 200_000))),
                List.of(new InMemorySnapshotStore(List.of()))
        );

        LeaderboardResult result = service.get("profitRate", "5");

        assertEquals("database", result.source());
        assertEquals("Database", result.entries().get(0).nickname());
    }

    @Test
    void rejectsInvalidModeAndLimit() {
        GetLeaderboardService service = new GetLeaderboardService(
                new InMemoryProjectionRepository(List.of()),
                List.of()
        );

        CoreException invalidMode = assertThrows(CoreException.class, () -> service.get("unrealizedPnl", "5"));
        assertEquals(ErrorCode.INVALID_REQUEST, invalidMode.errorCode());

        CoreException invalidLimit = assertThrows(CoreException.class, () -> service.get("profitRate", "0"));
        assertEquals(ErrorCode.INVALID_REQUEST, invalidLimit.errorCode());

        CoreException nonNumericLimit = assertThrows(CoreException.class, () -> service.get("profitRate", "many"));
        assertEquals(ErrorCode.INVALID_REQUEST, nonNumericLimit.errorCode());
    }

    private static LeaderboardEntry entry(Long memberId, String nickname, double walletBalance) {
        return new LeaderboardEntry(
                memberId,
                nickname,
                walletBalance,
                (walletBalance - TradingAccount.INITIAL_WALLET_BALANCE) / TradingAccount.INITIAL_WALLET_BALANCE,
                Instant.parse("2026-04-26T00:00:00Z")
        );
    }

    private record InMemoryProjectionRepository(List<LeaderboardEntry> entries) implements LeaderboardProjectionRepository {
        @Override
        public List<LeaderboardEntry> findAll() {
            return entries;
        }

        @Override
        public Optional<LeaderboardEntry> findByMemberId(Long memberId) {
            return entries.stream()
                    .filter(entry -> entry.memberId().equals(memberId))
                    .findFirst();
        }
    }

    private static class InMemorySnapshotStore implements LeaderboardSnapshotStore {
        private final List<LeaderboardEntry> entries;

        private InMemorySnapshotStore(List<LeaderboardEntry> entries) {
            this.entries = new ArrayList<>(entries);
        }

        @Override
        public Optional<LeaderboardSnapshot> findTop(LeaderboardMode mode, int limit, int tieSlack) {
            return Optional.of(new LeaderboardSnapshot(entries, Instant.parse("2026-04-26T00:00:01Z")));
        }

        @Override
        public void replace(LeaderboardSnapshot snapshot) {
        }

        @Override
        public void update(LeaderboardEntry entry) {
        }
    }
}
