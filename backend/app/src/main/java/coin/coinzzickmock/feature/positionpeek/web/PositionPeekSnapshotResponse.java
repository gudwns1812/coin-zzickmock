package coin.coinzzickmock.feature.positionpeek.web;

import coin.coinzzickmock.feature.positionpeek.application.result.PositionPeekSnapshotResult;
import java.time.Instant;
import java.util.List;

public record PositionPeekSnapshotResponse(
        String peekId,
        PositionPeekTargetResponse target,
        Instant createdAt,
        List<PositionPeekPublicPositionResponse> positions,
        Integer remainingPeekItemCount
) {
    public static PositionPeekSnapshotResponse from(PositionPeekSnapshotResult result) {
        return new PositionPeekSnapshotResponse(
                result.peekId(),
                PositionPeekTargetResponse.from(result.target()),
                result.createdAt(),
                result.positions().stream()
                        .map(PositionPeekPublicPositionResponse::from)
                        .toList(),
                result.remainingPeekItemCount()
        );
    }
}
