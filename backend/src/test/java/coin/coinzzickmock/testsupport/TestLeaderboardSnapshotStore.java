package coin.coinzzickmock.testsupport;

import coin.coinzzickmock.feature.leaderboard.application.store.LeaderboardSnapshotResult;
import coin.coinzzickmock.feature.leaderboard.application.store.LeaderboardSnapshotStore;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardEntry;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardMode;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardSnapshot;
import java.util.Optional;

public abstract class TestLeaderboardSnapshotStore implements LeaderboardSnapshotStore {
    @Override
    public Optional<LeaderboardSnapshotResult> findSnapshot(LeaderboardMode mode, int limit, Long currentMemberId) {
        return Optional.empty();
    }

    @Override
    public void replace(LeaderboardSnapshot snapshot) {
        throw new UnsupportedOperationException("replace is not implemented for this test fake");
    }

    @Override
    public void update(LeaderboardEntry entry) {
        throw new UnsupportedOperationException("update is not implemented for this test fake");
    }

    @Override
    public void remove(Long memberId) {
        throw new UnsupportedOperationException("remove is not implemented for this test fake");
    }
}
