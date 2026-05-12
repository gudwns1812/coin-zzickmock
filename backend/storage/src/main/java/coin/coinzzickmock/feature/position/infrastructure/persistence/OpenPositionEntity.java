package coin.coinzzickmock.feature.position.infrastructure.persistence;

import coin.coinzzickmock.common.persistence.AuditableEntity;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "open_positions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_open_position_member_symbol_side",
                columnNames = {"member_id", "symbol", "position_side"}
        )
)
public class OpenPositionEntity extends AuditableEntity {
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

    @Column(name = "quantity", nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity;

    @Column(name = "entry_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal entryPrice;

    @Column(name = "mark_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal markPrice;

    @Column(name = "liquidation_price", precision = 19, scale = 4)
    private BigDecimal liquidationPrice;

    @Column(name = "unrealized_pnl", nullable = false, precision = 19, scale = 4)
    private BigDecimal unrealizedPnl;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "original_quantity", nullable = false, precision = 19, scale = 8)
    private BigDecimal originalQuantity;

    @Column(name = "accumulated_closed_quantity", nullable = false, precision = 19, scale = 8)
    private BigDecimal accumulatedClosedQuantity;

    @Column(name = "accumulated_exit_notional", nullable = false, precision = 19, scale = 4)
    private BigDecimal accumulatedExitNotional;

    @Column(name = "accumulated_realized_pnl", nullable = false, precision = 19, scale = 4)
    private BigDecimal accumulatedRealizedPnl;

    @Column(name = "accumulated_open_fee", nullable = false, precision = 19, scale = 4)
    private BigDecimal accumulatedOpenFee;

    @Column(name = "accumulated_close_fee", nullable = false, precision = 19, scale = 4)
    private BigDecimal accumulatedCloseFee;

    @Column(name = "accumulated_funding_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal accumulatedFundingCost;

    @Column(name = "take_profit_price", precision = 19, scale = 4)
    private BigDecimal takeProfitPrice;

    @Column(name = "stop_loss_price", precision = 19, scale = 4)
    private BigDecimal stopLossPrice;

    @Column(name = "version", nullable = false)
    private long version;

    protected OpenPositionEntity() {
    }

    public static OpenPositionEntity from(Long memberId, PositionSnapshot positionSnapshot) {
        OpenPositionEntity entity = new OpenPositionEntity();
        entity.memberId = memberId;
        entity.apply(positionSnapshot);
        return entity;
    }

    public void apply(PositionSnapshot positionSnapshot) {
        this.symbol = positionSnapshot.symbol();
        this.positionSide = positionSnapshot.positionSide();
        this.marginMode = positionSnapshot.marginMode();
        this.leverage = positionSnapshot.leverage();
        this.quantity = decimal(positionSnapshot.quantity());
        this.entryPrice = decimal(positionSnapshot.entryPrice());
        this.markPrice = decimal(positionSnapshot.markPrice());
        this.liquidationPrice = positionSnapshot.liquidationPrice() == null ? null : decimal(positionSnapshot.liquidationPrice());
        this.unrealizedPnl = decimal(positionSnapshot.unrealizedPnl());
        this.openedAt = positionSnapshot.openedAt();
        this.originalQuantity = decimal(positionSnapshot.originalQuantity());
        this.accumulatedClosedQuantity = decimal(positionSnapshot.accumulatedClosedQuantity());
        this.accumulatedExitNotional = decimal(positionSnapshot.accumulatedExitNotional());
        this.accumulatedRealizedPnl = decimal(positionSnapshot.accumulatedRealizedPnl());
        this.accumulatedOpenFee = decimal(positionSnapshot.accumulatedOpenFee());
        this.accumulatedCloseFee = decimal(positionSnapshot.accumulatedCloseFee());
        this.accumulatedFundingCost = decimal(positionSnapshot.accumulatedFundingCost());
        this.takeProfitPrice = positionSnapshot.takeProfitPrice() == null ? null : decimal(positionSnapshot.takeProfitPrice());
        this.stopLossPrice = positionSnapshot.stopLossPrice() == null ? null : decimal(positionSnapshot.stopLossPrice());
        this.version = positionSnapshot.version();
    }

    public PositionSnapshot toDomain() {
        return PositionSnapshot.restore(
                symbol,
                positionSide,
                marginMode,
                leverage,
                quantity.doubleValue(),
                entryPrice.doubleValue(),
                markPrice.doubleValue(),
                liquidationPrice == null ? null : liquidationPrice.doubleValue(),
                unrealizedPnl.doubleValue(),
                openedAt,
                originalQuantity.doubleValue(),
                accumulatedClosedQuantity.doubleValue(),
                accumulatedExitNotional.doubleValue(),
                accumulatedRealizedPnl.doubleValue(),
                accumulatedOpenFee.doubleValue(),
                accumulatedCloseFee.doubleValue(),
                accumulatedFundingCost.doubleValue(),
                takeProfitPrice == null ? null : takeProfitPrice.doubleValue(),
                stopLossPrice == null ? null : stopLossPrice.doubleValue(),
                version
        );
    }

    public Long memberId() {
        return memberId;
    }

    public long version() {
        return version;
    }

    private static BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value);
    }
}
