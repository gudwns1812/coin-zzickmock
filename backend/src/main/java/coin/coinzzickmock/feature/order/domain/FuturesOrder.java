package coin.coinzzickmock.feature.order.domain;

import java.time.Instant;

public record FuturesOrder(
        String orderId,
        String symbol,
        String positionSide,
        String orderType,
        String orderPurpose,
        String marginMode,
        int leverage,
        double quantity,
        Double limitPrice,
        String status,
        String feeType,
        double estimatedFee,
        double executionPrice,
        Instant orderTime,
        Double triggerPrice,
        String triggerType,
        String triggerSource,
        String ocoGroupId
) {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_FILLED = "FILLED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String PURPOSE_OPEN_POSITION = "OPEN_POSITION";
    public static final String PURPOSE_CLOSE_POSITION = "CLOSE_POSITION";
    public static final String TYPE_MARKET = "MARKET";
    public static final String TRIGGER_TYPE_TAKE_PROFIT = "TAKE_PROFIT";
    public static final String TRIGGER_TYPE_STOP_LOSS = "STOP_LOSS";
    public static final String TRIGGER_SOURCE_MARK_PRICE = "MARK_PRICE";

    public FuturesOrder(
            String orderId,
            String symbol,
            String positionSide,
            String orderType,
            String orderPurpose,
            String marginMode,
            int leverage,
            double quantity,
            Double limitPrice,
            String status,
            String feeType,
            double estimatedFee,
            double executionPrice,
            Instant orderTime
    ) {
        this(
                orderId,
                symbol,
                positionSide,
                orderType,
                orderPurpose,
                marginMode,
                leverage,
                quantity,
                limitPrice,
                status,
                feeType,
                estimatedFee,
                executionPrice,
                orderTime,
                null,
                null,
                null,
                null
        );
    }

    public FuturesOrder(
            String orderId,
            String symbol,
            String positionSide,
            String orderType,
            String marginMode,
            int leverage,
            double quantity,
            Double limitPrice,
            String status,
            String feeType,
            double estimatedFee,
            double executionPrice
    ) {
        this(
                orderId,
                symbol,
                positionSide,
                orderType,
                PURPOSE_OPEN_POSITION,
                marginMode,
                leverage,
                quantity,
                limitPrice,
                status,
                feeType,
                estimatedFee,
                executionPrice,
                Instant.now()
        );
    }

    public static FuturesOrder place(
            String orderId,
            String symbol,
            String positionSide,
            String orderType,
            String orderPurpose,
            String marginMode,
            int leverage,
            double quantity,
            Double limitPrice,
            boolean executable,
            String feeType,
            double estimatedFee,
            double executionPrice
    ) {
        return new FuturesOrder(
                orderId,
                symbol,
                positionSide,
                orderType,
                orderPurpose,
                marginMode,
                leverage,
                quantity,
                limitPrice,
                executable ? STATUS_FILLED : STATUS_PENDING,
                feeType,
                estimatedFee,
                executionPrice,
                Instant.now(),
                null,
                null,
                null,
                null
        );
    }

    public static FuturesOrder conditionalClose(
            String orderId,
            String symbol,
            String positionSide,
            String marginMode,
            int leverage,
            double quantity,
            double triggerPrice,
            String triggerType,
            String ocoGroupId
    ) {
        return new FuturesOrder(
                orderId,
                symbol,
                positionSide,
                TYPE_MARKET,
                PURPOSE_CLOSE_POSITION,
                marginMode,
                leverage,
                quantity,
                null,
                STATUS_PENDING,
                "TAKER",
                0,
                0,
                Instant.now(),
                triggerPrice,
                triggerType,
                TRIGGER_SOURCE_MARK_PRICE,
                ocoGroupId
        );
    }

    public static FuturesOrder place(
            String orderId,
            String symbol,
            String positionSide,
            String orderType,
            String marginMode,
            int leverage,
            double quantity,
            Double limitPrice,
            boolean executable,
            String feeType,
            double estimatedFee,
            double executionPrice
    ) {
        return place(
                orderId,
                symbol,
                positionSide,
                orderType,
                PURPOSE_OPEN_POSITION,
                marginMode,
                leverage,
                quantity,
                limitPrice,
                executable,
                feeType,
                estimatedFee,
                executionPrice
        );
    }

    public boolean isPending() {
        return STATUS_PENDING.equalsIgnoreCase(status);
    }

    public boolean isClosePositionOrder() {
        return PURPOSE_CLOSE_POSITION.equalsIgnoreCase(orderPurpose);
    }

    public boolean isOpenPositionOrder() {
        return PURPOSE_OPEN_POSITION.equalsIgnoreCase(orderPurpose);
    }

    public boolean isConditionalOrder() {
        return triggerPrice != null || triggerType != null || triggerSource != null || ocoGroupId != null;
    }

    public boolean isConditionalCloseOrder() {
        return isClosePositionOrder() && triggerPrice != null && triggerType != null;
    }

    public boolean isTakeProfitOrder() {
        return TRIGGER_TYPE_TAKE_PROFIT.equalsIgnoreCase(triggerType);
    }

    public boolean isStopLossOrder() {
        return TRIGGER_TYPE_STOP_LOSS.equalsIgnoreCase(triggerType);
    }

    public boolean usesMarkPriceTrigger() {
        return TRIGGER_SOURCE_MARK_PRICE.equalsIgnoreCase(triggerSource);
    }

    public boolean isBuySideLimitOrder() {
        if (limitPrice == null) {
            return false;
        }
        return (isOpenPositionOrder() && "LONG".equalsIgnoreCase(positionSide))
                || (isClosePositionOrder() && "SHORT".equalsIgnoreCase(positionSide));
    }

    public boolean isSellSideLimitOrder() {
        if (limitPrice == null) {
            return false;
        }
        return (isOpenPositionOrder() && "SHORT".equalsIgnoreCase(positionSide))
                || (isClosePositionOrder() && "LONG".equalsIgnoreCase(positionSide));
    }

    public FuturesOrder fill(double executionPrice, String feeType, double estimatedFee) {
        return new FuturesOrder(
                orderId,
                symbol,
                positionSide,
                orderType,
                orderPurpose,
                marginMode,
                leverage,
                quantity,
                limitPrice,
                STATUS_FILLED,
                feeType,
                estimatedFee,
                executionPrice,
                orderTime,
                triggerPrice,
                triggerType,
                triggerSource,
                ocoGroupId
        );
    }

    public FuturesOrder cancel() {
        return new FuturesOrder(
                orderId,
                symbol,
                positionSide,
                orderType,
                orderPurpose,
                marginMode,
                leverage,
                quantity,
                limitPrice,
                STATUS_CANCELLED,
                feeType,
                estimatedFee,
                executionPrice,
                orderTime,
                triggerPrice,
                triggerType,
                triggerSource,
                ocoGroupId
        );
    }

    public FuturesOrder withQuantity(double nextQuantity) {
        return new FuturesOrder(
                orderId,
                symbol,
                positionSide,
                orderType,
                orderPurpose,
                marginMode,
                leverage,
                nextQuantity,
                limitPrice,
                status,
                feeType,
                estimatedFee,
                executionPrice,
                orderTime,
                triggerPrice,
                triggerType,
                triggerSource,
                ocoGroupId
        );
    }
}
