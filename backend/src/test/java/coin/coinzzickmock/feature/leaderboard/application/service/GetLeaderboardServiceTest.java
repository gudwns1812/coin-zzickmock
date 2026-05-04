package coin.coinzzickmock.feature.leaderboard.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.leaderboard.application.repository.LeaderboardProjectionRepository;
import coin.coinzzickmock.feature.leaderboard.application.result.LeaderboardResult;
import coin.coinzzickmock.feature.leaderboard.application.result.LeaderboardMemberRankResult;
import coin.coinzzickmock.feature.leaderboard.application.store.LeaderboardSnapshotResult;
import coin.coinzzickmock.feature.leaderboard.application.store.LeaderboardSnapshotStore;
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
    void calculatesMyRankFromDatabaseByCountingHigherScoresOnly() {
        GetLeaderboardService service = new GetLeaderboardService(
                new InMemoryProjectionRepository(List.of(
                        entry(1L, "First", 150_000),
                        entry(2L, "TieLowerMemberId", 120_000),
                        entry(3L, "TieHigherMemberId", 120_000),
                        entry(4L, "Loss", 90_000)
                )),
                List.of()
        );

        LeaderboardResult result = service.get("profitRate", "2", 3L);

        assertEquals("database", result.source());
        assertEquals(2, result.myRank().orElseThrow().rank());
        assertEquals(2, result.entries().size());
    }

    @Test
    void returnsMyRankFromRedisSnapshotWithoutDatabaseFallback() {
        GetLeaderboardService service = new GetLeaderboardService(
                new InMemoryProjectionRepository(List.of(entry(10L, "Database", 200_000))),
                List.of(new InMemorySnapshotStore(
                        List.of(entry(11L, "Redis", 130_000)),
                        Optional.of(7)
                ))
        );

        LeaderboardResult result = service.get("profitRate", "1", 99L);

        assertEquals("redis", result.source());
        assertEquals(7, result.myRank().orElseThrow().rank());
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
        return LeaderboardEntry.fromWalletBalance(
                memberId,
                nickname,
                walletBalance,
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
        private final Optional<Integer> myRank;

        private InMemorySnapshotStore(List<LeaderboardEntry> entries) {
            this(entries, Optional.empty());
        }

        private InMemorySnapshotStore(List<LeaderboardEntry> entries, Optional<Integer> myRank) {
            this.entries = new ArrayList<>(entries);
            this.myRank = myRank;
        }

        @Override
        public Optional<LeaderboardSnapshotResult> findSnapshot(
                LeaderboardMode mode,
                int limit,
                Long currentMemberId
        ) {
            return Optional.of(new LeaderboardSnapshotResult(
                    entries,
                    Instant.parse("2026-04-26T00:00:01Z"),
                    myRank.map(LeaderboardMemberRankResult::new)
            ));
        }

        @Override
        public void replace(LeaderboardSnapshot snapshot) {
        }

        @Override
        public void update(LeaderboardEntry entry) {
        }

        @Override
        public void remove(Long memberId) {
        }
    }
}
