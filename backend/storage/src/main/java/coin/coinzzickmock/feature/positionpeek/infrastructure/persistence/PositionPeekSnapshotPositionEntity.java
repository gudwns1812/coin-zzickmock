package coin.coinzzickmock.feature.positionpeek.infrastructure.persistence;

import coin.coinzzickmock.common.persistence.AuditableEntity;
import coin.coinzzickmock.feature.positionpeek.application.result.PositionPeekPublicPositionResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "position_peek_snapshot_positions")
public class PositionPeekSnapshotPositionEntity extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "peek_snapshot_id", nullable = false)
    private PositionPeekSnapshotEntity snapshot;

    @Column(name = "symbol", nullable = false, length = 30)
    private String symbol;

    @Column(name = "position_side", nullable = false, length = 20)
    private String positionSide;

    @Column(name = "leverage", nullable = false)
    private int leverage;

    @Column(name = "position_size", nullable = false)
    private double positionSize;

    @Column(name = "entry_price")
    private Double entryPrice;

    @Column(name = "notional_value", nullable = false)
    private double notionalValue;

    @Column(name = "unrealized_pnl", nullable = false)
    private double unrealizedPnl;

    @Column(name = "roi", nullable = false)
    private double roi;

    protected PositionPeekSnapshotPositionEntity() {
    }

    public PositionPeekSnapshotPositionEntity(
            PositionPeekSnapshotEntity snapshot,
            PositionPeekPublicPositionResult position
    ) {
        this.snapshot = snapshot;
        this.symbol = position.symbol();
        this.positionSide = position.positionSide();
        this.leverage = position.leverage();
        this.positionSize = position.positionSize();
        this.entryPrice = position.entryPrice();
        this.notionalValue = position.notionalValue();
        this.unrealizedPnl = position.unrealizedPnl();
        this.roi = position.roi();
    }

    public PositionPeekPublicPositionResult toResult() {
        return new PositionPeekPublicPositionResult(
                symbol,
                positionSide,
                leverage,
                positionSize,
                entryPrice,
                notionalValue,
                unrealizedPnl,
                roi
        );
    }

    public void attach(PositionPeekSnapshotEntity snapshot) {
        this.snapshot = snapshot;
    }
}
