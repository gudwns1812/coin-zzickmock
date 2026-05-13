package coin.coinzzickmock.feature.leaderboard.application.result;

import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardEntry;

public record LeaderboardEntryResult(
        int rank,
        String nickname,
        double walletBalance,
        double profitRate,
        String targetToken
) {
    public static LeaderboardEntryResult from(LeaderboardEntry entry, int rank, String targetToken) {
        return new LeaderboardEntryResult(
                rank,
                entry.nickname(),
                entry.walletBalance(),
                entry.profitRate(),
                targetToken
        );
    }
}
