package coin.coinzzickmock.feature.order.application.realtime;

import coin.coinzzickmock.feature.market.application.realtime.MarketTradePriceMovedEvent;
import coin.coinzzickmock.feature.market.application.realtime.MarketTradePriceMovementPublisher;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.springframework.stereotype.Component;

@Component
public class MarketTradeMovementQueue implements MarketTradePriceMovementPublisher {
    private static final int DEFAULT_CAPACITY = 10_000;

    private final BlockingQueue<MarketTradePriceMovedEvent> events = new LinkedBlockingQueue<>(DEFAULT_CAPACITY);

    @Override
    public boolean publish(MarketTradePriceMovedEvent event) {
        return events.offer(event);
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
