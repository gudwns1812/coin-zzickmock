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
            String symbol,
            MarketStreamEventSource source,
            Object data
    ) {
        return of(MarketStreamEventType.MARKET_SUMMARY, symbol, null, source, data);
    }

    public static MarketStreamEventResponse candle(
            String symbol,
            String interval,
            MarketStreamEventSource source,
            Object data
    ) {
        return of(MarketStreamEventType.MARKET_CANDLE, symbol, interval, source, data);
    }

    public static MarketStreamEventResponse historyFinalized(
            String symbol,
            String interval,
            MarketStreamEventSource source,
            Object data
    ) {
        return of(MarketStreamEventType.MARKET_HISTORY_FINALIZED, symbol, interval, source, data);
    }

    private static MarketStreamEventResponse of(
            MarketStreamEventType type,
            String symbol,
            String interval,
            MarketStreamEventSource source,
            Object data
    ) {
        return new MarketStreamEventResponse(type, type, symbol, interval, Instant.now(), source, data);
    }
}
