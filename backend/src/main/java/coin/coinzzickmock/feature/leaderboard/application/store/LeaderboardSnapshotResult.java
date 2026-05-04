package coin.coinzzickmock.feature.leaderboard.application.store;

import coin.coinzzickmock.feature.leaderboard.application.result.LeaderboardMemberRankResult;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardEntry;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record LeaderboardSnapshotResult(
        List<LeaderboardEntry> entries,
        Instant refreshedAt,
        Optional<LeaderboardMemberRankResult> myRank
) {
}
