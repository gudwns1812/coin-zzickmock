package coin.coinzzickmock.feature.order.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class OrderPreviewPolicy {
    private static final String POSITION_SIDE_LONG = "LONG";
    private static final int DIVISION_SCALE = 8;

    private final OrderPlacementPolicy orderPlacementPolicy;

    public OrderPreviewPolicy() {
        this(new OrderPlacementPolicy());
    }

    public OrderPreviewPolicy(OrderPlacementPolicy orderPlacementPolicy) {
        this.orderPlacementPolicy = orderPlacementPolicy;
    }

    public OrderPreview preview(OrderPlacementRequest request, double latestTradePrice) {
        OrderPlacementDecision decision = orderPlacementPolicy.decide(request, latestTradePrice);
        BigDecimal entryPrice = decimal(decision.estimatePrice());
        BigDecimal feeRate = decimal(decision.feeRate());
        BigDecimal quantity = decimal(request.quantity());
        BigDecimal estimatedFee = entryPrice.multiply(quantity).multiply(feeRate);
        BigDecimal estimatedInitialMargin = entryPrice.multiply(quantity)
                .divide(BigDecimal.valueOf(request.leverage()), DIVISION_SCALE, RoundingMode.HALF_UP);
        BigDecimal liquidationGap = entryPrice.divide(
                BigDecimal.valueOf(request.leverage()),
                DIVISION_SCALE,
                RoundingMode.HALF_UP
        );
        BigDecimal estimatedLiquidationPrice = isLong(request.positionSide())
                ? entryPrice.subtract(liquidationGap)
                : entryPrice.add(liquidationGap);

        return new OrderPreview(
                decision.feeType(),
                estimatedFee.doubleValue(),
                estimatedInitialMargin.doubleValue(),
                estimatedLiquidationPrice.doubleValue(),
                entryPrice.doubleValue(),
                decision.executable()
        );
    }

    private boolean isLong(String positionSide) {
        return POSITION_SIDE_LONG.equalsIgnoreCase(positionSide);
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value);
    }
}
