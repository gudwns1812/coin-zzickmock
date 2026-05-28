package coin.coinzzickmock.feature.push.infrastructure;

import coin.coinzzickmock.feature.market.application.dto.MarketCandleUpdatedEvent;
import coin.coinzzickmock.feature.market.application.dto.MarketCandleResult;
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
        return marketCandle(event.symbol(), event.interval(), toCandleResponse(event));
    }

    public PushEventEnvelope marketCandle(String symbol, String interval, MarketCandleResponse response) {
        Instant now = Instant.now();
        return PushEventEnvelope.marketCandle(
                symbol,
                interval,
                candleDedupeKey("market-candle", symbol, interval, response),
                response.closeTime() == null ? now : response.closeTime(),
                now,
                properties.marketMaxAge(),
                json(response)
        );
    }

    public PushEventEnvelope marketCandle(String symbol, String interval, MarketCandleResult result) {
        return marketCandle(symbol, interval, toCandleResponse(result));
    }

    public PushEventEnvelope unifiedMarketCandle(MarketCandleUpdatedEvent event) {
        return unifiedMarketCandle(event.symbol(), event.interval(), toCandleResponse(event));
    }

    public PushEventEnvelope unifiedMarketCandle(String symbol, String interval, MarketCandleResponse response) {
        Instant now = Instant.now();
        return PushEventEnvelope.marketUnifiedCandle(
                symbol,
                interval,
                candleDedupeKey("unified-candle", symbol, interval, response),
                response.closeTime() == null ? now : response.closeTime(),
                now,
                properties.marketMaxAge(),
                json(MarketStreamEventResponse.candle(
                        new CandleSubscription(symbol, interval),
                        response,
                        MarketStreamEventSource.LIVE,
                        now
                ))
        );
    }

    public PushEventEnvelope unifiedMarketCandle(String symbol, String interval, MarketCandleResult result) {
        return unifiedMarketCandle(symbol, interval, toCandleResponse(result));
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
        return toCandleResponse(event.candle());
    }

    private MarketCandleResponse toCandleResponse(MarketCandleResult result) {
        return new MarketCandleResponse(
                result.openTime(),
                result.closeTime(),
                result.openPrice(),
                result.highPrice(),
                result.lowPrice(),
                result.closePrice(),
                result.volume()
        );
    }

    private String tradingDedupeKey(TradingExecutionEvent event) {
        String orderPart = event.orderId() == null ? "position" : event.orderId();
        return "trading:" + event.memberId() + ":" + event.type() + ":" + event.symbol() + ":" + orderPart + ":" + event.executionPrice();
    }

    private String candleDedupeKey(
            String prefix,
            String symbol,
            String interval,
            MarketCandleResponse response
    ) {
        return prefix + ":" + symbol + ":" + interval + ":" + response.openTime()
                + ":" + response.openPrice()
                + ":" + response.highPrice()
                + ":" + response.lowPrice()
                + ":" + response.closePrice()
                + ":" + response.volume();
    }

    private String json(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize push payload.", exception);
        }
    }
}
