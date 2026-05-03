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
        double accumulatedOpenFee,
        double accumulatedCloseFee,
        double accumulatedFundingCost,
        Double takeProfitPrice,
        Double stopLossPrice,
        long version
) {
    private static final String MARGIN_MODE_CROSS = "CROSS";
    private static final int MIN_LEVERAGE = 1;
    private static final int MAX_LEVERAGE = 50;

    @Deprecated(since = "2026-05-03", forRemoval = false)
    public PositionSnapshot(
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
                openedAt,
                originalQuantity,
                accumulatedClosedQuantity,
                accumulatedExitNotional,
                accumulatedRealizedPnl,
                0,
                accumulatedCloseFee,
                0,
                null,
                null,
                0
        );
    }

    @Deprecated(since = "2026-05-03", forRemoval = false)
    public PositionSnapshot(
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
            double accumulatedOpenFee,
            double accumulatedCloseFee,
            double accumulatedFundingCost
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
                openedAt,
                originalQuantity,
                accumulatedClosedQuantity,
                accumulatedExitNotional,
                accumulatedRealizedPnl,
                accumulatedOpenFee,
                accumulatedCloseFee,
                accumulatedFundingCost,
                null,
                null,
                0
        );
    }

    @Deprecated(since = "2026-05-03", forRemoval = false)
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
                0,
                0,
                0,
                null,
                null,
                0
        );
    }

    public static PositionSnapshot restore(
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
            double accumulatedOpenFee,
            double accumulatedCloseFee,
            double accumulatedFundingCost,
            Double takeProfitPrice,
            Double stopLossPrice,
            long version
    ) {
        return new PositionSnapshot(
                symbol,
                positionSide,
                marginMode,
                leverage,
                quantity,
                entryPrice,
                markPrice,
                liquidationPrice,
                unrealizedPnl,
                openedAt,
                originalQuantity,
                accumulatedClosedQuantity,
                accumulatedExitNotional,
                accumulatedRealizedPnl,
                accumulatedOpenFee,
                accumulatedCloseFee,
                accumulatedFundingCost,
                takeProfitPrice,
                stopLossPrice,
                version
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
        return open(symbol, positionSide, marginMode, leverage, quantity, entryPrice, markPrice, 0);
    }

    public static PositionSnapshot open(
            String symbol,
            String positionSide,
            String marginMode,
            int leverage,
            double quantity,
            double entryPrice,
            double markPrice,
            double openFee
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
                openFee,
                0,
                0,
                null,
                null,
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
                accumulatedOpenFee,
                accumulatedCloseFee,
                accumulatedFundingCost,
                takeProfitPrice,
                stopLossPrice,
                version
        );
    }

    public PositionSnapshot increase(int leverage, double additionalQuantity, double executionPrice, double markPrice) {
        return increase(leverage, additionalQuantity, executionPrice, markPrice, 0);
    }

    public PositionSnapshot increase(
            int leverage,
            double additionalQuantity,
            double executionPrice,
            double markPrice,
            double openFee
    ) {
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
                accumulatedOpenFee + openFee,
                accumulatedCloseFee,
                accumulatedFundingCost,
                takeProfitPrice,
                stopLossPrice,
                version
        );
    }

    public PositionSnapshot withVersion(long nextVersion) {
        return new PositionSnapshot(
                symbol,
                positionSide,
                marginMode,
                leverage,
                quantity,
                entryPrice,
                markPrice,
                liquidationPrice,
                unrealizedPnl,
                openedAt,
                originalQuantity,
                accumulatedClosedQuantity,
                accumulatedExitNotional,
                accumulatedRealizedPnl,
                accumulatedOpenFee,
                accumulatedCloseFee,
                accumulatedFundingCost,
                takeProfitPrice,
                stopLossPrice,
                nextVersion
        );
    }

    public PositionSnapshot withTakeProfitStopLoss(Double nextTakeProfitPrice, Double nextStopLossPrice) {
        return new PositionSnapshot(
                symbol,
                positionSide,
                marginMode,
                leverage,
                quantity,
                entryPrice,
                markPrice,
                liquidationPrice,
                unrealizedPnl,
                openedAt,
                originalQuantity,
                accumulatedClosedQuantity,
                accumulatedExitNotional,
                accumulatedRealizedPnl,
                accumulatedOpenFee,
                accumulatedCloseFee,
                accumulatedFundingCost,
                nextTakeProfitPrice,
                nextStopLossPrice,
                version
        );
    }

    public PositionSnapshot withLeverage(int nextLeverage) {
        if (nextLeverage < MIN_LEVERAGE || nextLeverage > MAX_LEVERAGE) {
            throw new IllegalArgumentException("leverage must be between 1 and 50");
        }
        return new PositionSnapshot(
                symbol,
                positionSide,
                marginMode,
                nextLeverage,
                quantity,
                entryPrice,
                markPrice,
                liquidationPrice(positionSide, nextLeverage, entryPrice),
                pnl(markPrice, quantity),
                openedAt,
                originalQuantity,
                accumulatedClosedQuantity,
                accumulatedExitNotional,
                accumulatedRealizedPnl,
                accumulatedOpenFee,
                accumulatedCloseFee,
                accumulatedFundingCost,
                takeProfitPrice,
                stopLossPrice,
                version
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

    public double realizedPnl() {
        if (originalQuantity <= 0 || accumulatedClosedQuantity <= 0) {
            return 0;
        }
        double closedRatio = Math.min(1, accumulatedClosedQuantity / originalQuantity);
        return accumulatedRealizedPnl
                - accumulatedCloseFee
                - (accumulatedOpenFee * closedRatio)
                - (accumulatedFundingCost * closedRatio);
    }

    public String stableKey() {
        return String.join(":", symbol, positionSide, marginMode);
    }

    public boolean triggersTakeProfit(double targetMarkPrice) {
        if (takeProfitPrice == null) {
            return false;
        }
        return isLong()
                ? targetMarkPrice >= takeProfitPrice
                : targetMarkPrice <= takeProfitPrice;
    }

    public boolean triggersStopLoss(double targetMarkPrice) {
        if (stopLossPrice == null) {
            return false;
        }
        return isLong()
                ? targetMarkPrice <= stopLossPrice
                : targetMarkPrice >= stopLossPrice;
    }

    public String triggeredCloseReason(double targetMarkPrice) {
        if (triggersTakeProfit(targetMarkPrice)) {
            return PositionHistory.CLOSE_REASON_TAKE_PROFIT;
        }
        if (triggersStopLoss(targetMarkPrice)) {
            return PositionHistory.CLOSE_REASON_STOP_LOSS;
        }
        return null;
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
                accumulatedOpenFee,
                nextAccumulatedCloseFee,
                accumulatedFundingCost,
                takeProfitPrice,
                stopLossPrice,
                version
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
                accumulatedOpenFee,
                nextAccumulatedCloseFee,
                accumulatedFundingCost
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
