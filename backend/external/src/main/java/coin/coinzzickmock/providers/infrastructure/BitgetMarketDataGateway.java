package coin.coinzzickmock.providers.infrastructure;

import coin.coinzzickmock.providers.connector.ProviderMarketCandleInterval;
import coin.coinzzickmock.providers.connector.ProviderMarketHistoricalCandleSnapshot;
import coin.coinzzickmock.providers.connector.ProviderMarketMinuteCandleSnapshot;
import coin.coinzzickmock.providers.connector.ProviderMarketSnapshot;
import coin.coinzzickmock.providers.connector.MarketHistoricalCandleGranularity;
import coin.coinzzickmock.providers.connector.MarketDataGateway;
import coin.coinzzickmock.providers.infrastructure.mapper.BitgetTickerSnapshotMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.DayOfWeek;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class BitgetMarketDataGateway implements MarketDataGateway {
    private static final int BITGET_HISTORICAL_CANDLE_LIMIT = 200;
    private static final int BITGET_HISTORICAL_CANDLE_TOTAL_LIMIT = 10_000;
    private static final Duration BITGET_HISTORICAL_CANDLE_MAX_RANGE = Duration.ofDays(90);
    private static final String OPERATION_TICKER = "ticker";
    private static final String OPERATION_MINUTE_CANDLES = "minute_candles";
    private static final String OPERATION_HISTORY_CANDLES = "history_candles";
    private static final ZoneOffset STORAGE_ZONE = ZoneOffset.UTC;

    private final RestClient bitgetRestClient;
    private final BitgetTickerSnapshotMapper bitgetTickerSnapshotMapper;
    private final Clock clock;
    private final BitgetTelemetry bitgetTelemetry;

    @Autowired
    public BitgetMarketDataGateway(
            RestClient bitgetRestClient,
            BitgetTickerSnapshotMapper bitgetTickerSnapshotMapper,
            BitgetTelemetry bitgetTelemetry
    ) {
        this(bitgetRestClient, bitgetTickerSnapshotMapper, Clock.systemUTC(), bitgetTelemetry);
    }

    BitgetMarketDataGateway(
            RestClient bitgetRestClient,
            BitgetTickerSnapshotMapper bitgetTickerSnapshotMapper
    ) {
        this(bitgetRestClient, bitgetTickerSnapshotMapper, Clock.systemUTC(), BitgetTelemetry.noop());
    }

    BitgetMarketDataGateway(
            RestClient bitgetRestClient,
            BitgetTickerSnapshotMapper bitgetTickerSnapshotMapper,
            Clock clock
    ) {
        this(bitgetRestClient, bitgetTickerSnapshotMapper, clock, BitgetTelemetry.noop());
    }

    BitgetMarketDataGateway(
            RestClient bitgetRestClient,
            BitgetTickerSnapshotMapper bitgetTickerSnapshotMapper,
            Clock clock,
            BitgetTelemetry bitgetTelemetry
    ) {
        this.bitgetRestClient = bitgetRestClient;
        this.bitgetTickerSnapshotMapper = bitgetTickerSnapshotMapper;
        this.clock = clock;
        this.bitgetTelemetry = bitgetTelemetry;
    }

    @Override
    public List<ProviderMarketSnapshot> loadSupportedMarkets() {
        return List.of(loadMarket("BTCUSDT"), loadMarket("ETHUSDT"));
    }

    @Override
    public ProviderMarketSnapshot loadMarket(String symbol) {
        long startedAt = System.nanoTime();
        try {
            BitgetTickerResponse response = bitgetRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v2/mix/market/ticker")
                            .queryParam("symbol", symbol)
                            .queryParam("productType", "USDT-FUTURES")
                            .build())
                    .retrieve()
                    .body(BitgetTickerResponse.class);

            if (response == null || response.data() == null || response.data().isEmpty()) {
                log.warn("Bitget ticker response is empty; using fallback market snapshot. symbol={} code={}",
                        symbol, responseCode(response));
            }
            ProviderMarketSnapshot market = bitgetTickerSnapshotMapper.fromResponse(symbol, response);
            if (response == null || response.data() == null || response.data().isEmpty()) {
                recordBitgetRequest(OPERATION_TICKER, "empty", startedAt);
                recordBitgetFallback(OPERATION_TICKER, symbol, "empty");
            } else {
                recordBitgetRequest(OPERATION_TICKER, "success", startedAt);
            }
            return market;
        } catch (Exception exception) {
            log.warn("Failed to load Bitget ticker; using fallback market snapshot. symbol={}", symbol, exception);
            recordBitgetRequest(OPERATION_TICKER, "failure", startedAt);
            recordBitgetFallback(OPERATION_TICKER, symbol, "exception");
            return bitgetTickerSnapshotMapper.fallback(symbol);
        }
    }

    @Override
    public List<ProviderMarketMinuteCandleSnapshot> loadMinuteCandles(
            String symbol,
            Instant fromInclusive,
            Instant toExclusive
    ) {
        long startedAt = System.nanoTime();
        try {
            BitgetCandleResponse response = bitgetRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v2/mix/market/candles")
                            .queryParam("symbol", symbol)
                            .queryParam("productType", "USDT-FUTURES")
                            .queryParam("granularity", "1m")
                            .queryParam("startTime", fromInclusive.minus(1, ChronoUnit.MINUTES).toEpochMilli())
                            .queryParam("endTime", toExclusive.toEpochMilli())
                            .queryParam("limit", 1000)
                            .build())
                    .retrieve()
                    .body(BitgetCandleResponse.class);

            if (response == null || response.data() == null || !"00000".equals(response.code())) {
                log.warn(
                        "Bitget candle response is not usable; returning empty history. symbol={} from={} to={} code={}",
                        symbol, fromInclusive, toExclusive, responseCode(response)
                );
                recordBitgetRequest(OPERATION_MINUTE_CANDLES, "invalid_response", startedAt);
                recordBitgetFallback(OPERATION_MINUTE_CANDLES, symbol, "invalid_response");
                return List.of();
            }

            List<ProviderMarketMinuteCandleSnapshot> candles = response.data().stream()
                    .map(this::toMinuteCandleSnapshot)
                    .filter(Objects::nonNull)
                    .filter(candle -> !candle.openTime().isBefore(fromInclusive))
                    .filter(candle -> candle.openTime().isBefore(toExclusive))
                    .sorted(Comparator.comparing(ProviderMarketMinuteCandleSnapshot::openTime))
                    .toList();
            if (candles.isEmpty()) {
                recordBitgetRequest(OPERATION_MINUTE_CANDLES, "empty", startedAt);
                recordBitgetFallback(OPERATION_MINUTE_CANDLES, symbol, "empty");
            } else {
                recordBitgetRequest(OPERATION_MINUTE_CANDLES, "success", startedAt);
            }
            return candles;
        } catch (Exception exception) {
            log.warn("Failed to load Bitget minute candles; returning empty history. symbol={} from={} to={}",
                    symbol, fromInclusive, toExclusive, exception);
            recordBitgetRequest(OPERATION_MINUTE_CANDLES, "failure", startedAt);
            recordBitgetFallback(OPERATION_MINUTE_CANDLES, symbol, "exception");
            return List.of();
        }
    }

    @Override
    public List<ProviderMarketHistoricalCandleSnapshot> loadHistoricalCandles(
            String symbol,
            ProviderMarketCandleInterval interval,
            Instant fromInclusive,
            Instant toExclusive,
            int limit
    ) {
        if (limit <= 0 || !fromInclusive.isBefore(toExclusive)) {
            return List.of();
        }

        Instant alignedFromInclusive = alignHistoricalBoundary(fromInclusive, interval);
        Instant alignedToExclusive = alignHistoricalBoundary(toExclusive, interval);
        Instant providerSafeToExclusive = min(alignedToExclusive, alignHistoricalBoundary(Instant.now(clock), interval));
        if (!alignedFromInclusive.isBefore(providerSafeToExclusive)) {
            return List.of();
        }

        int safeLimit = Math.min(limit, BITGET_HISTORICAL_CANDLE_TOTAL_LIMIT);
        List<ProviderMarketHistoricalCandleSnapshot> candles = new ArrayList<>();
        Instant cursor = alignedFromInclusive;
        int remaining = safeLimit;

        while (cursor.isBefore(providerSafeToExclusive) && remaining > 0) {
            Instant batchEndExclusive = batchEndExclusive(cursor, providerSafeToExclusive, interval, remaining);
            if (!batchEndExclusive.isAfter(cursor)) {
                log.warn("Bitget historical candle batch did not advance. interval={} cursor={} to={}",
                        interval.value(), cursor, providerSafeToExclusive);
                break;
            }
            int requestLimit = Math.min(
                    remaining,
                    Math.min(BITGET_HISTORICAL_CANDLE_LIMIT, candleCount(cursor, batchEndExclusive, interval))
            );
            if (requestLimit <= 0) {
                break;
            }
            candles.addAll(loadHistoricalCandleBatch(symbol, interval, cursor, batchEndExclusive, requestLimit));
            remaining -= requestLimit;
            cursor = batchEndExclusive;
        }

        return newest(safeLimit, candles.stream()
                .filter(candle -> !candle.openTime().isBefore(alignedFromInclusive))
                .filter(candle -> candle.openTime().isBefore(providerSafeToExclusive))
                .sorted(Comparator.comparing(ProviderMarketHistoricalCandleSnapshot::openTime))
                .toList());
    }

    private List<ProviderMarketHistoricalCandleSnapshot> loadHistoricalCandleBatch(
            String symbol,
            ProviderMarketCandleInterval interval,
            Instant fromInclusive,
            Instant toExclusive,
            int requestLimit
    ) {
        long startedAt = System.nanoTime();
        try {
            BitgetCandleResponse response = bitgetRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v2/mix/market/history-candles")
                            .queryParam("symbol", symbol)
                            .queryParam("productType", "USDT-FUTURES")
                            .queryParam("granularity", MarketHistoricalCandleGranularity.from(interval).value())
                            .queryParam("startTime", fromInclusive.toEpochMilli())
                            .queryParam("endTime", toExclusive.toEpochMilli())
                            .queryParam("limit", requestLimit)
                            .build())
                    .retrieve()
                    .body(BitgetCandleResponse.class);

            if (response == null || response.data() == null || !"00000".equals(response.code())) {
                log.warn(
                        "Bitget historical candle response is not usable. symbol={} interval={} from={} to={} code={}",
                        symbol, interval.value(), fromInclusive, toExclusive, responseCode(response)
                );
                recordBitgetRequest(OPERATION_HISTORY_CANDLES, "invalid_response", startedAt);
                recordBitgetFallback(OPERATION_HISTORY_CANDLES, symbol, "invalid_response");
                return List.of();
            }

            List<ProviderMarketHistoricalCandleSnapshot> candles = response.data().stream()
                    .map(rawCandle -> toHistoricalCandleSnapshot(rawCandle, interval))
                    .filter(Objects::nonNull)
                    .filter(candle -> !candle.openTime().isBefore(fromInclusive))
                    .filter(candle -> candle.openTime().isBefore(toExclusive))
                    .sorted(Comparator.comparing(ProviderMarketHistoricalCandleSnapshot::openTime))
                    .toList();
            if (candles.isEmpty()) {
                recordBitgetRequest(OPERATION_HISTORY_CANDLES, "empty", startedAt);
                recordBitgetFallback(OPERATION_HISTORY_CANDLES, symbol, "empty");
            } else {
                recordBitgetRequest(OPERATION_HISTORY_CANDLES, "success", startedAt);
            }
            return candles;
        } catch (Exception exception) {
            log.warn("Failed to load Bitget historical candles. symbol={} interval={} from={} to={}",
                    symbol, interval.value(), fromInclusive, toExclusive, exception);
            recordBitgetRequest(OPERATION_HISTORY_CANDLES, "failure", startedAt);
            recordBitgetFallback(OPERATION_HISTORY_CANDLES, symbol, "exception");
            return List.of();
        }
    }

    private void recordBitgetRequest(String operation, String result, long startedAt) {
        try {
            bitgetTelemetry.recordRequest(operation, result, Duration.ofNanos(System.nanoTime() - startedAt));
        } catch (RuntimeException exception) {
            log.warn("Failed to record Bitget request telemetry. operation={} result={}", operation, result, exception);
        }
    }

    private void recordBitgetFallback(String operation, String symbol, String reason) {
        try {
            bitgetTelemetry.recordFallback(operation, symbol, reason);
        } catch (RuntimeException exception) {
            log.warn("Failed to record Bitget fallback telemetry. operation={} reason={}", operation, reason, exception);
        }
    }

    private Instant batchEndExclusive(
            Instant cursor,
            Instant toExclusive,
            ProviderMarketCandleInterval interval,
            int remaining
    ) {
        Instant requestedEnd = endAfterCandles(cursor, interval, Math.min(remaining, BITGET_HISTORICAL_CANDLE_LIMIT));
        Instant maxRangeEnd = alignHistoricalBoundary(cursor.plus(BITGET_HISTORICAL_CANDLE_MAX_RANGE), interval);
        Instant batchEnd = min(toExclusive, min(requestedEnd, maxRangeEnd));
        if (batchEnd.isAfter(cursor)) {
            return batchEnd;
        }
        return min(toExclusive, nextCandleOpenTime(cursor, interval));
    }

    private Instant endAfterCandles(Instant cursor, ProviderMarketCandleInterval interval, int candleCount) {
        Instant requestedEnd = cursor;
        for (int count = 0; count < candleCount; count++) {
            requestedEnd = nextCandleOpenTime(requestedEnd, interval);
        }
        return requestedEnd;
    }

    private int candleCount(Instant fromInclusive, Instant toExclusive, ProviderMarketCandleInterval interval) {
        int count = 0;
        Instant cursor = fromInclusive;
        while (cursor.isBefore(toExclusive) && count < BITGET_HISTORICAL_CANDLE_LIMIT) {
            cursor = nextCandleOpenTime(cursor, interval);
            count++;
        }
        return count;
    }

    private Instant alignHistoricalBoundary(Instant time, ProviderMarketCandleInterval interval) {
        return switch (interval) {
            case ONE_MINUTE -> alignToMinuteBucket(time, 1);
            case THREE_MINUTES -> alignToMinuteBucket(time, 3);
            case FIVE_MINUTES -> alignToMinuteBucket(time, 5);
            case FIFTEEN_MINUTES -> alignToMinuteBucket(time, 15);
            case ONE_HOUR -> truncate(time, ChronoUnit.HOURS);
            case FOUR_HOURS, TWELVE_HOURS, ONE_DAY, ONE_WEEK, ONE_MONTH -> bucketStart(time, interval);
        };
    }

    private Instant alignToMinuteBucket(Instant time, int bucketMinutes) {
        ZonedDateTime storageTime = atStorageZone(time).truncatedTo(ChronoUnit.MINUTES);
        int alignedMinute = (storageTime.getMinute() / bucketMinutes) * bucketMinutes;
        return storageTime.withMinute(alignedMinute).toInstant();
    }

    private Instant truncate(Instant instant, ChronoUnit unit) {
        return atStorageZone(instant).truncatedTo(unit).toInstant();
    }

    private Instant bucketStart(Instant time, ProviderMarketCandleInterval interval) {
        ZonedDateTime storageTime = atStorageZone(time);
        return switch (interval) {
            case FOUR_HOURS -> storageTime.truncatedTo(ChronoUnit.HOURS)
                    .withHour((storageTime.getHour() / 4) * 4)
                    .toInstant();
            case TWELVE_HOURS -> storageTime.truncatedTo(ChronoUnit.HOURS)
                    .withHour((storageTime.getHour() / 12) * 12)
                    .toInstant();
            case ONE_DAY -> storageTime.truncatedTo(ChronoUnit.DAYS).toInstant();
            case ONE_WEEK -> storageTime.truncatedTo(ChronoUnit.DAYS)
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .toInstant();
            case ONE_MONTH -> storageTime.truncatedTo(ChronoUnit.DAYS)
                    .withDayOfMonth(1)
                    .toInstant();
            default -> throw new IllegalArgumentException("unsupported calendar interval: " + interval);
        };
    }

    private ZonedDateTime atStorageZone(Instant instant) {
        return ZonedDateTime.ofInstant(instant, STORAGE_ZONE);
    }

    private Instant min(Instant first, Instant second) {
        return first.isBefore(second) ? first : second;
    }

    private Instant nextCandleOpenTime(Instant openTime, ProviderMarketCandleInterval interval) {
        return switch (interval) {
            case ONE_MINUTE -> openTime.plus(1, ChronoUnit.MINUTES);
            case THREE_MINUTES -> openTime.plus(3, ChronoUnit.MINUTES);
            case FIVE_MINUTES -> openTime.plus(5, ChronoUnit.MINUTES);
            case FIFTEEN_MINUTES -> openTime.plus(15, ChronoUnit.MINUTES);
            case ONE_HOUR -> openTime.plus(1, ChronoUnit.HOURS);
            case FOUR_HOURS -> openTime.plus(4, ChronoUnit.HOURS);
            case TWELVE_HOURS -> openTime.plus(12, ChronoUnit.HOURS);
            case ONE_DAY -> openTime.plus(1, ChronoUnit.DAYS);
            case ONE_WEEK -> openTime.plus(7, ChronoUnit.DAYS);
            case ONE_MONTH -> ZonedDateTime.ofInstant(openTime, STORAGE_ZONE)
                    .plusMonths(1)
                    .toInstant();
        };
    }

    private List<ProviderMarketHistoricalCandleSnapshot> newest(
            int limit,
            List<ProviderMarketHistoricalCandleSnapshot> candles
    ) {
        Map<Instant, ProviderMarketHistoricalCandleSnapshot> candlesByOpenTime = new LinkedHashMap<>();
        candles.forEach(candle -> candlesByOpenTime.put(candle.openTime(), candle));
        List<ProviderMarketHistoricalCandleSnapshot> sorted = new ArrayList<>(candlesByOpenTime.values());
        if (sorted.size() <= limit) {
            return sorted;
        }
        return sorted.subList(sorted.size() - limit, sorted.size());
    }

    private ProviderMarketMinuteCandleSnapshot toMinuteCandleSnapshot(List<String> rawCandle) {
        if (rawCandle == null || rawCandle.size() < 7) {
            return null;
        }

        try {
            Instant openTime = Instant.ofEpochMilli(Long.parseLong(rawCandle.get(0)));
            return new ProviderMarketMinuteCandleSnapshot(
                    openTime,
                    openTime.plus(1, ChronoUnit.MINUTES),
                    parseDecimal(rawCandle.get(1)),
                    parseDecimal(rawCandle.get(2)),
                    parseDecimal(rawCandle.get(3)),
                    parseDecimal(rawCandle.get(4)),
                    parseDecimal(rawCandle.get(5)),
                    parseDecimal(rawCandle.get(6))
            );
        } catch (RuntimeException exception) {
            log.debug("Skipping malformed Bitget minute candle. rawCandle={}", rawCandle, exception);
            return null;
        }
    }

    private ProviderMarketHistoricalCandleSnapshot toHistoricalCandleSnapshot(
            List<String> rawCandle,
            ProviderMarketCandleInterval interval
    ) {
        if (rawCandle == null || rawCandle.size() < 7) {
            return null;
        }

        try {
            Instant openTime = Instant.ofEpochMilli(Long.parseLong(rawCandle.get(0)));
            return new ProviderMarketHistoricalCandleSnapshot(
                    openTime,
                    closeTime(openTime, interval),
                    parseDecimal(rawCandle.get(1)),
                    parseDecimal(rawCandle.get(2)),
                    parseDecimal(rawCandle.get(3)),
                    parseDecimal(rawCandle.get(4)),
                    parseDecimal(rawCandle.get(5)),
                    parseDecimal(rawCandle.get(6))
            );
        } catch (RuntimeException exception) {
            log.debug("Skipping malformed Bitget historical candle. rawCandle={}", rawCandle, exception);
            return null;
        }
    }

    private Instant closeTime(Instant openTime, ProviderMarketCandleInterval interval) {
        return switch (interval) {
            case ONE_MINUTE -> openTime.plus(1, ChronoUnit.MINUTES);
            case THREE_MINUTES -> openTime.plus(3, ChronoUnit.MINUTES);
            case FIVE_MINUTES -> openTime.plus(5, ChronoUnit.MINUTES);
            case FIFTEEN_MINUTES -> openTime.plus(15, ChronoUnit.MINUTES);
            case ONE_HOUR -> openTime.plus(1, ChronoUnit.HOURS);
            case FOUR_HOURS, TWELVE_HOURS, ONE_DAY, ONE_WEEK, ONE_MONTH -> bucketClose(openTime, interval);
        };
    }

    private Instant bucketClose(Instant openTime, ProviderMarketCandleInterval interval) {
        ZonedDateTime storageTime = atStorageZone(openTime);
        return switch (interval) {
            case FOUR_HOURS -> storageTime.plusHours(4).toInstant();
            case TWELVE_HOURS -> storageTime.plusHours(12).toInstant();
            case ONE_DAY -> storageTime.plusDays(1).toInstant();
            case ONE_WEEK -> storageTime.plusWeeks(1).toInstant();
            case ONE_MONTH -> storageTime.plusMonths(1).toInstant();
            default -> throw new IllegalArgumentException("unsupported calendar interval: " + interval);
        };
    }

    private BigDecimal parseDecimal(String value) {
        return new BigDecimal(value);
    }

    private String responseCode(BitgetTickerResponse response) {
        return response == null ? null : response.code();
    }

    private String responseCode(BitgetCandleResponse response) {
        return response == null ? null : response.code();
    }
}
