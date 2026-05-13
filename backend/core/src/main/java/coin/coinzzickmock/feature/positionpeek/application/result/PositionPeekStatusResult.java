package coin.coinzzickmock.feature.positionpeek.application.result;

public record PositionPeekStatusResult(
        PositionPeekTargetResult target,
        PositionPeekSnapshotResult latestSnapshot,
        int remainingPeekItemCount
) {
    public static PositionPeekStatusResult from(
            PositionPeekTargetResult target,
            PositionPeekSnapshotResult latestSnapshot,
            int remainingPeekItemCount
    ) {
        return new PositionPeekStatusResult(target, latestSnapshot, remainingPeekItemCount);
    }
}
