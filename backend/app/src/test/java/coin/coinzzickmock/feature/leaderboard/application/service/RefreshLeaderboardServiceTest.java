package coin.coinzzickmock.feature.leaderboard.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.account.application.event.TradingAccountOpenedEvent;
import coin.coinzzickmock.feature.leaderboard.application.repository.LeaderboardProjectionRepository;
import coin.coinzzickmock.feature.leaderboard.application.store.LeaderboardSnapshotStore;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardEntry;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardSnapshot;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class RefreshLeaderboardServiceTest {
    @Test
    void leaderboardRefreshDoesNotKeepRedisUpdateInsideReadTransaction() throws NoSuchMethodException {
        assertThat(RefreshLeaderboardService.class.getAnnotation(Transactional.class)).isNull();

        Method refreshAll = RefreshLeaderboardService.class.getDeclaredMethod("refreshAll");
        Method refreshMember = RefreshLeaderboardService.class.getDeclaredMethod("refreshMember", Long.class);

        assertThat(refreshAll.getAnnotation(Transactional.class)).isNull();
        assertThat(refreshMember.getAnnotation(Transactional.class)).isNull();
    }

    @Test
    void recordOpenedAccountUpdatesSnapshotWithoutProjectionLookup() {
        RecordingSnapshotStore snapshotStore = new RecordingSnapshotStore();
        RefreshLeaderboardService service = new RefreshLeaderboardService(
                new FailingProjectionRepository(),
                snapshotStore
        );

        service.recordOpenedAccount(new TradingAccountOpenedEvent(
                7L,
                "opened@coinzzickmock.dev",
                "Opened",
                "Opened Nick",
                100_000d,
                100_000d,
                0L,
                Instant.parse("2026-05-02T00:00:00Z")
        ));

        assertThat(snapshotStore.updatedEntries)
                .extracting(LeaderboardEntry::memberId, LeaderboardEntry::nickname, LeaderboardEntry::walletBalance)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(7L, "Opened Nick", 100_000d));
        assertThat(snapshotStore.removedMemberIds).isEmpty();
    }

    @Test
    void refreshMemberRemovesStaleSnapshotsWhenProjectionIsMissing() {
        RecordingSnapshotStore snapshotStore = new RecordingSnapshotStore();
        RefreshLeaderboardService service = new RefreshLeaderboardService(
                new EmptyProjectionRepository(),
                snapshotStore
        );

        service.refreshMember(7L);

        assertThat(snapshotStore.removedMemberIds).containsExactly(7L);
        assertThat(snapshotStore.updatedEntries).isEmpty();
    }

    @Test
    void refreshMemberUpdatesSnapshotWhenProjectionExists() {
        RecordingSnapshotStore snapshotStore = new RecordingSnapshotStore();
        LeaderboardEntry entry = entry(7L, "Active", 120_000);
        RefreshLeaderboardService service = new RefreshLeaderboardService(
                new SingleProjectionRepository(entry),
                snapshotStore
        );

        service.refreshMember(7L);

        assertThat(snapshotStore.updatedEntries).containsExactly(entry);
        assertThat(snapshotStore.removedMemberIds).isEmpty();
    }

    private static LeaderboardEntry entry(Long memberId, String nickname, double walletBalance) {
        return LeaderboardEntry.fromWalletBalance(
                memberId,
                nickname,
                walletBalance,
                Instant.parse("2026-05-02T00:00:00Z")
        );
    }

    private static class EmptyProjectionRepository implements LeaderboardProjectionRepository {
        @Override
        public List<LeaderboardEntry> findAll() {
            return List.of();
        }

        @Override
        public Optional<LeaderboardEntry> findByMemberId(Long memberId) {
            return Optional.empty();
        }
    }

    private static class FailingProjectionRepository implements LeaderboardProjectionRepository {
        @Override
        public List<LeaderboardEntry> findAll() {
            throw new AssertionError("opened account projection must not query full leaderboard projection");
        }

        @Override
        public Optional<LeaderboardEntry> findByMemberId(Long memberId) {
            throw new AssertionError("opened account projection must not query member leaderboard projection");
        }
    }

    private record SingleProjectionRepository(LeaderboardEntry entry) implements LeaderboardProjectionRepository {
        @Override
        public List<LeaderboardEntry> findAll() {
            return List.of(entry);
        }

        @Override
        public Optional<LeaderboardEntry> findByMemberId(Long memberId) {
            return entry.memberId().equals(memberId) ? Optional.of(entry) : Optional.empty();
        }
    }

    private static class RecordingSnapshotStore extends coin.coinzzickmock.testsupport.TestLeaderboardSnapshotStore {
        private final java.util.ArrayList<LeaderboardEntry> updatedEntries = new java.util.ArrayList<>();
        private final java.util.ArrayList<Long> removedMemberIds = new java.util.ArrayList<>();

        @Override
        public void replace(LeaderboardSnapshot snapshot) {
        }

        @Override
        public void update(LeaderboardEntry entry) {
            updatedEntries.add(entry);
        }

        @Override
        public void remove(Long memberId) {
            removedMemberIds.add(memberId);
        }
    }
}
