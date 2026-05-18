package coin.coinzzickmock.feature.order.application.implement;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.order.domain.OrderPlacementPolicy;
import coin.coinzzickmock.feature.order.domain.OrderPlacementRequest;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class OrderEditPlanner {
    private static final String FEE_TYPE_MAKER = "MAKER";

    public EditPlan plan(FuturesOrder order, BigDecimal nextLimitPrice) {
        double nextLimitPriceValue = validatedLimitPrice(nextLimitPrice);
        validateEditable(order);
        return new EditPlan(
                nextLimitPriceValue,
                FEE_TYPE_MAKER,
                pendingMakerEstimatedFee(order, nextLimitPriceValue),
                placementRequest(order, nextLimitPriceValue)
        );
    }

    private double validatedLimitPrice(BigDecimal limitPrice) {
        if (limitPrice == null || limitPrice.signum() <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        double limitPriceValue = limitPrice.doubleValue();
        if (!Double.isFinite(limitPriceValue) || limitPriceValue <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        return limitPriceValue;
    }

    private void validateEditable(FuturesOrder order) {
        if (!order.isEditableLimitOrder()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }

    private OrderPlacementRequest placementRequest(FuturesOrder order, double limitPrice) {
        return new OrderPlacementRequest(
                order.orderPurpose(),
                order.positionSide(),
                FuturesOrder.TYPE_LIMIT,
                order.marginMode(),
                limitPrice,
                order.quantity(),
                order.leverage()
        );
    }

    private double pendingMakerEstimatedFee(FuturesOrder order, double nextLimitPrice) {
        if (order.isClosePositionOrder()) {
            return 0;
        }
        return nextLimitPrice * order.quantity() * OrderPlacementPolicy.MAKER_FEE_RATE;
    }

    public record EditPlan(
            double limitPrice,
            String feeType,
            double estimatedFee,
            OrderPlacementRequest placementRequest
    ) {
    }
}
