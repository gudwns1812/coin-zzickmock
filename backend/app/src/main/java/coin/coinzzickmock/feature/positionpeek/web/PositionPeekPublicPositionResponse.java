package coin.coinzzickmock.feature.positionpeek.web;

import coin.coinzzickmock.feature.positionpeek.application.result.PositionPeekPublicPositionResult;

public record PositionPeekPublicPositionResponse(
        String symbol,
        String positionSide,
        int leverage,
        double positionSize,
        double notionalValue,
        double unrealizedPnl,
        double roi
) {
    public static PositionPeekPublicPositionResponse from(PositionPeekPublicPositionResult result) {
        return new PositionPeekPublicPositionResponse(
                result.symbol(),
                result.positionSide(),
                result.leverage(),
                result.positionSize(),
                result.notionalValue(),
                result.unrealizedPnl(),
                result.roi()
        );
    }
}
