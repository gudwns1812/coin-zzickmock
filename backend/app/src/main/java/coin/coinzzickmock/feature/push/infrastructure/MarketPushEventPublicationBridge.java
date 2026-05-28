package coin.coinzzickmock.feature.push.infrastructure;

import coin.coinzzickmock.feature.market.application.dto.MarketCandleUpdatedEvent;
import coin.coinzzickmock.feature.market.application.dto.MarketHistoryFinalizedEvent;
import coin.coinzzickmock.feature.market.application.dto.MarketSummaryUpdatedEvent;
import coin.coinzzickmock.feature.market.web.MarketFinalizedCandleIntervalsReader;
import coin.coinzzickmock.feature.market.web.MarketSummaryResponse;
import coin.coinzzickmock.feature.push.application.publisher.PushEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class MarketPushEventPublicationBridge {
    private final MarketFinalizedCandleIntervalsReader finalizedCandleIntervalsReader;
    private final PushEventPublisher pushEventPublisher;
    private final PushEventEnvelopeFactory pushEventEnvelopeFactory;
    private final PushPublicationProperties pushPublicationProperties;

    @EventListener
    void onMarketSummaryUpdated(MarketSummaryUpdatedEvent event) {
        if (!pushPublicationProperties.deliveryMode().publishesToRedis()) {
            return;
        }
        MarketSummaryResponse response = toResponse(event);
        try {
            pushEventPublisher.publish(pushEventEnvelopeFactory.marketSummary(event, response));
            pushEventPublisher.publish(pushEventEnvelopeFactory.unifiedMarketSummary(event, response));
        } catch (RuntimeException exception) {
            log.warn("Failed to publish market summary push event. symbol={}", response.symbol(), exception);
        }
    }

    private MarketSummaryResponse toResponse(MarketSummaryUpdatedEvent event) {
        var result = event.result();
        return MarketSummaryResponse.of(
                result.symbol(),
                result.displayName(),
                result.lastPrice(),
                result.markPrice(),
                result.indexPrice(),
                result.fundingRate(),
                result.change24h(),
                result.turnover24hUsdt(),
                result.turnover24hUsdt(),
                result.serverTime(),
                result.nextFundingAt(),
                result.fundingIntervalHours()
        );
    }

    @EventListener
    void onCandleUpdated(MarketCandleUpdatedEvent event) {
        if (!pushPublicationProperties.deliveryMode().publishesToRedis() || !event.hasPayload()) {
            return;
        }
        try {
            pushEventPublisher.publish(pushEventEnvelopeFactory.marketCandle(event));
            pushEventPublisher.publish(pushEventEnvelopeFactory.unifiedMarketCandle(event));
        } catch (RuntimeException exception) {
            log.warn("Failed to publish market candle push event. symbol={} interval={}", event.symbol(), event.interval(), exception);
        }
    }

    @EventListener
    void onHistoryFinalized(MarketHistoryFinalizedEvent event) {
        if (!pushPublicationProperties.deliveryMode().publishesToRedis()) {
            return;
        }
        try {
            for (String interval : finalizedCandleIntervalsReader.readAffectedIntervals(event.symbol(), event.openTime(), event.closeTime())) {
                pushEventPublisher.publish(pushEventEnvelopeFactory.marketHistoryFinalized(event, interval));
                pushEventPublisher.publish(pushEventEnvelopeFactory.unifiedMarketHistoryFinalized(event, interval));
            }
        } catch (RuntimeException exception) {
            log.warn("Failed to publish market history push event. symbol={} openTime={} closeTime={}",
                    event.symbol(), event.openTime(), event.closeTime(), exception);
        }
    }
}
