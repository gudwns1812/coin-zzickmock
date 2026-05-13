package coin.coinzzickmock.feature.positionpeek.application.result;

import java.time.Instant;
import java.util.List;

public record PositionPeekSnapshotRecord(
        Long id,
        String peekId,
        Long viewerMemberId,
        Long targetMemberId,
        String targetTokenFingerprint,
        String targetDisplayNameSnapshot,
        Integer rankAtUse,
        String leaderboardModeAtUse,
        Instant createdAt,
        List<PositionPeekPublicPositionResult> positions
) {
    public PositionPeekSnapshotRecord withIdAndCreatedAt(Long id, Instant createdAt) {
        return new PositionPeekSnapshotRecord(
                id,
                peekId,
                viewerMemberId,
                targetMemberId,
                targetTokenFingerprint,
                targetDisplayNameSnapshot,
                rankAtUse,
                leaderboardModeAtUse,
                createdAt,
                positions
        );
    }
}
