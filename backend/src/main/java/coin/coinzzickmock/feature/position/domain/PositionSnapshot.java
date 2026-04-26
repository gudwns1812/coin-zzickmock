package coin.coinzzickmock.feature.position.domain;

import java.time.Instant;

public record PositionSnapshot(
        String symbol,
        String positionSide,
        String marginMode,
        int leverage,
        double quantity,
        double entryPrice,
        double markPrice,
        Double liquidationPrice,
        double unrealizedPnl,
        Instant openedAt,
        double originalQuantity,
        double accumulatedClosedQuantity,
        double accumulatedExitNotional,
        double accumulatedRealizedPnl,
        double accumulatedCloseFee
) {
    private static final String MARGIN_MODE_CROSS = "CROSS";

    public PositionSnapshot(
            String symbol,
            String positionSide,
            String marginMode,
            int leverage,
            double quantity,
            double entryPrice,
            double markPrice,
            Double liquidationPrice,
            double unrealizedPnl
    ) {
        this(
                symbol,
                positionSide,
                marginMode,
                leverage,
                quantity,
                entryPrice,
                markPrice,
                liquidationPrice,
                unrealizedPnl,
                Instant.now(),
                quantity,
                0,
                0,
                0,
                0
        );
    }

    public static PositionSnapshot open(
            String symbol,
            String positionSide,
            String marginMode,
            int leverage,
            double quantity,
            double entryPrice,
            double markPrice
    ) {
        return new PositionSnapshot(
                symbol,
                positionSide,
                marginMode,
                leverage,
                quantity,
                entryPrice,
                markPrice,
                liquidationPrice(positionSide, leverage, entryPrice),
                0,
                Instant.now(),
                quantity,
                0,
                0,
                0,
                0
        );
    }

    public PositionSnapshot markToMarket(double nextMarkPrice) {
        return new PositionSnapshot(
                symbol,
                positionSide,
                marginMode,
                leverage,
                quantity,
                entryPrice,
                nextMarkPrice,
                liquidationPrice,
                pnl(nextMarkPrice, quantity),
                openedAt,
                originalQuantity,
                accumulatedClosedQuantity,
                accumulatedExitNotional,
                accumulatedRealizedPnl,
                accumulatedCloseFee
        );
    }

    public PositionSnapshot increase(int leverage, double additionalQuantity, double executionPrice, double markPrice) {
        double totalQuantity = quantity + additionalQuantity;
        double weightedEntryPrice = ((entryPrice * quantity) + (executionPrice * additionalQuantity)) / totalQuantity;
        return new PositionSnapshot(
                symbol,
                positionSide,
                marginMode,
                leverage,
                totalQuantity,
                weightedEntryPrice,
                markPrice,
                liquidationPrice(positionSide, leverage, weightedEntryPrice),
                0,
                openedAt,
                originalQuantity + additionalQuantity,
                accumulatedClosedQuantity,
                accumulatedExitNotional,
                accumulatedRealizedPnl,
                accumulatedCloseFee
        );
    }

    public boolean isCrossMargin() {
        return MARGIN_MODE_CROSS.equalsIgnoreCase(marginMode);
    }

    public double notional(double targetMarkPrice) {
        return targetMarkPrice * quantity;
    }

    public double initialMargin() {
        return (entryPrice * quantity) / leverage;
    }

    public double originalInitialMargin() {
        return (entryPrice * originalQuantity) / leverage;
    }

    public double roi() {
        double margin = initialMargin();
        if (margin == 0) {
            return 0;
        }
        return unrealizedPnl / margin;
    }

    public double unrealizedPnl(double targetMarkPrice) {
        return pnl(targetMarkPrice, quantity);
    }

    public String stableKey() {
        return String.join(":", symbol, positionSide, marginMode);
    }

    public PositionCloseOutcome close(double closeQuantity, double markPrice, double executionPrice, double takerFeeRate) {
        double safeCloseQuantity = Math.min(closeQuantity, quantity);
        double realizedPnl = pnl(executionPrice, safeCloseQuantity);
        double closeFee = executionPrice * safeCloseQuantity * takerFeeRate;
        double releasedMargin = (entryPrice * safeCloseQuantity) / leverage;
        double remainingQuantity = quantity - safeCloseQuantity;
        double nextAccumulatedClosedQuantity = accumulatedClosedQuantity + safeCloseQuantity;
        double nextAccumulatedExitNotional = accumulatedExitNotional + (executionPrice * safeCloseQuantity);
        double nextAccumulatedRealizedPnl = accumulatedRealizedPnl + realizedPnl;
        double nextAccumulatedCloseFee = accumulatedCloseFee + closeFee;

        PositionSnapshot remainingPosition = remainingQuantity <= 0
                ? null
                : new PositionSnapshot(
                symbol,
                positionSide,
                marginMode,
                leverage,
                remainingQuantity,
                entryPrice,
                markPrice,
                liquidationPrice,
                pnl(markPrice, remainingQuantity),
                openedAt,
                originalQuantity,
                nextAccumulatedClosedQuantity,
                nextAccumulatedExitNotional,
                nextAccumulatedRealizedPnl,
                nextAccumulatedCloseFee
        );

        return new PositionCloseOutcome(
                safeCloseQuantity,
                realizedPnl,
                closeFee,
                releasedMargin,
                remainingPosition,
                openedAt,
                originalQuantity,
                entryPrice,
                nextAccumulatedClosedQuantity,
                nextAccumulatedExitNotional,
                nextAccumulatedRealizedPnl,
                nextAccumulatedCloseFee
        );
    }

    private double pnl(double targetMarkPrice, double targetQuantity) {
        if (isLong()) {
            return (targetMarkPrice - entryPrice) * targetQuantity;
        }
        return (entryPrice - targetMarkPrice) * targetQuantity;
    }

    private boolean isLong() {
        return "LONG".equalsIgnoreCase(positionSide);
    }

    private static double liquidationPrice(String positionSide, int leverage, double entryPrice) {
        double liquidationGap = entryPrice / leverage;
        if ("LONG".equalsIgnoreCase(positionSide)) {
            return entryPrice - liquidationGap;
        }
        return entryPrice + liquidationGap;
    }
}
