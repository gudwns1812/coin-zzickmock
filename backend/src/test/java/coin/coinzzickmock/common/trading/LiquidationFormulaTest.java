package coin.coinzzickmock.common.trading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class LiquidationFormulaTest {
    @Test
    void isolatedLongLiquidationPriceUsesMaintenanceMarginRate() {
        double expected = 100 * (1d - (1d / 50)) / (1d - 0.005d);

        assertEquals(expected, LiquidationFormula.isolatedLiquidationPrice("LONG", 50, 100), 0.0001);
    }

    @Test
    void isolatedShortLiquidationPriceUsesMaintenanceMarginRate() {
        double expected = 100 * (1d + (1d / 50)) / (1d + 0.005d);

        assertEquals(expected, LiquidationFormula.isolatedLiquidationPrice("SHORT", 50, 100), 0.0001);
    }

    @Test
    void liquidationPriceIsUnavailableForCrossMargin() {
        assertNull(LiquidationFormula.liquidationPrice("LONG", "CROSS", 50, 100));
    }

    @Test
    void solvesLinearBoundaryForLongAndShortSlopes() {
        assertEquals(50.2512562814, LiquidationFormula.solveLinearBoundary(-50, 1, 0, 0.005), 0.0001);
        assertEquals(149.2537313433, LiquidationFormula.solveLinearBoundary(150, -1, 0, 0.005), 0.0001);
    }
}
