package coin.coinzzickmock.feature.push.infrastructure;

import coin.coinzzickmock.feature.market.candle.application.dto.MarketCandleUpdatedEvent;
import coin.coinzzickmock.feature.market.history.application.dto.MarketHistoryFinalizedEvent;
import coin.coinzzickmock.feature.market.catalog.application.dto.MarketSummaryUpdatedEvent;
import coin.coinzzickmock.feature.market.candle.application.implement.RealtimeMarketCandleProjector;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.web.MarketFinalizedCandleIntervalsReader;
import coin.coinzzickmock.feature.market.web.MarketSummaryResponse;
import coin.coinzzickmock.feature.push.application.publisher.PushEventPublisher;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class MarketPushEventPublicationBridge {
    private static final List<MarketCandleInterval> MINUTE_DERIVED_INTERVALS = List.of(
            MarketCandleInterval.ONE_MINUTE,
            MarketCandleInterval.THREE_MINUTES,
            MarketCandleInterval.FIVE_MINUTES,
            MarketCandleInterval.FIFTEEN_MINUTES
    );
    private static final List<MarketCandleInterval> HOURLY_DERIVED_INTERVALS = List.of(
            MarketCandleInterval.ONE_HOUR,
            MarketCandleInterval.FOUR_HOURS,
            MarketCandleInterval.TWELVE_HOURS,
            MarketCandleInterval.ONE_DAY,
            MarketCandleInterval.ONE_WEEK,
            MarketCandleInterval.ONE_MONTH
    );

    private final MarketFinalizedCandleIntervalsReader finalizedCandleIntervalsReader;
    private final RealtimeMarketCandleProjector realtimeMarketCandleProjector;
    private final PushEventPublisher pushEventPublisher;
    private final PushEventEnvelopeFactory pushEventEnvelopeFactory;

    @EventListener
    void onMarketSummaryUpdated(MarketSummaryUpdatedEvent event) {
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
        if (!event.hasPayload()) {
            return;
        }
        try {
            for (MarketCandleInterval interval : pushedIntervals(event.interval())) {
                realtimeMarketCandleProjector.latest(event.symbol(), interval)
                        .ifPresent(result -> {
                            pushEventPublisher.publish(pushEventEnvelopeFactory.marketCandle(
                                    event.symbol(),
                                    interval.value(),
                                    result
                            ));
                            pushEventPublisher.publish(pushEventEnvelopeFactory.unifiedMarketCandle(
                                    event.symbol(),
                                    interval.value(),
                                    result
                            ));
                        });
            }
        } catch (RuntimeException exception) {
            log.warn("Failed to publish market candle push event. symbol={} interval={}", event.symbol(), event.interval(), exception);
        }
    }

    @EventListener
    void onHistoryFinalized(MarketHistoryFinalizedEvent event) {
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

    private List<MarketCandleInterval> pushedIntervals(String sourceInterval) {
        MarketCandleInterval interval = MarketCandleInterval.from(sourceInterval);
        return switch (interval) {
            case ONE_MINUTE -> MINUTE_DERIVED_INTERVALS;
            case ONE_HOUR -> HOURLY_DERIVED_INTERVALS;
            default -> List.of(interval);
        };
    }
}
