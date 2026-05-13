package coin.coinzzickmock.feature.positionpeek.infrastructure.persistence;

import coin.coinzzickmock.common.persistence.AuditableEntity;
import coin.coinzzickmock.feature.positionpeek.application.result.PositionPeekPublicPositionResult;
import coin.coinzzickmock.feature.positionpeek.application.result.PositionPeekSnapshotRecord;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "position_peek_snapshots")
public class PositionPeekSnapshotEntity extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "peek_id", nullable = false, unique = true, length = 36)
    private String peekId;

    @Column(name = "viewer_member_id", nullable = false)
    private Long viewerMemberId;

    @Column(name = "target_member_id", nullable = false)
    private Long targetMemberId;

    @Column(name = "target_token_fingerprint", nullable = false, length = 128)
    private String targetTokenFingerprint;

    @Column(name = "target_display_name_snapshot", nullable = false, length = 100)
    private String targetDisplayNameSnapshot;

    @Column(name = "discovery_source", length = 50)
    private String discoverySource;

    @Column(name = "rank_at_use")
    private Integer rankAtUse;

    @Column(name = "leaderboard_mode_at_use", length = 30)
    private String leaderboardModeAtUse;

    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<PositionPeekSnapshotPositionEntity> positions = new ArrayList<>();

    protected PositionPeekSnapshotEntity() {
    }

    public static PositionPeekSnapshotEntity from(PositionPeekSnapshotRecord record) {
        PositionPeekSnapshotEntity entity = new PositionPeekSnapshotEntity();
        entity.apply(record);
        return entity;
    }

    public void apply(PositionPeekSnapshotRecord record) {
        this.peekId = record.peekId();
        this.viewerMemberId = record.viewerMemberId();
        this.targetMemberId = record.targetMemberId();
        this.targetTokenFingerprint = record.targetTokenFingerprint();
        this.targetDisplayNameSnapshot = record.targetDisplayNameSnapshot();
        this.discoverySource = null;
        this.rankAtUse = record.rankAtUse();
        this.leaderboardModeAtUse = record.leaderboardModeAtUse();
        this.positions.clear();
        record.positions().stream()
                .map(position -> new PositionPeekSnapshotPositionEntity(this, position))
                .forEach(this.positions::add);
    }

    public void setPositions(List<PositionPeekPublicPositionResult> snapshotPositions) {
        this.positions.clear();
        snapshotPositions.stream()
                .map(position -> new PositionPeekSnapshotPositionEntity(this, position))
                .forEach(this.positions::add);
    }

    public Long getId() {
        return id;
    }

    public PositionPeekSnapshotRecord toRecord() {
        return new PositionPeekSnapshotRecord(
                id,
                peekId,
                viewerMemberId,
                targetMemberId,
                targetTokenFingerprint,
                targetDisplayNameSnapshot,
                rankAtUse,
                leaderboardModeAtUse,
                createdAt(),
                positions.stream().map(PositionPeekSnapshotPositionEntity::toResult).toList()
        );
    }
}
