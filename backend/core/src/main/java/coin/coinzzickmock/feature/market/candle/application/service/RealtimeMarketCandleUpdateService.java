package coin.coinzzickmock.feature.market.candle.application.service;

import coin.coinzzickmock.feature.market.quote.application.implement.RealtimeMarketDataStore;
import coin.coinzzickmock.feature.market.candle.application.dto.MarketCandleUpdatedEvent;
import coin.coinzzickmock.feature.market.candle.application.dto.RealtimeMarketCandleUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RealtimeMarketCandleUpdateService {
    private final RealtimeMarketDataStore realtimeMarketDataStore;
    private final ApplicationEventPublisher applicationEventPublisher;

    public boolean accept(RealtimeMarketCandleUpdate update) {
        boolean accepted = realtimeMarketDataStore.acceptCandle(update);
        if (accepted) {
            applicationEventPublisher.publishEvent(MarketCandleUpdatedEvent.from(update));
        }
        return accepted;
    }
}
