package coin.coinzzickmock.feature.order.application.realtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import coin.coinzzickmock.feature.market.application.realtime.MarketPriceMovementDirection;
import coin.coinzzickmock.feature.market.application.realtime.MarketTradePriceMovedEvent;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import java.time.Instant;
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

        MarketTradeMovementQueue queue = new MarketTradeMovementQueue();
        MarketTradeMovementWorker worker = new MarketTradeMovementWorker(queue, scenario.pendingFillProcessor());

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
}
