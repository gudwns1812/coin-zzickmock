package coin.coinzzickmock.feature.leaderboard.application.result;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record LeaderboardResult(
        String mode,
        String source,
        Instant lastRefreshedAt,
        List<LeaderboardEntryResult> entries,
        Optional<LeaderboardMemberRankResult> myRank
) {
}
