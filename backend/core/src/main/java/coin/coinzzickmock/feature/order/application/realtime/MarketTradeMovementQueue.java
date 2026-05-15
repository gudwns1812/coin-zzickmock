package coin.coinzzickmock.feature.order.application.realtime;

import coin.coinzzickmock.feature.market.application.realtime.MarketTradePriceMovedEvent;
import coin.coinzzickmock.feature.market.application.realtime.MarketTradePriceMovementPublisher;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MarketTradeMovementQueue implements MarketTradePriceMovementPublisher {
    private static final int DEFAULT_CAPACITY = 10_000;

    private final BlockingQueue<MarketTradePriceMovedEvent> events;
    private final MarketTradeMovementTelemetry telemetry;

    @Autowired
    public MarketTradeMovementQueue(MarketTradeMovementTelemetry telemetry) {
        this(DEFAULT_CAPACITY, telemetry);
    }

    MarketTradeMovementQueue(int capacity, MarketTradeMovementTelemetry telemetry) {
        this.events = new LinkedBlockingQueue<>(capacity);
        this.telemetry = telemetry;
        this.telemetry.registerQueueSizeGauge(this::size);
    }

    @Override
    public boolean publish(MarketTradePriceMovedEvent event) {
        boolean published = events.offer(event);
        if (!published) {
            telemetry.recordQueueDrop();
        }
        return published;
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
