package coin.coinzzickmock.feature.positionpeek.application.result;

public record PositionPeekTargetResult(
        Integer rank,
        String nickname,
        Double walletBalance,
        Double profitRate,
        String targetToken
) {
}
