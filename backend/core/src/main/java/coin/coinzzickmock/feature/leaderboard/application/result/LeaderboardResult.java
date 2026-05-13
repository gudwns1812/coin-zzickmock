package coin.coinzzickmock.feature.leaderboard.application.result;

import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardMode;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardSnapshot;
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
    public static LeaderboardResult from(
            LeaderboardMode mode,
            String source,
            LeaderboardSnapshot snapshot,
            List<LeaderboardEntryResult> entries,
            Optional<LeaderboardMemberRankResult> myRank
    ) {
        return new LeaderboardResult(
                mode.value(),
                source,
                snapshot.refreshedAt(),
                entries,
                myRank
        );
    }
}
