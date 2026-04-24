package coin.coinzzickmock.feature.position.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class LiquidationPolicyTest {
    private final LiquidationPolicy liquidationPolicy = new LiquidationPolicy();

    @Test
    void assessesIsolatedMaintenanceBreachFromExplicitMath() {
        PositionSnapshot position = PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                2,
                100,
                100
        );

        IsolatedLiquidationAssessment assessment = liquidationPolicy.assessIsolated(position, 90);

        assertEquals(180, assessment.notional(), 0.0001);
        assertEquals(20, assessment.initialMargin(), 0.0001);
        assertEquals(-20, assessment.unrealizedPnl(), 0.0001);
        assertEquals(0, assessment.equity(), 0.0001);
        assertEquals(0.9, assessment.maintenanceRequirement(), 0.0001);
        assertTrue(assessment.breached());
    }

    @Test
    void ranksCrossPositionsByRiskRatioThenLossForDeterministicLiquidation() {
        PositionSnapshot highestRisk = PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "CROSS",
                10,
                1,
                100,
                100
        ).markToMarket(90);
        PositionSnapshot saferCross = PositionSnapshot.open(
                "ETHUSDT",
                "SHORT",
                "CROSS",
                5,
                1,
                200,
                200
        ).markToMarket(190);
        PositionSnapshot isolated = PositionSnapshot.open(
                "XRPUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                10,
                10
        ).markToMarket(1);

        CrossLiquidationAssessment assessment = liquidationPolicy.assessCross(
                0,
                List.of(saferCross, highestRisk, isolated)
        );

        assertTrue(assessment.breached());
        assertEquals(2, assessment.rankedRisks().size());
        assertEquals("BTCUSDT", assessment.rankedRisks().get(0).position().symbol());
        assertEquals("BTCUSDT", assessment.liquidationCandidate().orElseThrow().position().symbol());
        assertFalse(assessment.rankedRisks().stream()
                .map(risk -> risk.position().marginMode())
                .anyMatch("ISOLATED"::equalsIgnoreCase));
    }
}
