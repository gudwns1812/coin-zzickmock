package coin.coinzzickmock.feature.leaderboard.web;

import coin.coinzzickmock.feature.leaderboard.application.result.LeaderboardEntryResult;
import coin.coinzzickmock.feature.positionpeek.application.result.PositionPeekTargetResult;

public record LeaderboardEntryResponse(
        int rank,
        String nickname,
        double walletBalance,
        double profitRate,
        String targetToken
) {
    public static LeaderboardEntryResponse from(LeaderboardEntryResult result) {
        return new LeaderboardEntryResponse(
                result.rank(),
                result.nickname(),
                result.walletBalance(),
                result.profitRate(),
                result.targetToken()
        );
    }

    public static LeaderboardEntryResponse from(PositionPeekTargetResult result) {
        return new LeaderboardEntryResponse(
                result.rank(),
                result.nickname(),
                result.walletBalance() == null ? 0 : result.walletBalance(),
                result.profitRate() == null ? 0 : result.profitRate(),
                result.targetToken()
        );
    }
}
