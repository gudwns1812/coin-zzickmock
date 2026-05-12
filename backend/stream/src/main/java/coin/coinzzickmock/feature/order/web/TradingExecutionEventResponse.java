package coin.coinzzickmock.feature.order.web;

import coin.coinzzickmock.feature.order.application.realtime.TradingExecutionEvent;

public record TradingExecutionEventResponse(
        String type,
        String orderId,
        String symbol,
        String positionSide,
        String marginMode,
        double quantity,
        double executionPrice,
        double realizedPnl,
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
