package coin.coinzzickmock.feature.order.domain;

import coin.coinzzickmock.common.trading.LiquidationFormula;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class OrderPreviewPolicy {
    private static final String LIQUIDATION_PRICE_TYPE_EXACT = "EXACT";
    private static final String LIQUIDATION_PRICE_TYPE_UNAVAILABLE = "UNAVAILABLE";
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
        Double estimatedLiquidationPrice = LiquidationFormula.liquidationPrice(
                request.positionSide(),
                request.marginMode(),
                request.leverage(),
                entryPrice.doubleValue()
        );

        return new OrderPreview(
                decision.feeType(),
                estimatedFee.doubleValue(),
                estimatedInitialMargin.doubleValue(),
                estimatedLiquidationPrice,
                estimatedLiquidationPrice == null
                        ? LIQUIDATION_PRICE_TYPE_UNAVAILABLE
                        : LIQUIDATION_PRICE_TYPE_EXACT,
                entryPrice.doubleValue(),
                decision.executable()
        );
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value);
    }
}
