package coin.coinzzickmock.feature.market.application.repair;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketHistoryRepairQueueRequestedEventListener {
    private final MarketHistoryRepairQueuePublisher marketHistoryRepairQueuePublisher;

    @EventListener
    public void onRepairQueueRequested(MarketHistoryRepairQueueRequestedEvent event) {
        marketHistoryRepairQueuePublisher.enqueue(event.eventId());
    }
}
