package coin.coinzzickmock.common.trading;

public final class LiquidationFormula {
    public static final double MAINTENANCE_MARGIN_RATE = 0.005d;

    private static final String POSITION_SIDE_LONG = "LONG";
    private static final String POSITION_SIDE_SHORT = "SHORT";
    private static final String MARGIN_MODE_CROSS = "CROSS";

    private LiquidationFormula() {
    }

    public static Double liquidationPrice(String positionSide, String marginMode, int leverage, double entryPrice) {
        if (MARGIN_MODE_CROSS.equalsIgnoreCase(marginMode)) {
            return null;
        }
        return isolatedLiquidationPrice(positionSide, leverage, entryPrice);
    }

    public static double isolatedLiquidationPrice(String positionSide, int leverage, double entryPrice) {
        validatePositiveFinite("entryPrice", entryPrice);
        if (leverage <= 0) {
            throw new IllegalArgumentException("leverage must be positive");
        }

        if (POSITION_SIDE_LONG.equalsIgnoreCase(positionSide)) {
            return entryPrice * (1d - (1d / leverage)) / (1d - MAINTENANCE_MARGIN_RATE);
        }
        if (POSITION_SIDE_SHORT.equalsIgnoreCase(positionSide)) {
            return entryPrice * (1d + (1d / leverage)) / (1d + MAINTENANCE_MARGIN_RATE);
        }
        throw new IllegalArgumentException("unsupported position side: " + positionSide);
    }

    public static double maintenanceRequirement(double markPrice, double quantity) {
        validatePositiveFinite("markPrice", markPrice);
        validatePositiveFinite("quantity", quantity);
        return markPrice * quantity * MAINTENANCE_MARGIN_RATE;
    }

    public static Double solveLinearBoundary(
            double equityConstant,
            double pnlSlope,
            double maintenanceConstant,
            double maintenanceSlope
    ) {
        double denominator = pnlSlope - maintenanceSlope;
        if (!Double.isFinite(equityConstant)
                || !Double.isFinite(pnlSlope)
                || !Double.isFinite(maintenanceConstant)
                || !Double.isFinite(maintenanceSlope)
                || denominator == 0) {
            return null;
        }

        double boundary = (maintenanceConstant - equityConstant) / denominator;
        if (!Double.isFinite(boundary) || boundary <= 0) {
            return null;
        }
        return boundary;
    }

    private static void validatePositiveFinite(String name, double value) {
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException(name + " must be positive and finite");
        }
    }
}
