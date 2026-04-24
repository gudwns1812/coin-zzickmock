package coin.coinzzickmock.feature.order.application.realtime;

public record TradingExecutionEvent(
        String memberId,
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
    public static TradingExecutionEvent orderFilled(
            String memberId,
            String orderId,
            String symbol,
            String positionSide,
            String marginMode,
            double quantity,
            double executionPrice
    ) {
        return new TradingExecutionEvent(
                memberId,
                "ORDER_FILLED",
                orderId,
                symbol,
                positionSide,
                marginMode,
                quantity,
                executionPrice,
                0d,
                "지정가 주문이 체결되었습니다."
        );
    }

    public static TradingExecutionEvent positionLiquidated(
            String memberId,
            String symbol,
            String positionSide,
            String marginMode,
            double quantity,
            double executionPrice,
            double realizedPnl
    ) {
        return new TradingExecutionEvent(
                memberId,
                "POSITION_LIQUIDATED",
                null,
                symbol,
                positionSide,
                marginMode,
                quantity,
                executionPrice,
                realizedPnl,
                "포지션이 강제 청산되었습니다."
        );
    }
}
