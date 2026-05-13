package coin.coinzzickmock.feature.positionpeek.web;

import coin.coinzzickmock.feature.positionpeek.application.result.PositionPeekStatusResult;
import java.time.Instant;
import java.util.List;

public record PositionPeekStatusResponse(
        PositionPeekTargetResponse target,
        PositionPeekSnapshotResponse latestSnapshot,
        int remainingPeekItemCount,
        String peekId,
        Instant createdAt,
        List<PositionPeekPublicPositionResponse> positions
) {
    public static PositionPeekStatusResponse from(PositionPeekStatusResult result) {
        PositionPeekSnapshotResponse snapshot = result.latestSnapshot() == null
                ? null
                : PositionPeekSnapshotResponse.from(result.latestSnapshot());
        return new PositionPeekStatusResponse(
                PositionPeekTargetResponse.from(result.target()),
                snapshot,
                result.remainingPeekItemCount(),
                snapshot == null ? null : snapshot.peekId(),
                snapshot == null ? null : snapshot.createdAt(),
                snapshot == null ? List.of() : snapshot.positions()
        );
    }
}
