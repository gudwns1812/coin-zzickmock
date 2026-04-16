package coin.coinzzickmock.feature.order.infrastructure.persistence;

import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "futures_orders")
public class FuturesOrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true, length = 64)
    private String orderId;

    @Column(name = "member_id", nullable = false, length = 64)
    private String memberId;

    @Column(name = "symbol", nullable = false, length = 30)
    private String symbol;

    @Column(name = "position_side", nullable = false, length = 20)
    private String positionSide;

    @Column(name = "order_type", nullable = false, length = 20)
    private String orderType;

    @Column(name = "margin_mode", nullable = false, length = 20)
    private String marginMode;

    @Column(name = "leverage", nullable = false)
    private int leverage;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity;

    @Column(name = "limit_price", precision = 19, scale = 4)
    private BigDecimal limitPrice;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "fee_type", nullable = false, length = 20)
    private String feeType;

    @Column(name = "estimated_fee", nullable = false, precision = 19, scale = 4)
    private BigDecimal estimatedFee;

    @Column(name = "execution_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal executionPrice;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FuturesOrderEntity() {
    }

    public static FuturesOrderEntity from(String memberId, FuturesOrder futuresOrder) {
        FuturesOrderEntity entity = new FuturesOrderEntity();
        entity.orderId = futuresOrder.orderId();
        entity.memberId = memberId;
        entity.symbol = futuresOrder.symbol();
        entity.positionSide = futuresOrder.positionSide();
        entity.orderType = futuresOrder.orderType();
        entity.marginMode = futuresOrder.marginMode();
        entity.leverage = futuresOrder.leverage();
        entity.quantity = decimal(futuresOrder.quantity());
        entity.limitPrice = futuresOrder.limitPrice() == null ? null : decimal(futuresOrder.limitPrice());
        entity.status = futuresOrder.status();
        entity.feeType = futuresOrder.feeType();
        entity.estimatedFee = decimal(futuresOrder.estimatedFee());
        entity.executionPrice = decimal(futuresOrder.executionPrice());
        return entity;
    }

    public FuturesOrder toDomain() {
        return new FuturesOrder(
                orderId,
                symbol,
                positionSide,
                orderType,
                marginMode,
                leverage,
                quantity.doubleValue(),
                limitPrice == null ? null : limitPrice.doubleValue(),
                status,
                feeType,
                estimatedFee.doubleValue(),
                executionPrice.doubleValue()
        );
    }

    private static BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value);
    }
}
