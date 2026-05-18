package coin.coinzzickmock.feature.order.application.dto;

import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.order.domain.OrderPreview;

public record CreateOrderResult(
        String orderId,
        String status,
        String symbol,
        String feeType,
        double estimatedFee,
        double estimatedInitialMargin,
        Double estimatedLiquidationPrice,
        String estimatedLiquidationPriceType,
        double executionPrice
) {
    public static CreateOrderResult from(FuturesOrder order, OrderPreview preview) {
        return new CreateOrderResult(
                order.orderId(),
                order.status(),
                order.symbol(),
                order.feeType(),
                preview.estimatedFee(),
                preview.estimatedInitialMargin(),
                preview.estimatedLiquidationPrice(),
                preview.estimatedLiquidationPriceType(),
                order.executionPrice()
        );
    }
}
