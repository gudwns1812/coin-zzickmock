package coin.coinzzickmock.feature.push.infrastructure;

import coin.coinzzickmock.feature.market.application.dto.MarketCandleUpdatedEvent;
import coin.coinzzickmock.feature.market.application.dto.MarketHistoryFinalizedEvent;
import coin.coinzzickmock.feature.market.application.dto.MarketSummaryUpdatedEvent;
import coin.coinzzickmock.feature.market.web.CandleSubscription;
import coin.coinzzickmock.feature.market.web.MarketCandleHistoryFinalizedResponse;
import coin.coinzzickmock.feature.market.web.MarketCandleResponse;
import coin.coinzzickmock.feature.market.web.MarketStreamEventResponse;
import coin.coinzzickmock.feature.market.web.MarketStreamEventSource;
import coin.coinzzickmock.feature.market.web.MarketSummaryResponse;
import coin.coinzzickmock.feature.order.application.dto.TradingExecutionEvent;
import coin.coinzzickmock.feature.order.web.TradingExecutionEventResponse;
import coin.coinzzickmock.feature.push.application.dto.PushEventEnvelope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PushEventEnvelopeFactory {
    private final ObjectMapper objectMapper;
    private final PushPublicationProperties properties;

    public PushEventEnvelope marketSummary(MarketSummaryUpdatedEvent event, MarketSummaryResponse response) {
        Instant now = Instant.now();
        return PushEventEnvelope.marketSummary(
                response.symbol(),
                "market-summary:" + response.symbol() + ":" + response.serverTime(),
                response.serverTime() == null ? now : response.serverTime(),
                now,
                properties.marketMaxAge(),
                json(response)
        );
    }

    public PushEventEnvelope unifiedMarketSummary(MarketSummaryUpdatedEvent event, MarketSummaryResponse response) {
        Instant now = Instant.now();
        return PushEventEnvelope.marketUnifiedSummary(
                response.symbol(),
                "unified-summary:" + response.symbol() + ":" + response.serverTime(),
                response.serverTime() == null ? now : response.serverTime(),
                now,
                properties.marketMaxAge(),
                json(MarketStreamEventResponse.summary(response, MarketStreamEventSource.LIVE, now))
        );
    }

    public PushEventEnvelope marketCandle(MarketCandleUpdatedEvent event) {
        Instant now = Instant.now();
        return PushEventEnvelope.marketCandle(
                event.symbol(),
                event.interval(),
                "market-candle:" + event.symbol() + ":" + event.interval() + ":" + event.candle().openTime(),
                event.candle().closeTime() == null ? now : event.candle().closeTime(),
                now,
                properties.marketMaxAge(),
                json(toCandleResponse(event))
        );
    }

    public PushEventEnvelope unifiedMarketCandle(MarketCandleUpdatedEvent event) {
        Instant now = Instant.now();
        return PushEventEnvelope.marketUnifiedCandle(
                event.symbol(),
                event.interval(),
                "unified-candle:" + event.symbol() + ":" + event.interval() + ":" + event.candle().openTime(),
                event.candle().closeTime() == null ? now : event.candle().closeTime(),
                now,
                properties.marketMaxAge(),
                json(MarketStreamEventResponse.candle(
                        new CandleSubscription(event.symbol(), event.interval()),
                        toCandleResponse(event),
                        MarketStreamEventSource.LIVE,
                        now
                ))
        );
    }

    public PushEventEnvelope marketHistoryFinalized(MarketHistoryFinalizedEvent event, String interval) {
        Instant now = Instant.now();
        return PushEventEnvelope.marketHistoryFinalized(
                event.symbol(),
                interval,
                "market-history:" + event.symbol() + ":" + interval + ":" + event.closeTime(),
                event.closeTime(),
                now,
                properties.marketMaxAge(),
                json(MarketCandleHistoryFinalizedResponse.of(event.symbol(), event.openTime(), event.closeTime(), List.of(interval)))
        );
    }

    public PushEventEnvelope unifiedMarketHistoryFinalized(MarketHistoryFinalizedEvent event, String interval) {
        Instant now = Instant.now();
        return PushEventEnvelope.marketUnifiedHistoryFinalized(
                event.symbol(),
                interval,
                "unified-history:" + event.symbol() + ":" + interval + ":" + event.closeTime(),
                event.closeTime(),
                now,
                properties.marketMaxAge(),
                json(MarketStreamEventResponse.historyFinalized(
                        new CandleSubscription(event.symbol(), interval),
                        MarketCandleHistoryFinalizedResponse.of(event.symbol(), event.openTime(), event.closeTime(), List.of(interval)),
                        now
                ))
        );
    }

    public PushEventEnvelope tradingExecution(TradingExecutionEvent event) {
        Instant now = Instant.now();
        return PushEventEnvelope.tradingExecution(
                event.memberId(),
                event.symbol(),
                tradingDedupeKey(event),
                now,
                now,
                properties.tradingMaxAge(),
                json(TradingExecutionEventResponse.from(event))
        );
    }

    private MarketCandleResponse toCandleResponse(MarketCandleUpdatedEvent event) {
        return new MarketCandleResponse(
                event.candle().openTime(),
                event.candle().closeTime(),
                event.candle().openPrice(),
                event.candle().highPrice(),
                event.candle().lowPrice(),
                event.candle().closePrice(),
                event.candle().volume()
        );
    }

    private String tradingDedupeKey(TradingExecutionEvent event) {
        String orderPart = event.orderId() == null ? "position" : event.orderId();
        return "trading:" + event.memberId() + ":" + event.type() + ":" + event.symbol() + ":" + orderPart + ":" + event.executionPrice();
    }

    private String json(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize push payload.", exception);
        }
    }
}
