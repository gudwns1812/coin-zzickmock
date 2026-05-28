package coin.coinzzickmock.feature.push.application.dto;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record PushEventEnvelope(
        int schemaVersion,
        PushStream stream,
        PushEventType eventType,
        PushTargetType targetType,
        String symbol,
        String interval,
        Long memberId,
        String dedupeKey,
        Instant eventTime,
        Instant publishedAt,
        Duration maxAge,
        String payloadJson
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public PushEventEnvelope {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("unsupported push event schema version: " + schemaVersion);
        }
        Objects.requireNonNull(stream, "stream must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(targetType, "targetType must not be null");
        Objects.requireNonNull(eventTime, "eventTime must not be null");
        Objects.requireNonNull(publishedAt, "publishedAt must not be null");
        Objects.requireNonNull(maxAge, "maxAge must not be null");
        if (maxAge.isNegative() || maxAge.isZero()) {
            throw new IllegalArgumentException("maxAge must be positive");
        }
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new IllegalArgumentException("payloadJson must not be blank");
        }
        if (targetType == PushTargetType.MEMBER && memberId == null) {
            throw new IllegalArgumentException("memberId is required for MEMBER push target");
        }
        if ((targetType == PushTargetType.SYMBOL || targetType == PushTargetType.SYMBOL_INTERVAL)
                && (symbol == null || symbol.isBlank())) {
            throw new IllegalArgumentException("symbol is required for symbol push target");
        }
        if (targetType == PushTargetType.SYMBOL_INTERVAL && (interval == null || interval.isBlank())) {
            throw new IllegalArgumentException("interval is required for symbol interval push target");
        }
    }

    public static PushEventEnvelope marketSummary(String symbol, String dedupeKey, Instant eventTime, Instant publishedAt, Duration maxAge, String payloadJson) {
        return new PushEventEnvelope(CURRENT_SCHEMA_VERSION, PushStream.MARKET, PushEventType.MARKET_SUMMARY,
                PushTargetType.SYMBOL, symbol, null, null, dedupeKey, eventTime, publishedAt, maxAge, payloadJson);
    }

    public static PushEventEnvelope marketUnifiedSummary(String symbol, String dedupeKey, Instant eventTime, Instant publishedAt, Duration maxAge, String payloadJson) {
        return new PushEventEnvelope(CURRENT_SCHEMA_VERSION, PushStream.MARKET, PushEventType.MARKET_UNIFIED_SUMMARY,
                PushTargetType.SYMBOL, symbol, null, null, dedupeKey, eventTime, publishedAt, maxAge, payloadJson);
    }

    public static PushEventEnvelope marketCandle(String symbol, String interval, String dedupeKey, Instant eventTime, Instant publishedAt, Duration maxAge, String payloadJson) {
        return new PushEventEnvelope(CURRENT_SCHEMA_VERSION, PushStream.MARKET, PushEventType.MARKET_CANDLE,
                PushTargetType.SYMBOL_INTERVAL, symbol, interval, null, dedupeKey, eventTime, publishedAt, maxAge, payloadJson);
    }

    public static PushEventEnvelope marketUnifiedCandle(String symbol, String interval, String dedupeKey, Instant eventTime, Instant publishedAt, Duration maxAge, String payloadJson) {
        return new PushEventEnvelope(CURRENT_SCHEMA_VERSION, PushStream.MARKET, PushEventType.MARKET_UNIFIED_CANDLE,
                PushTargetType.SYMBOL_INTERVAL, symbol, interval, null, dedupeKey, eventTime, publishedAt, maxAge, payloadJson);
    }

    public static PushEventEnvelope marketHistoryFinalized(String symbol, String interval, String dedupeKey, Instant eventTime, Instant publishedAt, Duration maxAge, String payloadJson) {
        return new PushEventEnvelope(CURRENT_SCHEMA_VERSION, PushStream.MARKET, PushEventType.MARKET_HISTORY_FINALIZED,
                PushTargetType.SYMBOL_INTERVAL, symbol, interval, null, dedupeKey, eventTime, publishedAt, maxAge, payloadJson);
    }

    public static PushEventEnvelope marketUnifiedHistoryFinalized(String symbol, String interval, String dedupeKey, Instant eventTime, Instant publishedAt, Duration maxAge, String payloadJson) {
        return new PushEventEnvelope(CURRENT_SCHEMA_VERSION, PushStream.MARKET, PushEventType.MARKET_UNIFIED_HISTORY_FINALIZED,
                PushTargetType.SYMBOL_INTERVAL, symbol, interval, null, dedupeKey, eventTime, publishedAt, maxAge, payloadJson);
    }

    public static PushEventEnvelope tradingExecution(Long memberId, String symbol, String dedupeKey, Instant eventTime, Instant publishedAt, Duration maxAge, String payloadJson) {
        return new PushEventEnvelope(CURRENT_SCHEMA_VERSION, PushStream.TRADING, PushEventType.TRADING_EXECUTION,
                PushTargetType.MEMBER, symbol, null, memberId, dedupeKey, eventTime, publishedAt, maxAge, payloadJson);
    }

    public boolean staleAt(Instant now) {
        return publishedAt.plus(maxAge).isBefore(now);
    }
}
