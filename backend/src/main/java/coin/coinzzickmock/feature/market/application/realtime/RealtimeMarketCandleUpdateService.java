package coin.coinzzickmock.feature.market.application.realtime;

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
            applicationEventPublisher.publishEvent(new MarketCandleUpdatedEvent(update.symbol()));
        }
        return accepted;
    }
}
