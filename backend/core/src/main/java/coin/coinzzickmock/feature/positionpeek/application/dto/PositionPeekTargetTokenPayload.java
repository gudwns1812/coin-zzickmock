package coin.coinzzickmock.feature.positionpeek.application.dto;

public record PositionPeekTargetTokenPayload(
        Long targetMemberId,
        Integer rank,
        String nickname,
        Double walletBalance,
        Double profitRate,
        String leaderboardMode
) {
}
