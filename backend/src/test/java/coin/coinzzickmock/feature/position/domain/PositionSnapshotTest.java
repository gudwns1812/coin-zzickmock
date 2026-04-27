package coin.coinzzickmock.feature.position.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PositionSnapshotTest {
    @Test
    void longCloseSeparatesEventNetPnlFromWholePositionNetPnl() {
        PositionSnapshot position = PositionSnapshot.open(
                        "BTCUSDT",
                        "LONG",
                        "ISOLATED",
                        10,
                        1,
                        100,
                        100,
                        5
                )
                .increase(10, 1, 110, 110, 7);

        PositionCloseOutcome partial = position.close(1, 120, 120, 0.0005);

        assertEquals(15, partial.grossRealizedPnl(), 0.0001);
        assertEquals(0.06, partial.closeFee(), 0.0001);
        assertEquals(14.94, partial.eventNetRealizedPnl(), 0.0001);
        assertEquals(15, partial.accumulatedGrossRealizedPnl(), 0.0001);
        assertEquals(12, partial.accumulatedOpenFee(), 0.0001);
        assertEquals(0.06, partial.accumulatedCloseFee(), 0.0001);
        assertEquals(0, partial.accumulatedFundingCost(), 0.0001);
        assertEquals(2.94, partial.positionNetRealizedPnl(), 0.0001);
        assertFalse(partial.fullyClosed());

        PositionCloseOutcome full = partial.remainingPosition().close(1, 120, 120, 0.0005);

        assertTrue(full.fullyClosed());
        assertEquals(30, full.accumulatedGrossRealizedPnl(), 0.0001);
        assertEquals(12, full.accumulatedOpenFee(), 0.0001);
        assertEquals(0.12, full.accumulatedCloseFee(), 0.0001);
        assertEquals(17.88, full.positionNetRealizedPnl(), 0.0001);
    }

    @Test
    void shortCloseUsesInversePnlDirectionAndNetFees() {
        PositionSnapshot position = PositionSnapshot.open(
                "BTCUSDT",
                "SHORT",
                "ISOLATED",
                10,
                2,
                100,
                100,
                10
        );

        PositionCloseOutcome outcome = position.close(2, 90, 90, 0.0005);

        assertEquals(20, outcome.grossRealizedPnl(), 0.0001);
        assertEquals(0.09, outcome.closeFee(), 0.0001);
        assertEquals(19.91, outcome.eventNetRealizedPnl(), 0.0001);
        assertEquals(9.91, outcome.positionNetRealizedPnl(), 0.0001);
        assertEquals(10.09, outcome.totalFee(), 0.0001);
    }

    @Test
    void takeProfitAndStopLossTriggerByPositionDirection() {
        PositionSnapshot longPosition = PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                100,
                100
        ).withTakeProfitStopLoss(110.0, 95.0);
        PositionSnapshot shortPosition = PositionSnapshot.open(
                "BTCUSDT",
                "SHORT",
                "ISOLATED",
                10,
                1,
                100,
                100
        ).withTakeProfitStopLoss(90.0, 105.0);

        assertTrue(longPosition.triggersTakeProfit(110));
        assertTrue(longPosition.triggersStopLoss(95));
        assertFalse(longPosition.triggersTakeProfit(109.99));
        assertFalse(longPosition.triggersStopLoss(95.01));

        assertTrue(shortPosition.triggersTakeProfit(90));
        assertTrue(shortPosition.triggersStopLoss(105));
        assertFalse(shortPosition.triggersTakeProfit(90.01));
        assertFalse(shortPosition.triggersStopLoss(104.99));
    }

    @Test
    void nullTakeProfitAndStopLossNeverTrigger() {
        PositionSnapshot position = PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                100,
                100
        );

        assertFalse(position.triggersTakeProfit(1000));
        assertFalse(position.triggersStopLoss(1));
    }
}
