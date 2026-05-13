package coin.coinzzickmock.feature.positionpeek.application.result;

import coin.coinzzickmock.feature.position.domain.PositionSnapshot;

public record PositionPeekPublicPositionResult(
        String symbol,
        String positionSide,
        int leverage,
        double positionSize,
        Double entryPrice,
        double notionalValue,
        double unrealizedPnl,
        double roi
) {
    public static PositionPeekPublicPositionResult from(PositionSnapshot snapshot) {
        return new PositionPeekPublicPositionResult(
                snapshot.symbol(),
                snapshot.positionSide(),
                snapshot.leverage(),
                snapshot.quantity(),
                snapshot.entryPrice(),
                Math.abs(snapshot.quantity() * snapshot.markPrice()),
                snapshot.unrealizedPnl(),
                snapshot.roi()
        );
    }
}
