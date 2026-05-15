package coin.coinzzickmock.feature.order.application.realtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import coin.coinzzickmock.feature.market.application.realtime.MarketPriceMovementDirection;
import coin.coinzzickmock.feature.market.application.realtime.MarketTradePriceMovedEvent;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.testsupport.TestTelemetryProvider;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MarketTradeMovementWorkerTest {
    @Test
    void drainsQueuedTradeMovementThroughPendingFillProcessor() {
        OrderRealtimeProcessorFixtures.Scenario scenario = OrderRealtimeProcessorFixtures.scenario();
        FuturesOrder order = scenario.pendingOpenOrderAt(
                "open-long",
                "LONG",
                99,
                Instant.parse("2026-04-27T00:00:00Z")
        );
        scenario.orders.save(1L, order);
        scenario.pendingLimitOrderBook.add(1L, order);
        scenario.market(98, 98);

        MarketTradeMovementTelemetry telemetry = new MarketTradeMovementTelemetry(new CapturingTelemetryProvider());
        MarketTradeMovementQueue queue = new MarketTradeMovementQueue(10, telemetry);
        MarketTradeMovementWorker worker = new MarketTradeMovementWorker(
                queue,
                scenario.pendingFillProcessor(),
                telemetry
        );

        queue.publish(new MarketTradePriceMovedEvent(
                "BTCUSDT",
                101,
                98,
                MarketPriceMovementDirection.DOWN,
                Instant.parse("2026-04-27T00:00:01Z"),
                Instant.parse("2026-04-27T00:00:01Z")
        ));

        assertEquals(1, worker.processAvailable());
        assertEquals(FuturesOrder.STATUS_FILLED, scenario.orders.findByMemberIdAndOrderId(1L, "open-long")
                .orElseThrow()
                .status());
    }

    @Test
    void recordsWorkerFailureWhenPendingFillProcessorThrows() {
        CapturingTelemetryProvider telemetryProvider = new CapturingTelemetryProvider();
        MarketTradeMovementTelemetry telemetry = new MarketTradeMovementTelemetry(telemetryProvider);
        MarketTradeMovementQueue queue = new MarketTradeMovementQueue(10, telemetry);
        PendingOrderFillProcessor processor = mock(PendingOrderFillProcessor.class);
        MarketTradeMovementWorker worker = new MarketTradeMovementWorker(queue, processor, telemetry);
        queue.publish(movement());
        doThrow(new IllegalStateException("boom")).when(processor)
                .fillExecutablePendingOrders(any(MarketTradePriceMovedEvent.class));

        assertThrows(IllegalStateException.class, worker::processAvailable);

        assertEquals(List.of("market.trade.movement.worker.failure.total"), telemetryProvider.eventNames);
        assertEquals(Map.of("reason", "runtime_exception"), telemetryProvider.eventTags.get(0));
    }

    private static MarketTradePriceMovedEvent movement() {
        return new MarketTradePriceMovedEvent(
                "BTCUSDT",
                101,
                98,
                MarketPriceMovementDirection.DOWN,
                Instant.parse("2026-04-27T00:00:01Z"),
                Instant.parse("2026-04-27T00:00:01Z")
        );
    }

    private static final class CapturingTelemetryProvider extends TestTelemetryProvider {
        private final List<String> eventNames = new ArrayList<>();
        private final List<Map<String, String>> eventTags = new ArrayList<>();

        @Override
        public void recordEvent(String eventName, Map<String, String> tags) {
            eventNames.add(eventName);
            eventTags.add(tags);
        }
    }
}
