package coin.coinzzickmock.feature.order.web;

import coin.coinzzickmock.feature.order.application.realtime.TradingExecutionEvent;
import java.math.BigDecimal;

public record TradingExecutionEventResponse(
        String type,
        String orderId,
        String symbol,
        String positionSide,
        String marginMode,
        BigDecimal quantity,
        BigDecimal executionPrice,
        BigDecimal realizedPnl,
        String message
) {
    static TradingExecutionEventResponse from(TradingExecutionEvent event) {
        return new TradingExecutionEventResponse(
                event.type(),
                event.orderId(),
                event.symbol(),
                event.positionSide(),
                event.marginMode(),
                event.quantity(),
                event.executionPrice(),
                event.realizedPnl(),
                event.message()
        );
    }
}
