package coin.coinzzickmock.feature.order.application.implement;

import coin.coinzzickmock.feature.market.application.realtime.MarketTradePriceMovedEvent;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderMarketTradeMovementQueue {
    private static final int DEFAULT_CAPACITY = 10_000;

    private final BlockingQueue<MarketTradePriceMovedEvent> events;
    private final OrderMarketTradeMovementTelemetry telemetry;

    @Autowired
    public OrderMarketTradeMovementQueue(OrderMarketTradeMovementTelemetry telemetry) {
        this(DEFAULT_CAPACITY, telemetry);
    }

    OrderMarketTradeMovementQueue(int capacity, OrderMarketTradeMovementTelemetry telemetry) {
        this.events = new LinkedBlockingQueue<>(capacity);
        this.telemetry = telemetry;
        this.telemetry.registerQueueSizeGauge(this::size);
    }

    public boolean enqueue(MarketTradePriceMovedEvent event) {
        boolean enqueued = events.offer(event);
        if (!enqueued) {
            telemetry.recordQueueDrop();
        }
        return enqueued;
    }

    Optional<MarketTradePriceMovedEvent> poll() {
        return Optional.ofNullable(events.poll());
    }

    MarketTradePriceMovedEvent take() throws InterruptedException {
        return events.take();
    }

    int size() {
        return events.size();
    }
}
