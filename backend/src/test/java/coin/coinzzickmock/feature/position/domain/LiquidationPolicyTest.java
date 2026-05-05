package coin.coinzzickmock.feature.position.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void crossAssessmentUsesWalletBackedEquityInsteadOfAvailableMargin() {
        PositionSnapshot cross = PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "CROSS",
                50,
                1_000,
                100,
                100
        );
        PositionSnapshot isolated = PositionSnapshot.open(
                "ETHUSDT",
                "LONG",
                "ISOLATED",
                10,
                10,
                100,
                100
        );

        CrossLiquidationAssessment assessment = liquidationPolicy.assessCross(
                99_950,
                List.of(cross, isolated)
        );

        assertEquals(99_950, assessment.walletBalance(), 0.0001);
        assertEquals(100, assessment.isolatedInitialMargin(), 0.0001);
        assertEquals(99_850, assessment.crossEquity(), 0.0001);
        assertEquals(500, assessment.maintenanceRequirement(), 0.0001);
        assertFalse(assessment.breached());
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

    @Test
    void estimatesSingleCrossLongLiquidationPriceAsExact() {
        PositionSnapshot position = PositionSnapshot.open("BTCUSDT", "LONG", "CROSS", 10, 1, 100, 100);

        CrossLiquidationEstimate estimate = liquidationPolicy.estimateCrossLiquidationPrice(
                50,
                List.of(position),
                "BTCUSDT"
        );

        assertEquals(CrossLiquidationEstimate.TYPE_EXACT, estimate.liquidationPriceType());
        assertEquals(50.2512562814, estimate.liquidationPrice(), 0.0001);
    }

    @Test
    void estimatesSameSymbolCrossLongShortAsExact() {
        PositionSnapshot longPosition = PositionSnapshot.open("BTCUSDT", "LONG", "CROSS", 10, 2, 100, 100);
        PositionSnapshot shortPosition = PositionSnapshot.open("BTCUSDT", "SHORT", "CROSS", 10, 1, 120, 120);

        CrossLiquidationEstimate estimate = liquidationPolicy.estimateCrossLiquidationPrice(
                50,
                List.of(longPosition, shortPosition),
                "BTCUSDT"
        );

        assertEquals(CrossLiquidationEstimate.TYPE_EXACT, estimate.liquidationPriceType());
        assertEquals(30.4568527919, estimate.liquidationPrice(), 0.0001);
    }

    @Test
    void estimatesMultiSymbolCrossPriceWithFixedOtherMarks() {
        PositionSnapshot target = PositionSnapshot.open("BTCUSDT", "LONG", "CROSS", 10, 1, 100, 100);
        PositionSnapshot other = PositionSnapshot.open("ETHUSDT", "LONG", "CROSS", 10, 1, 50, 50);

        CrossLiquidationEstimate estimate = liquidationPolicy.estimateCrossLiquidationPrice(
                80,
                List.of(target, other),
                "BTCUSDT"
        );

        assertEquals(CrossLiquidationEstimate.TYPE_ESTIMATED, estimate.liquidationPriceType());
        assertEquals(20.351758794, estimate.liquidationPrice(), 0.0001);
    }

    @Test
    void returnsUnavailableWhenTargetSymbolHasNoCrossPosition() {
        CrossLiquidationEstimate estimate = liquidationPolicy.estimateCrossLiquidationPrice(
                80,
                List.of(PositionSnapshot.open("ETHUSDT", "LONG", "CROSS", 10, 1, 50, 50)),
                "BTCUSDT"
        );

        assertEquals(CrossLiquidationEstimate.TYPE_UNAVAILABLE, estimate.liquidationPriceType());
        assertNull(estimate.liquidationPrice());
    }
}
