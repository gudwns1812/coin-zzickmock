package coin.coinzzickmock.feature.positionpeek.application.result;

public record PositionPeekStatusResult(
        PositionPeekTargetResult target,
        PositionPeekSnapshotResult latestSnapshot,
        int remainingPeekItemCount
) {
}
