package coin.coinzzickmock.providers.infrastructure;

import java.time.Instant;

public sealed interface BitgetWebSocketMarketEvent
        permits BitgetWebSocketTradeEvent, BitgetWebSocketTickerEvent, BitgetWebSocketCandleEvent {
    String symbol();

    Instant sourceEventTime();

    Instant receivedAt();
}
