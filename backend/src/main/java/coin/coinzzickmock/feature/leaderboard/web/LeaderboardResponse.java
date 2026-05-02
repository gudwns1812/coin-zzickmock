package coin.coinzzickmock.feature.leaderboard.web;

import coin.coinzzickmock.feature.leaderboard.application.result.LeaderboardResult;
import java.time.Instant;
import java.util.List;

public record LeaderboardResponse(
        String mode,
        String source,
        Instant lastRefreshedAt,
        List<LeaderboardEntryResponse> entries
) {
    public static LeaderboardResponse from(LeaderboardResult result) {
        return new LeaderboardResponse(
                result.mode(),
                result.source(),
                result.lastRefreshedAt(),
                result.entries().stream()
                        .map(LeaderboardEntryResponse::from)
                        .toList()
        );
    }
}
