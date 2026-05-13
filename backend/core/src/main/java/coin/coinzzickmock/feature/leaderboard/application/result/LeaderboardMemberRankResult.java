package coin.coinzzickmock.feature.leaderboard.application.result;

public record LeaderboardMemberRankResult(
        int rank
) {
    public static LeaderboardMemberRankResult fromRank(long rank) {
        return new LeaderboardMemberRankResult(Math.toIntExact(rank));
    }
}
