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
}
