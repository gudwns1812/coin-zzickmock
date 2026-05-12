package coin.coinzzickmock.feature.position.infrastructure.persistence;

import coin.coinzzickmock.feature.position.domain.PositionHistory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "position_history")
public class PositionHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "symbol", nullable = false, length = 30)
    private String symbol;

    @Column(name = "position_side", nullable = false, length = 20)
    private String positionSide;

    @Column(name = "margin_mode", nullable = false, length = 20)
    private String marginMode;

    @Column(name = "leverage", nullable = false)
    private int leverage;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "average_entry_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal averageEntryPrice;

    @Column(name = "average_exit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal averageExitPrice;

    @Column(name = "position_size", nullable = false, precision = 19, scale = 8)
    private BigDecimal positionSize;

    @Column(name = "realized_pnl", nullable = false, precision = 19, scale = 4)
    private BigDecimal realizedPnl;

    @Column(name = "gross_realized_pnl", nullable = false, precision = 19, scale = 4)
    private BigDecimal grossRealizedPnl;

    @Column(name = "open_fee", nullable = false, precision = 19, scale = 4)
    private BigDecimal openFee;

    @Column(name = "close_fee", nullable = false, precision = 19, scale = 4)
    private BigDecimal closeFee;

    @Column(name = "total_fee", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalFee;

    @Column(name = "funding_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal fundingCost;

    @Column(name = "net_realized_pnl", nullable = false, precision = 19, scale = 4)
    private BigDecimal netRealizedPnl;

    @Column(name = "roi", nullable = false, precision = 19, scale = 8)
    private BigDecimal roi;

    @Column(name = "closed_at", nullable = false)
    private Instant closedAt;

    @Column(name = "close_reason", nullable = false, length = 30)
    private String closeReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PositionHistoryEntity() {
    }

    public static PositionHistoryEntity from(Long memberId, PositionHistory history) {
        PositionHistoryEntity entity = new PositionHistoryEntity();
        entity.memberId = memberId;
        entity.symbol = history.symbol();
        entity.positionSide = history.positionSide();
        entity.marginMode = history.marginMode();
        entity.leverage = history.leverage();
        entity.openedAt = history.openedAt();
        entity.averageEntryPrice = decimal(history.averageEntryPrice());
        entity.averageExitPrice = decimal(history.averageExitPrice());
        entity.positionSize = decimal(history.positionSize());
        entity.realizedPnl = decimal(history.realizedPnl());
        entity.grossRealizedPnl = decimal(history.grossRealizedPnl());
        entity.openFee = decimal(history.openFee());
        entity.closeFee = decimal(history.closeFee());
        entity.totalFee = decimal(history.totalFee());
        entity.fundingCost = decimal(history.fundingCost());
        entity.netRealizedPnl = decimal(history.netRealizedPnl());
        entity.roi = decimal(history.roi());
        entity.closedAt = history.closedAt();
        entity.closeReason = history.closeReason();
        return entity;
    }

    public PositionHistory toDomain() {
        return new PositionHistory(
                symbol,
                positionSide,
                marginMode,
                leverage,
                openedAt,
                averageEntryPrice.doubleValue(),
                averageExitPrice.doubleValue(),
                positionSize.doubleValue(),
                realizedPnl.doubleValue(),
                grossRealizedPnl.doubleValue(),
                openFee.doubleValue(),
                closeFee.doubleValue(),
                totalFee.doubleValue(),
                fundingCost.doubleValue(),
                netRealizedPnl.doubleValue(),
                roi.doubleValue(),
                closedAt,
                closeReason
        );
    }

    public Long memberId() {
        return memberId;
    }

    private static BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value);
    }
}
