package coin.coinzzickmock.feature.positionpeek.infrastructure.persistence;

import coin.coinzzickmock.feature.positionpeek.application.repository.PositionPeekSnapshotRepository;
import coin.coinzzickmock.feature.positionpeek.application.result.PositionPeekSnapshotRecord;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class PositionPeekSnapshotPersistenceRepository implements PositionPeekSnapshotRepository {
    private final PositionPeekSnapshotEntityRepository repository;

    @Override
    @Transactional
    public PositionPeekSnapshotRecord save(PositionPeekSnapshotRecord snapshot) {
        PositionPeekSnapshotEntity entity = snapshot.id() == null
                ? PositionPeekSnapshotEntity.from(snapshot)
                : repository.findById(snapshot.id())
                .map(existing -> {
                    existing.apply(snapshot);
                    return existing;
                })
                .orElseGet(() -> PositionPeekSnapshotEntity.from(snapshot));
        PositionPeekSnapshotEntity saved = repository.save(entity);
        return saved.toRecord();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PositionPeekSnapshotRecord> findByPeekIdAndViewerMemberId(String peekId, Long viewerMemberId) {
        return repository.findByPeekIdAndViewerMemberId(peekId, viewerMemberId)
                .map(PositionPeekSnapshotEntity::toRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PositionPeekSnapshotRecord> findLatestByViewerMemberIdAndTargetMemberId(
            Long viewerMemberId,
            Long targetMemberId
    ) {
        return repository.findFirstByViewerMemberIdAndTargetMemberIdOrderByCreatedAtDesc(viewerMemberId, targetMemberId)
                .map(PositionPeekSnapshotEntity::toRecord);
    }
}
