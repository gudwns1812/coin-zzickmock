package coin.coinzzickmock.feature.market.web;

import coin.coinzzickmock.feature.market.application.realtime.MarketCandleUpdatedEvent;
import coin.coinzzickmock.feature.market.application.realtime.MarketHistoryFinalizedEvent;
import coin.coinzzickmock.feature.market.application.realtime.MarketSummaryUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketStreamEventBridge {
    private final MarketRealtimeSseBroker marketRealtimeSseBroker;
    private final MarketCandleRealtimeSseBroker marketCandleRealtimeSseBroker;
    private final MarketStreamBroker marketStreamBroker;

    @EventListener
    public void onMarketSummaryUpdated(MarketSummaryUpdatedEvent event) {
        MarketSummaryResponse response = MarketStreamResponseMapper.toResponse(event.result());
        marketRealtimeSseBroker.onMarketUpdated(response);
        marketStreamBroker.onMarketUpdated(response);
    }

    @EventListener
    public void onCandleUpdated(MarketCandleUpdatedEvent event) {
        marketCandleRealtimeSseBroker.onCandleUpdated(event.symbol());
        marketStreamBroker.onCandleUpdated(event.symbol());
    }

    @EventListener
    public void onHistoryFinalized(MarketHistoryFinalizedEvent event) {
        marketCandleRealtimeSseBroker.onHistoryFinalized(event.symbol(), event.openTime(), event.closeTime());
        marketStreamBroker.onHistoryFinalized(event.symbol(), event.openTime(), event.closeTime());
    }
}
