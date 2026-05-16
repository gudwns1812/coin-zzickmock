package coin.coinzzickmock.feature.order.application.service;

import coin.coinzzickmock.feature.market.application.realtime.MarketTradePriceMovedEvent;
import coin.coinzzickmock.feature.market.application.realtime.MarketTradePriceMovementPublisher;
import coin.coinzzickmock.feature.order.application.implement.OrderMarketTradeMovementQueue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderMarketTradeMovementListener implements MarketTradePriceMovementPublisher {
    private final OrderMarketTradeMovementQueue queue;

    @Override
    public boolean publish(MarketTradePriceMovedEvent event) {
        return queue.enqueue(event);
    }
}
