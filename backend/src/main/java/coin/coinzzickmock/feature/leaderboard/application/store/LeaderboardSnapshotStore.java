package coin.coinzzickmock.feature.leaderboard.application.store;

import coin.coinzzickmock.feature.leaderboard.application.result.LeaderboardMemberRankResult;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardEntry;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardMode;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardSnapshot;
import java.util.Optional;

public interface LeaderboardSnapshotStore {
    Optional<LeaderboardSnapshot> findTop(LeaderboardMode mode, int limit, int tieSlack);

    default Optional<LeaderboardSnapshotResult> findSnapshot(
            LeaderboardMode mode,
            int limit,
            Optional<Long> currentMemberId
    ) {
        return findTop(mode, limit, 0)
                .map(snapshot -> new LeaderboardSnapshotResult(
                        snapshot.entries(),
                        snapshot.refreshedAt(),
                        Optional.<LeaderboardMemberRankResult>empty()
                ));
    }

    void replace(LeaderboardSnapshot snapshot);

    void update(LeaderboardEntry entry);

    void remove(Long memberId);
}
