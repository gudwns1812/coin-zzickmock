package coin.coinzzickmock.feature.market.web;

import coin.coinzzickmock.feature.market.application.result.MarketCandleResult;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import java.time.Instant;

public record MarketStreamEventResponse(
        MarketStreamEventType kind,
        MarketStreamEventType type,
        String symbol,
        String interval,
        Instant serverTime,
        MarketStreamEventSource source,
        Object data
) {
    public static MarketStreamEventResponse summary(
            MarketSummaryResult result,
            MarketStreamEventSource source,
            Instant serverTime
    ) {
        return new MarketStreamEventResponse(
                MarketStreamEventType.MARKET_SUMMARY,
                MarketStreamEventType.MARKET_SUMMARY,
                result.symbol(),
                null,
                serverTime,
                source,
                MarketSummaryResponse.from(result)
        );
    }

    public static MarketStreamEventResponse candle(
            String symbol,
            CandleSubscription subscription,
            MarketCandleResult result,
            MarketStreamEventSource source,
            Instant serverTime
    ) {
        return new MarketStreamEventResponse(
                MarketStreamEventType.MARKET_CANDLE,
                MarketStreamEventType.MARKET_CANDLE,
                symbol,
                subscription.interval().value(),
                serverTime,
                source,
                MarketCandleResponse.from(result)
        );
    }

    public static MarketStreamEventResponse historyFinalized(
            CandleSubscription subscription,
            MarketCandleHistoryFinalizedResponse response,
            Instant serverTime
    ) {
        return new MarketStreamEventResponse(
                MarketStreamEventType.MARKET_HISTORY_FINALIZED,
                MarketStreamEventType.MARKET_HISTORY_FINALIZED,
                subscription.symbol(),
                subscription.interval().value(),
                serverTime,
                MarketStreamEventSource.LIVE,
                response
        );
    }
}
