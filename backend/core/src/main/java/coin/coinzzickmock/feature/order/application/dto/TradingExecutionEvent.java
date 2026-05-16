package coin.coinzzickmock.feature.order.application.dto;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.ResourceBundle;

public record TradingExecutionEvent(
        Long memberId,
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
    private static final String MESSAGES_BUNDLE = "trading-execution-messages";
    private static final Locale DEFAULT_LOCALE = Locale.KOREAN;

    public TradingExecutionEvent(
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
        this(
                memberId,
                type,
                orderId,
                symbol,
                positionSide,
                marginMode,
                BigDecimal.valueOf(quantity),
                BigDecimal.valueOf(executionPrice),
                BigDecimal.valueOf(realizedPnl),
                message
        );
    }

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
                BigDecimal.valueOf(quantity),
                BigDecimal.valueOf(executionPrice),
                BigDecimal.ZERO,
                message("order.filled")
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
                BigDecimal.valueOf(quantity),
                BigDecimal.valueOf(executionPrice),
                BigDecimal.valueOf(realizedPnl),
                message("position.liquidated")
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
        return new TradingExecutionEvent(
                memberId,
                type,
                null,
                symbol,
                positionSide,
                marginMode,
                BigDecimal.valueOf(quantity),
                BigDecimal.valueOf(executionPrice),
                BigDecimal.valueOf(realizedPnl),
                triggerMessage(closeReason)
        );
    }

    private static String triggerMessage(String closeReason) {
        return "TAKE_PROFIT".equals(closeReason)
                ? message("position.take-profit")
                : message("position.stop-loss");
    }

    private static String message(String key) {
        return ResourceBundle.getBundle(MESSAGES_BUNDLE, DEFAULT_LOCALE).getString(key);
    }
}
