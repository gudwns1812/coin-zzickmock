package coin.coinzzickmock.feature.leaderboard.domain;

import java.time.Instant;

public record LeaderboardEntry(
        String memberId,
        String nickname,
        double walletBalance,
        double profitRate,
        Instant updatedAt
) {
}
