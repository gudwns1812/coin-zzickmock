package coin.coinzzickmock.providers.connector;

import java.time.Instant;

public sealed interface ProviderMarketRealtimeEvent
        permits ProviderMarketTradeEvent, ProviderMarketTickerEvent, ProviderMarketCandleEvent {
    String symbol();

    Instant sourceEventTime();

    Instant receivedAt();
}
