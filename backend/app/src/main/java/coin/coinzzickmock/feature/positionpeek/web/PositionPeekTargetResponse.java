package coin.coinzzickmock.feature.positionpeek.web;

import coin.coinzzickmock.feature.positionpeek.application.result.PositionPeekTargetResult;

public record PositionPeekTargetResponse(
        Integer rank,
        String nickname,
        Double walletBalance,
        Double profitRate,
        String targetToken
) {
    public static PositionPeekTargetResponse from(PositionPeekTargetResult result) {
        return new PositionPeekTargetResponse(
                result.rank(),
                result.nickname(),
                result.walletBalance(),
                result.profitRate(),
                result.targetToken()
        );
    }
}
