package coin.coinzzickmock.feature.market.application.realtime;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record MarketRealtimeSourceSnapshot(
        String symbol,
        MarketRealtimeSourceType sourceType,
        MarketRealtimeHealth health,
        Instant sourceEventTime,
        Instant receivedAt,
        MarketRealtimeReconnectState reconnectState,
        String fallbackReason,
        String lastTradeId,
        Instant lastCandleOpenTime
) {
    public MarketRealtimeSourceSnapshot {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }
        Objects.requireNonNull(sourceType, "sourceType must not be null");
        Objects.requireNonNull(health, "health must not be null");
        Objects.requireNonNull(sourceEventTime, "sourceEventTime must not be null");
        Objects.requireNonNull(receivedAt, "receivedAt must not be null");
        Objects.requireNonNull(reconnectState, "reconnectState must not be null");
    }

    public static MarketRealtimeSourceSnapshot webSocket(
            String symbol,
            MarketRealtimeSourceType sourceType,
            Instant sourceEventTime,
            Instant receivedAt,
            String lastTradeId,
            Instant lastCandleOpenTime
    ) {
        if (!sourceType.isWebSocketSource()) {
            throw new IllegalArgumentException("sourceType must be a WebSocket source");
        }

        return new MarketRealtimeSourceSnapshot(
                symbol,
                sourceType,
                MarketRealtimeHealth.HEALTHY,
                sourceEventTime,
                receivedAt,
                MarketRealtimeReconnectState.CONNECTED,
                null,
                lastTradeId,
                lastCandleOpenTime
        );
    }

    public static MarketRealtimeSourceSnapshot restFallback(
            String symbol,
            MarketRealtimeSourceType sourceType,
            MarketRealtimeHealth health,
            Instant sourceEventTime,
            Instant receivedAt,
            String fallbackReason
    ) {
        if (!sourceType.isRestFallbackSource()) {
            throw new IllegalArgumentException("sourceType must be a REST fallback source");
        }
        if (fallbackReason == null || fallbackReason.isBlank()) {
            throw new IllegalArgumentException("fallbackReason must not be blank");
        }

        return new MarketRealtimeSourceSnapshot(
                symbol,
                sourceType,
                health,
                sourceEventTime,
                receivedAt,
                MarketRealtimeReconnectState.RECONNECTING,
                fallbackReason,
                null,
                null
        );
    }

    public long ageMs(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return Math.max(0, Duration.between(receivedAt, now).toMillis());
    }

    public boolean isFresh(Instant now, Duration maxAge) {
        Objects.requireNonNull(maxAge, "maxAge must not be null");
        return health.canSatisfyFreshness() && ageMs(now) <= maxAge.toMillis();
    }

    public boolean isWebSocketSource() {
        return sourceType.isWebSocketSource();
    }

    public boolean isRestFallbackSource() {
        return sourceType.isRestFallbackSource();
    }
}
