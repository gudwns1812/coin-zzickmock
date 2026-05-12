package coin.coinzzickmock.feature.leaderboard.web;

import coin.coinzzickmock.feature.leaderboard.application.result.LeaderboardMemberRankResult;

public record LeaderboardMemberRankResponse(
        int rank
) {
    public static LeaderboardMemberRankResponse from(LeaderboardMemberRankResult result) {
        return new LeaderboardMemberRankResponse(result.rank());
    }
}
