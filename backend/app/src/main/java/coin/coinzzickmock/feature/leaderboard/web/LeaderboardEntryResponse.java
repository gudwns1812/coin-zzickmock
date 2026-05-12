package coin.coinzzickmock.feature.leaderboard.web;

import coin.coinzzickmock.feature.leaderboard.application.result.LeaderboardEntryResult;

public record LeaderboardEntryResponse(
        int rank,
        String nickname,
        double walletBalance,
        double profitRate
) {
    public static LeaderboardEntryResponse from(LeaderboardEntryResult result) {
        return new LeaderboardEntryResponse(
                result.rank(),
                result.nickname(),
                result.walletBalance(),
                result.profitRate()
        );
    }
}
