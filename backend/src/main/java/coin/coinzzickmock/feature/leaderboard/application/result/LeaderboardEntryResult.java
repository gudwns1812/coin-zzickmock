package coin.coinzzickmock.feature.leaderboard.application.result;

public record LeaderboardEntryResult(
        int rank,
        String nickname,
        double walletBalance,
        double profitRate
) {
}
