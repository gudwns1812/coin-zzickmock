package coin.coinzzickmock.feature.leaderboard.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import coin.coinzzickmock.feature.leaderboard.application.repository.LeaderboardProjectionRepository;
import coin.coinzzickmock.feature.leaderboard.application.dto.LeaderboardMemberRankResult;
import coin.coinzzickmock.feature.leaderboard.application.dto.LeaderboardResult;
import coin.coinzzickmock.feature.leaderboard.application.store.LeaderboardSnapshotResult;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardEntry;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardMode;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardSnapshot;
import coin.coinzzickmock.feature.positionpeek.application.service.PositionPeekTargetTokenCodec;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class GetLeaderboardServiceTest {
    @Test
    void leaderboardReadDoesNotOpenTransactionBeforeRedisSnapshotLookup() throws NoSuchMethodException {
        assertNull(GetLeaderboardService.class.getAnnotation(Transactional.class));

        Method anonymousRead = GetLeaderboardService.class.getDeclaredMethod("get", LeaderboardMode.class, int.class);
        Method memberRead = GetLeaderboardService.class.getDeclaredMethod(
                "get",
                LeaderboardMode.class,
                int.class,
                Long.class
        );

        assertNull(anonymousRead.getAnnotation(Transactional.class));
        assertNull(memberRead.getAnnotation(Transactional.class));
    }

    @Test
    void ranksProfitRateFromRealizedWalletBalance() {
        GetLeaderboardService service = service(List.of(
                entry(1L, "Flat", 100_000),
                entry(2L, "Winner", 124_580),
                entry(3L, "Loss", 95_000)
        ));

        LeaderboardResult result = service.get(LeaderboardMode.PROFIT_RATE, 3);

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
        GetLeaderboardService service = service(List.of(
                entry(1L, "SmallProfitRate", 120_000),
                entry(2L, "LargestWallet", 150_000)
        ));

        LeaderboardResult result = service.get(LeaderboardMode.WALLET_BALANCE, 1);

        assertEquals("walletBalance", result.mode());
        assertEquals(1, result.entries().size());
        assertEquals("LargestWallet", result.entries().get(0).nickname());
        assertEquals(150_000, result.entries().get(0).walletBalance(), 0.0001);
    }

    @Test
    void usesRedisStoreWhenSnapshotExists() {
        GetLeaderboardService service = service(
                List.of(entry(10L, "Database", 200_000)),
                new InMemorySnapshotStore(List.of(entry(11L, "Redis", 130_000)))
        );

        LeaderboardResult result = service.get(LeaderboardMode.PROFIT_RATE, 5);

        assertEquals("redis", result.source());
        assertEquals("Redis", result.entries().get(0).nickname());
    }

    @Test
    void keepsRedisSnapshotOrderWithoutApplicationResort() {
        GetLeaderboardService service = service(
                List.of(entry(10L, "Database", 200_000)),
                new InMemorySnapshotStore(List.of(
                        entry(11L, "RedisFirst", 100_000),
                        entry(12L, "RedisSecond", 150_000)
                ))
        );

        LeaderboardResult result = service.get(LeaderboardMode.PROFIT_RATE, 2);

        assertEquals("redis", result.source());
        assertEquals("RedisFirst", result.entries().get(0).nickname());
        assertEquals(1, result.entries().get(0).rank());
        assertEquals("RedisSecond", result.entries().get(1).nickname());
        assertEquals(2, result.entries().get(1).rank());
    }

    @Test
    void fallsBackToDatabaseWhenRedisSnapshotIsEmpty() {
        GetLeaderboardService service = service(
                List.of(entry(10L, "Database", 200_000)),
                new InMemorySnapshotStore(List.of())
        );

        LeaderboardResult result = service.get(LeaderboardMode.PROFIT_RATE, 5);

        assertEquals("database", result.source());
        assertEquals("Database", result.entries().get(0).nickname());
    }


    @Test
    void fallsBackToDatabaseWhenSnapshotStoreReadFails() {
        GetLeaderboardService service = service(
                List.of(entry(10L, "Database", 200_000)),
                new FailingSnapshotStore()
        );

        LeaderboardResult result = service.get(LeaderboardMode.PROFIT_RATE, 5);

        assertEquals("database", result.source());
        assertEquals("Database", result.entries().get(0).nickname());
    }

    @Test
    void calculatesMyRankFromDatabaseByCountingHigherScoresOnly() {
        GetLeaderboardService service = service(List.of(
                entry(1L, "First", 150_000),
                entry(2L, "TieLowerMemberId", 120_000),
                entry(3L, "TieHigherMemberId", 120_000),
                entry(4L, "Loss", 90_000)
        ));

        LeaderboardResult result = service.get(LeaderboardMode.PROFIT_RATE, 2, 3L);

        assertEquals("database", result.source());
        assertEquals(2, result.myRank().orElseThrow().rank());
        assertEquals(2, result.entries().size());
    }

    @Test
    void returnsMyRankFromRedisSnapshotWithoutDatabaseFallback() {
        GetLeaderboardService service = service(
                List.of(entry(10L, "Database", 200_000)),
                new InMemorySnapshotStore(
                        List.of(entry(11L, "Redis", 130_000)),
                        Optional.of(7)
                )
        );

        LeaderboardResult result = service.get(LeaderboardMode.PROFIT_RATE, 1, 99L);

        assertEquals("redis", result.source());
        assertEquals(7, result.myRank().orElseThrow().rank());
    }

    private GetLeaderboardService service(List<LeaderboardEntry> entries) {
        return service(entries, new InMemorySnapshotStore(List.of()));
    }

    private GetLeaderboardService service(List<LeaderboardEntry> entries, InMemorySnapshotStore store) {
        return new GetLeaderboardService(
                new InMemoryProjectionRepository(entries),
                store,
                new PositionPeekTargetTokenCodec()
        );
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

    private static class FailingSnapshotStore extends InMemorySnapshotStore {
        private FailingSnapshotStore() {
            super(List.of());
        }

        @Override
        public Optional<LeaderboardSnapshotResult> findSnapshot(
                LeaderboardMode mode,
                int limit,
                Long currentMemberId
        ) {
            throw new IllegalStateException("snapshot store read failed");
        }
    }

    private static class InMemorySnapshotStore extends coin.coinzzickmock.testsupport.TestLeaderboardSnapshotStore {
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
