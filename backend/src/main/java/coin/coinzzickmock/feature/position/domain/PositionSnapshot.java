package coin.coinzzickmock.feature.position.domain;

public record PositionSnapshot(
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
                0
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
                0
        );
    }

    public PositionCloseOutcome close(double closeQuantity, double markPrice, double executionPrice, double takerFeeRate) {
        double safeCloseQuantity = Math.min(closeQuantity, quantity);
        double realizedPnl = pnl(markPrice, safeCloseQuantity);
        double closeFee = executionPrice * safeCloseQuantity * takerFeeRate;
        double releasedMargin = (entryPrice * safeCloseQuantity) / leverage;
        double remainingQuantity = quantity - safeCloseQuantity;

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
                pnl(markPrice, remainingQuantity)
        );

        return new PositionCloseOutcome(
                safeCloseQuantity,
                realizedPnl,
                closeFee,
                releasedMargin,
                remainingPosition
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
