package coin.coinzzickmock.feature.order.application.implement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import coin.coinzzickmock.feature.order.application.dto.TradingExecutionEvent;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OrderPositionTakeProfitStopLossProcessorTest {
    @Test
    void triggersOnlyOrdersMatchingPositionSideAndTriggerType() {
        OrderExecutionProcessorFixtures.Scenario scenario = OrderExecutionProcessorFixtures.scenario();
        scenario.positions.save(1L, scenario.openPosition("LONG", "ISOLATED", 1, 100));
        scenario.orders.save(1L, FuturesOrder.conditionalClose(
                "long-tp",
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                105,
                FuturesOrder.TRIGGER_TYPE_TAKE_PROFIT,
                null
        ));
        scenario.orders.save(1L, FuturesOrder.conditionalClose(
                "long-sl-not-triggered",
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                95,
                FuturesOrder.TRIGGER_TYPE_STOP_LOSS,
                null
        ));

        scenario.takeProfitStopLossProcessor()
                .closeTriggeredPositions(scenario.market(106, 106));

        assertEquals(FuturesOrder.STATUS_FILLED, scenario.orders.findByMemberIdAndOrderId(1L, "long-tp")
                .orElseThrow()
                .status());
        assertEquals(FuturesOrder.STATUS_CANCELLED, scenario.orders.findByMemberIdAndOrderId(1L, "long-sl-not-triggered")
                .orElseThrow()
                .status());
        assertEquals(1, scenario.events.tradingEvents().size());
        assertEquals("POSITION_TAKE_PROFIT", scenario.events.tradingEvents().get(0).type());
    }

    @Test
    void synchronizesTriggeredOrderToCurrentPositionQuantityBeforeFill() {
        OrderExecutionProcessorFixtures.Scenario scenario = OrderExecutionProcessorFixtures.scenario();
        scenario.positions.save(1L, scenario.openPosition("LONG", "ISOLATED", 1, 100));
        scenario.orders.save(1L, FuturesOrder.conditionalClose(
                "tp-order",
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                0.4,
                105,
                FuturesOrder.TRIGGER_TYPE_TAKE_PROFIT,
                null
        ));

        scenario.takeProfitStopLossProcessor()
                .closeTriggeredPositions(scenario.market(106, 106));

        FuturesOrder takeProfit = scenario.orders.findByMemberIdAndOrderId(1L, "tp-order").orElseThrow();
        assertEquals(FuturesOrder.STATUS_FILLED, takeProfit.status());
        assertEquals(1, takeProfit.quantity(), 0.0001);
        assertEquals(0.053, takeProfit.estimatedFee(), 0.0001);
        assertTrue(scenario.positions.findOpenPosition(1L, "BTCUSDT", "LONG", "ISOLATED").isEmpty());
    }

    @Test
    void skipsTriggeredOrderThatDisappearedBeforeReload() {
        OrderExecutionProcessorFixtures.Scenario scenario = OrderExecutionProcessorFixtures.scenario();
        MissingReloadOrderRepository orders = new MissingReloadOrderRepository();
        scenario.orders = orders;
        scenario.positions.save(1L, scenario.openPosition("LONG", "ISOLATED", 1, 100));
        orders.save(1L, FuturesOrder.conditionalClose(
                "stale-tp",
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                105,
                FuturesOrder.TRIGGER_TYPE_TAKE_PROFIT,
                null
        ));

        scenario.takeProfitStopLossProcessor()
                .closeTriggeredPositions(scenario.market(106, 106));

        assertEquals(FuturesOrder.STATUS_PENDING, orders.findPendingBySymbol("BTCUSDT").get(0).order().status());
        assertTrue(scenario.events.tradingEvents().isEmpty());
    }

    @Test
    void cancelsStaleProtectiveOrdersWhenTriggeredOrderHasNoOpenPosition() {
        OrderExecutionProcessorFixtures.Scenario scenario = OrderExecutionProcessorFixtures.scenario();
        scenario.orders.save(1L, FuturesOrder.conditionalClose(
                "tp-order",
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                105,
                FuturesOrder.TRIGGER_TYPE_TAKE_PROFIT,
                null
        ));

        scenario.takeProfitStopLossProcessor()
                .closeTriggeredPositions(scenario.market(106, 106));

        assertEquals(FuturesOrder.STATUS_CANCELLED, scenario.orders.findByMemberIdAndOrderId(1L, "tp-order")
                .orElseThrow()
                .status());
        assertTrue(scenario.events.tradingEvents().isEmpty());
    }

    @Test
    void triggeredOcoOrderCancelsSiblingBeforeProtectiveCleanup() {
        OrderExecutionProcessorFixtures.Scenario scenario = OrderExecutionProcessorFixtures.scenario();
        scenario.positions.save(1L, scenario.openPosition("LONG", "ISOLATED", 1, 100));
        scenario.orders.save(1L, FuturesOrder.conditionalClose(
                "tp-order",
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                105,
                FuturesOrder.TRIGGER_TYPE_TAKE_PROFIT,
                "oco-1"
        ));
        scenario.orders.save(1L, FuturesOrder.conditionalClose(
                "sl-order",
                "BTCUSDT",
                "LONG",
                "ISOLATED",
                10,
                1,
                95,
                FuturesOrder.TRIGGER_TYPE_STOP_LOSS,
                "oco-1"
        ));

        scenario.takeProfitStopLossProcessor()
                .closeTriggeredPositions(scenario.market(106, 106));

        assertEquals(FuturesOrder.STATUS_FILLED, scenario.orders.findByMemberIdAndOrderId(1L, "tp-order")
                .orElseThrow()
                .status());
        assertEquals(FuturesOrder.STATUS_CANCELLED, scenario.orders.findByMemberIdAndOrderId(1L, "sl-order")
                .orElseThrow()
                .status());
        assertEquals(List.of("POSITION_TAKE_PROFIT"), scenario.events.tradingEvents().stream()
                .map(TradingExecutionEvent::type)
                .toList());
    }

    private static final class MissingReloadOrderRepository extends OrderExecutionProcessorFixtures.InMemoryOrderRepository {
        @Override
        public Optional<FuturesOrder> findByMemberIdAndOrderId(Long memberId, String orderId) {
            return Optional.empty();
        }
    }
}
