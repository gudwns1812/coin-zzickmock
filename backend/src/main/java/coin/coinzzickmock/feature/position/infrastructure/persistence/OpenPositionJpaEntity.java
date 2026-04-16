package coin.coinzzickmock.feature.position.infrastructure.persistence;

import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "open_positions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_open_position_member_symbol_side_mode",
                columnNames = {"member_id", "symbol", "position_side", "margin_mode"}
        )
)
public class OpenPositionJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false, length = 64)
    private String memberId;

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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OpenPositionJpaEntity() {
    }

    public static OpenPositionJpaEntity from(String memberId, PositionSnapshot positionSnapshot) {
        OpenPositionJpaEntity entity = new OpenPositionJpaEntity();
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
    }

    public PositionSnapshot toDomain() {
        return new PositionSnapshot(
                symbol,
                positionSide,
                marginMode,
                leverage,
                quantity.doubleValue(),
                entryPrice.doubleValue(),
                markPrice.doubleValue(),
                liquidationPrice == null ? null : liquidationPrice.doubleValue(),
                unrealizedPnl.doubleValue()
        );
    }

    public String memberId() {
        return memberId;
    }

    private static BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value);
    }
}
