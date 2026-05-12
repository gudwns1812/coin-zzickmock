package coin.coinzzickmock.feature.order.application.realtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.position.application.close.StaleProtectiveCloseOrderCanceller;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import org.junit.jupiter.api.Test;

class PositionLiquidationProcessorTest {
    @Test
    void liquidatesBreachedIsolatedPositionAndPublishesEventOnce() {
        OrderRealtimeProcessorFixtures.Scenario scenario = OrderRealtimeProcessorFixtures.scenario();
        scenario.positions.save(1L, scenario.openPosition("LONG", "ISOLATED", 2, 100));

        scenario.liquidationProcessor()
                .liquidateBreachedPositions(scenario.market(90, 90));

        assertTrue(scenario.positions.findOpenPositions(1L).isEmpty());
        assertEquals(1, scenario.events.tradingEvents().size());
        assertEquals("POSITION_LIQUIDATED", scenario.events.tradingEvents().get(0).type());
        assertEquals(-20.09, scenario.events.tradingEvents().get(0).realizedPnl(), 0.0001);
    }

    @Test
    void leavesIsolatedPositionOpenWhenMaintenanceIsNotBreached() {
        OrderRealtimeProcessorFixtures.Scenario scenario = OrderRealtimeProcessorFixtures.scenario();
        PositionSnapshot position = scenario.openPosition("LONG", "ISOLATED", 2, 100).withVersion(3);
        scenario.positions.save(1L, position);

        scenario.liquidationProcessor()
                .liquidateBreachedPositions(scenario.market(105, 105));

        PositionSnapshot current = scenario.positions.findOpenPosition(1L, "BTCUSDT", "LONG", "ISOLATED")
                .orElseThrow();
        assertEquals(3, current.version());
        assertEquals(100, current.markPrice(), 0.0001);
        assertTrue(scenario.events.tradingEvents().isEmpty());
    }

    @Test
    void liquidatesBreachedCrossPositionAndPublishesEventOnce() {
        OrderRealtimeProcessorFixtures.Scenario scenario = OrderRealtimeProcessorFixtures.scenario();
        scenario.positions.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "CROSS",
                50,
                40_000,
                100,
                100
        ));

        scenario.liquidationProcessor()
                .liquidateBreachedPositions(scenario.market(97, 97));

        assertTrue(scenario.positions.findOpenPositions(1L).isEmpty());
        assertEquals(1, scenario.events.tradingEvents().size());
        assertEquals("POSITION_LIQUIDATED", scenario.events.tradingEvents().get(0).type());
    }

    @Test
    void leavesCrossPositionOpenWhenWalletEquityIsNotBreached() {
        OrderRealtimeProcessorFixtures.Scenario scenario = OrderRealtimeProcessorFixtures.scenario();
        scenario.positions.save(1L, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "CROSS",
                10,
                1,
                100,
                100
        ));

        scenario.liquidationProcessor()
                .liquidateBreachedPositions(scenario.market(99, 99));

        assertTrue(scenario.positions.findOpenPosition(1L, "BTCUSDT", "LONG", "CROSS").isPresent());
        assertTrue(scenario.events.tradingEvents().isEmpty());
    }

    @Test
    void staleProtectiveCancelFailureDoesNotRollBackLiquidationEvent() {
        OrderRealtimeProcessorFixtures.Scenario scenario = OrderRealtimeProcessorFixtures.scenario();
        scenario.positions.save(1L, scenario.openPosition("LONG", "ISOLATED", 2, 100));
        scenario.orders.save(1L, FuturesOrder.conditionalClose(
                "tp-before-liquidation",
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                2,
                110,
                FuturesOrder.TRIGGER_TYPE_TAKE_PROFIT,
                null
        ));
        StaleProtectiveCloseOrderCanceller failingCanceller = new StaleProtectiveCloseOrderCanceller(scenario.orders) {
            @Override
            public void cancel(Long memberId, PositionSnapshot position) {
                throw new IllegalStateException("cleanup unavailable");
            }
        };

        scenario.liquidationProcessor(failingCanceller)
                .liquidateBreachedPositions(scenario.market(90, 90));

        assertTrue(scenario.positions.findOpenPositions(1L).isEmpty());
        assertEquals(1, scenario.events.tradingEvents().size());
        assertEquals("POSITION_LIQUIDATED", scenario.events.tradingEvents().get(0).type());
        assertEquals(FuturesOrder.STATUS_PENDING, scenario.orders.findByMemberIdAndOrderId(1L, "tp-before-liquidation")
                .orElseThrow()
                .status());
    }
}
