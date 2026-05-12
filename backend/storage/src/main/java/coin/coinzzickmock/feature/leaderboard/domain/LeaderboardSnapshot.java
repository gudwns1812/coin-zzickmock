package coin.coinzzickmock.feature.leaderboard.domain;

import java.time.Instant;
import java.util.List;

public record LeaderboardSnapshot(
        List<LeaderboardEntry> entries,
        Instant refreshedAt
) {
}
