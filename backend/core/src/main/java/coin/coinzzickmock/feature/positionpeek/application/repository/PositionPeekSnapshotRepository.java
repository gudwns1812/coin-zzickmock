package coin.coinzzickmock.feature.positionpeek.application.repository;

import coin.coinzzickmock.feature.positionpeek.application.result.PositionPeekSnapshotRecord;
import java.util.Optional;

public interface PositionPeekSnapshotRepository {
    PositionPeekSnapshotRecord save(PositionPeekSnapshotRecord snapshot);

    Optional<PositionPeekSnapshotRecord> findByPeekIdAndViewerMemberId(String peekId, Long viewerMemberId);

    Optional<PositionPeekSnapshotRecord> findLatestByViewerMemberIdAndTargetMemberId(Long viewerMemberId, Long targetMemberId);
}
