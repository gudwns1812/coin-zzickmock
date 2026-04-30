package coin.coinzzickmock.feature.leaderboard.domain;

import java.time.Instant;

public record LeaderboardEntry(
        Long memberId,
        String nickname,
        double walletBalance,
        double profitRate,
        Instant updatedAt
) {
}
