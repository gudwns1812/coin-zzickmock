package coin.coinzzickmock.feature.positionpeek.application.result;

import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardEntry;

public record PositionPeekTargetResult(
        Integer rank,
        String nickname,
        Double walletBalance,
        Double profitRate,
        String targetToken
) {
    public static PositionPeekTargetResult from(LeaderboardEntry entry, int rank, String targetToken) {
        return of(rank, entry.nickname(), entry.walletBalance(), entry.profitRate(), targetToken);
    }

    public static PositionPeekTargetResult of(
            Integer rank,
            String nickname,
            Double walletBalance,
            Double profitRate,
            String targetToken
    ) {
        return new PositionPeekTargetResult(rank, nickname, walletBalance, profitRate, targetToken);
    }

    public static PositionPeekTargetResult snapshot(Integer rank, String nickname) {
        return new PositionPeekTargetResult(rank, nickname, null, null, null);
    }
}
