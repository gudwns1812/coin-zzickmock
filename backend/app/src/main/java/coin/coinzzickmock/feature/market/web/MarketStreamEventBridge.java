package coin.coinzzickmock.feature.market.web;

import coin.coinzzickmock.feature.market.application.realtime.MarketCandleUpdatedEvent;
import coin.coinzzickmock.feature.market.application.realtime.MarketHistoryFinalizedEvent;
import coin.coinzzickmock.feature.market.application.realtime.MarketSummaryUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketStreamEventBridge {
    private final MarketRealtimeSseBroker marketRealtimeSseBroker;
    private final MarketCandleRealtimeSseBroker marketCandleRealtimeSseBroker;
    private final MarketStreamBroker marketStreamBroker;

    @EventListener
    public void onMarketSummaryUpdated(MarketSummaryUpdatedEvent event) {
        MarketSummaryResponse response = MarketStreamResponseMapper.toResponse(event.result());
        tryFanOutSummary(response, "summary");
        tryFanOutSummary(response, "unified");
    }

    @EventListener
    public void onCandleUpdated(MarketCandleUpdatedEvent event) {
        try {
            marketCandleRealtimeSseBroker.onCandleUpdated(event.symbol());
        } catch (RuntimeException exception) {
            log.warn("Failed to fan out raw candle SSE update. symbol={}", event.symbol(), exception);
        }
        try {
            marketStreamBroker.onCandleUpdated(event.symbol());
        } catch (RuntimeException exception) {
            log.warn("Failed to fan out unified candle SSE update. symbol={}", event.symbol(), exception);
        }
    }

    @EventListener
    public void onHistoryFinalized(MarketHistoryFinalizedEvent event) {
        try {
            marketCandleRealtimeSseBroker.onHistoryFinalized(event.symbol(), event.openTime(), event.closeTime());
        } catch (RuntimeException exception) {
            log.warn("Failed to fan out raw candle history SSE update. symbol={} openTime={} closeTime={}",
                    event.symbol(), event.openTime(), event.closeTime(), exception);
        }
        try {
            marketStreamBroker.onHistoryFinalized(event.symbol(), event.openTime(), event.closeTime());
        } catch (RuntimeException exception) {
            log.warn("Failed to fan out unified candle history SSE update. symbol={} openTime={} closeTime={}",
                    event.symbol(), event.openTime(), event.closeTime(), exception);
        }
    }

    private void tryFanOutSummary(MarketSummaryResponse response, String streamKind) {
        try {
            if ("summary".equals(streamKind)) {
                marketRealtimeSseBroker.onMarketUpdated(response);
            } else {
                marketStreamBroker.onMarketUpdated(response);
            }
        } catch (RuntimeException exception) {
            log.warn("Failed to fan out {} market SSE update. symbol={}",
                    streamKind, response.symbol(), exception);
        }
    }
}
