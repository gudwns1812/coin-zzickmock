package coin.coinzzickmock.feature.leaderboard.application.store;

import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardEntry;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardMode;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardSnapshot;
import java.util.Optional;

public interface LeaderboardSnapshotStore {
    Optional<LeaderboardSnapshotResult> findSnapshot(
            LeaderboardMode mode,
            int limit,
            Long currentMemberId
    );

    void replace(LeaderboardSnapshot snapshot);

    void update(LeaderboardEntry entry);

    void remove(Long memberId);
}
