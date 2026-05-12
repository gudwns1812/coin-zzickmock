package coin.coinzzickmock.feature.market.web;

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
            MarketSummaryResponse result,
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
                result
        );
    }

    public static MarketStreamEventResponse candle(
            CandleSubscription subscription,
            MarketCandleResponse result,
            MarketStreamEventSource source,
            Instant serverTime
    ) {
        return new MarketStreamEventResponse(
                MarketStreamEventType.MARKET_CANDLE,
                MarketStreamEventType.MARKET_CANDLE,
                subscription.symbol(),
                subscription.interval(),
                serverTime,
                source,
                result
        );
    }

    public static MarketStreamEventResponse historyFinalized(
            CandleSubscription subscription,
            MarketCandleHistoryFinalizedResponse response,
            Instant serverTime
    ) {
        // History finalization is emitted only after persisted market history closes, so it is always live.
        return new MarketStreamEventResponse(
                MarketStreamEventType.MARKET_HISTORY_FINALIZED,
                MarketStreamEventType.MARKET_HISTORY_FINALIZED,
                subscription.symbol(),
                subscription.interval(),
                serverTime,
                MarketStreamEventSource.LIVE,
                response
        );
    }
}
