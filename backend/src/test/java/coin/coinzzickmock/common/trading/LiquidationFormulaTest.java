package coin.coinzzickmock.common.trading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class LiquidationFormulaTest {
    private static final double FORMULA_UNIT = 1d;
    private static final double ENTRY_PRICE = 100d;
    private static final int HIGH_LEVERAGE = 50;
    private static final double ASSERTION_TOLERANCE = 0.0001;
    private static final double LONG_EQUITY_CONSTANT = -50d;
    private static final double LONG_PNL_SLOPE = 1d;
    private static final double SHORT_EQUITY_CONSTANT = 150d;
    private static final double SHORT_PNL_SLOPE = -1d;
    private static final double EMPTY_MAINTENANCE_CONSTANT = 0d;
    private static final double LONG_LINEAR_BOUNDARY = 50.2512562814;
    private static final double SHORT_LINEAR_BOUNDARY = 149.2537313433;

    @Test
    void isolatedLongLiquidationPriceUsesMaintenanceMarginRate() {
        double expected = ENTRY_PRICE * (FORMULA_UNIT - (FORMULA_UNIT / HIGH_LEVERAGE))
                / (FORMULA_UNIT - LiquidationFormula.MAINTENANCE_MARGIN_RATE);

        assertEquals(
                expected,
                LiquidationFormula.isolatedLiquidationPrice("LONG", HIGH_LEVERAGE, ENTRY_PRICE),
                ASSERTION_TOLERANCE
        );
    }

    @Test
    void isolatedShortLiquidationPriceUsesMaintenanceMarginRate() {
        double expected = ENTRY_PRICE * (FORMULA_UNIT + (FORMULA_UNIT / HIGH_LEVERAGE))
                / (FORMULA_UNIT + LiquidationFormula.MAINTENANCE_MARGIN_RATE);

        assertEquals(
                expected,
                LiquidationFormula.isolatedLiquidationPrice("SHORT", HIGH_LEVERAGE, ENTRY_PRICE),
                ASSERTION_TOLERANCE
        );
    }

    @Test
    void liquidationPriceIsUnavailableForCrossMargin() {
        assertNull(LiquidationFormula.liquidationPrice("LONG", "CROSS", HIGH_LEVERAGE, ENTRY_PRICE));
    }

    @Test
    void solvesLinearBoundaryForLongAndShortSlopes() {
        assertEquals(
                LONG_LINEAR_BOUNDARY,
                LiquidationFormula.solveLinearBoundary(
                        LONG_EQUITY_CONSTANT,
                        LONG_PNL_SLOPE,
                        EMPTY_MAINTENANCE_CONSTANT,
                        LiquidationFormula.MAINTENANCE_MARGIN_RATE
                ),
                ASSERTION_TOLERANCE
        );
        assertEquals(
                SHORT_LINEAR_BOUNDARY,
                LiquidationFormula.solveLinearBoundary(
                        SHORT_EQUITY_CONSTANT,
                        SHORT_PNL_SLOPE,
                        EMPTY_MAINTENANCE_CONSTANT,
                        LiquidationFormula.MAINTENANCE_MARGIN_RATE
                ),
                ASSERTION_TOLERANCE
        );
    }
}
