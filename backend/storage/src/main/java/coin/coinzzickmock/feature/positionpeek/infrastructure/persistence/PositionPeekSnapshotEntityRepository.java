package coin.coinzzickmock.feature.positionpeek.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionPeekSnapshotEntityRepository extends JpaRepository<PositionPeekSnapshotEntity, Long> {
    Optional<PositionPeekSnapshotEntity> findByPeekIdAndViewerMemberId(String peekId, Long viewerMemberId);

    Optional<PositionPeekSnapshotEntity> findFirstByViewerMemberIdAndTargetMemberIdOrderByCreatedAtDesc(
            Long viewerMemberId,
            Long targetMemberId
    );
}
