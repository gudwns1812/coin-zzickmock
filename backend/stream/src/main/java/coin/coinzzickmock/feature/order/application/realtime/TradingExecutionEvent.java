package coin.coinzzickmock.feature.order.application.realtime;

public record TradingExecutionEvent(
        Long memberId,
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
            Long memberId,
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
            Long memberId,
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

    public static TradingExecutionEvent positionClosedByTrigger(
            Long memberId,
            String symbol,
            String positionSide,
            String marginMode,
            double quantity,
            double executionPrice,
            double realizedPnl,
            String closeReason
    ) {
        String type = "TAKE_PROFIT".equals(closeReason)
                ? "POSITION_TAKE_PROFIT"
                : "POSITION_STOP_LOSS";
        String message = "TAKE_PROFIT".equals(closeReason)
                ? "TP 가격에 도달해 포지션이 종료되었습니다."
                : "SL 가격에 도달해 포지션이 종료되었습니다.";
        return new TradingExecutionEvent(
                memberId,
                type,
                null,
                symbol,
                positionSide,
                marginMode,
                quantity,
                executionPrice,
                realizedPnl,
                message
        );
    }
}
