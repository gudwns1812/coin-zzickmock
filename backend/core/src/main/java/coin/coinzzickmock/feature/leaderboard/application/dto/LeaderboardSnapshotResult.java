package coin.coinzzickmock.feature.leaderboard.application.dto;

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
