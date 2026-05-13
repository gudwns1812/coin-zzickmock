package coin.coinzzickmock.feature.positionpeek.application.result;

import java.time.Instant;
import java.util.List;

public record PositionPeekSnapshotResult(
        String peekId,
        PositionPeekTargetResult target,
        Instant createdAt,
        List<PositionPeekPublicPositionResult> positions,
        Integer remainingPeekItemCount
) {
    public static PositionPeekSnapshotResult from(PositionPeekSnapshotRecord record, Integer remainingPeekItemCount) {
        return new PositionPeekSnapshotResult(
                record.peekId(),
                PositionPeekTargetResult.snapshot(record.rankAtUse(), record.targetDisplayNameSnapshot()),
                record.createdAt(),
                record.positions(),
                remainingPeekItemCount
        );
    }
}
