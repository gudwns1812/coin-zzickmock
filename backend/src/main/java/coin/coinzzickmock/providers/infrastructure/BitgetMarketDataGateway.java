package coin.coinzzickmock.providers.infrastructure;

import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketHistoricalCandleSnapshot;
import coin.coinzzickmock.feature.market.domain.MarketMinuteCandleSnapshot;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.market.domain.MarketTime;
import coin.coinzzickmock.providers.connector.MarketHistoricalCandleGranularity;
import coin.coinzzickmock.providers.connector.MarketDataGateway;
import coin.coinzzickmock.providers.infrastructure.mapper.BitgetTickerSnapshotMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
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
    private static final Duration BITGET_HISTORICAL_CANDLE_MAX_RANGE = Duration.ofDays(90);

    private final RestClient bitgetRestClient;
    private final BitgetTickerSnapshotMapper bitgetTickerSnapshotMapper;
    private final Clock clock;

    @Autowired
    public BitgetMarketDataGateway(
            RestClient bitgetRestClient,
            BitgetTickerSnapshotMapper bitgetTickerSnapshotMapper
    ) {
        this(bitgetRestClient, bitgetTickerSnapshotMapper, Clock.systemUTC());
    }

    BitgetMarketDataGateway(
            RestClient bitgetRestClient,
            BitgetTickerSnapshotMapper bitgetTickerSnapshotMapper,
            Clock clock
    ) {
        this.bitgetRestClient = bitgetRestClient;
        this.bitgetTickerSnapshotMapper = bitgetTickerSnapshotMapper;
        this.clock = clock;
    }

    @Override
    public List<MarketSnapshot> loadSupportedMarkets() {
        return List.of(loadMarket("BTCUSDT"), loadMarket("ETHUSDT"));
    }

    @Override
    public MarketSnapshot loadMarket(String symbol) {
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
                log.warn("Bitget ticker response is empty; using fallback market snapshot. symbol={} code={} message={}",
                        symbol, responseCode(response), responseMessage(response));
            }
            return bitgetTickerSnapshotMapper.fromResponse(symbol, response);
        } catch (Exception exception) {
            log.warn("Failed to load Bitget ticker; using fallback market snapshot. symbol={}", symbol, exception);
            return bitgetTickerSnapshotMapper.fallback(symbol);
        }
    }

    @Override
    public List<MarketMinuteCandleSnapshot> loadMinuteCandles(
            String symbol,
            Instant fromInclusive,
            Instant toExclusive
    ) {
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
                        "Bitget candle response is not usable; returning empty history. symbol={} from={} to={} code={} message={}",
                        symbol, fromInclusive, toExclusive, responseCode(response), responseMessage(response)
                );
                return List.of();
            }

            return response.data().stream()
                    .map(this::toMinuteCandleSnapshot)
                    .filter(Objects::nonNull)
                    .filter(candle -> !candle.openTime().isBefore(fromInclusive))
                    .filter(candle -> candle.openTime().isBefore(toExclusive))
                    .sorted(Comparator.comparing(MarketMinuteCandleSnapshot::openTime))
                    .toList();
        } catch (Exception exception) {
            log.warn("Failed to load Bitget minute candles; returning empty history. symbol={} from={} to={}",
                    symbol, fromInclusive, toExclusive, exception);
            return List.of();
        }
    }

    @Override
    public List<MarketHistoricalCandleSnapshot> loadHistoricalCandles(
            String symbol,
            MarketCandleInterval interval,
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

        List<MarketHistoricalCandleSnapshot> candles = new ArrayList<>();
        Instant cursor = alignedFromInclusive;
        int remaining = limit;

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

        return newest(limit, candles.stream()
                .filter(candle -> !candle.openTime().isBefore(alignedFromInclusive))
                .filter(candle -> candle.openTime().isBefore(providerSafeToExclusive))
                .sorted(Comparator.comparing(MarketHistoricalCandleSnapshot::openTime))
                .toList());
    }

    private List<MarketHistoricalCandleSnapshot> loadHistoricalCandleBatch(
            String symbol,
            MarketCandleInterval interval,
            Instant fromInclusive,
            Instant toExclusive,
            int requestLimit
    ) {
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
                        "Bitget historical candle response is not usable. symbol={} interval={} from={} to={} code={} message={}",
                        symbol, interval.value(), fromInclusive, toExclusive, responseCode(response),
                        responseMessage(response)
                );
                return List.of();
            }

            return response.data().stream()
                    .map(rawCandle -> toHistoricalCandleSnapshot(rawCandle, interval))
                    .filter(Objects::nonNull)
                    .filter(candle -> !candle.openTime().isBefore(fromInclusive))
                    .filter(candle -> candle.openTime().isBefore(toExclusive))
                    .sorted(Comparator.comparing(MarketHistoricalCandleSnapshot::openTime))
                    .toList();
        } catch (Exception exception) {
            log.warn("Failed to load Bitget historical candles. symbol={} interval={} from={} to={}",
                    symbol, interval.value(), fromInclusive, toExclusive, exception);
            return List.of();
        }
    }

    private Instant batchEndExclusive(
            Instant cursor,
            Instant toExclusive,
            MarketCandleInterval interval,
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

    private Instant endAfterCandles(Instant cursor, MarketCandleInterval interval, int candleCount) {
        Instant requestedEnd = cursor;
        for (int count = 0; count < candleCount; count++) {
            requestedEnd = nextCandleOpenTime(requestedEnd, interval);
        }
        return requestedEnd;
    }

    private int candleCount(Instant fromInclusive, Instant toExclusive, MarketCandleInterval interval) {
        int count = 0;
        Instant cursor = fromInclusive;
        while (cursor.isBefore(toExclusive) && count < BITGET_HISTORICAL_CANDLE_LIMIT) {
            cursor = nextCandleOpenTime(cursor, interval);
            count++;
        }
        return count;
    }

    private Instant alignHistoricalBoundary(Instant time, MarketCandleInterval interval) {
        return switch (interval) {
            case ONE_MINUTE -> MarketTime.alignToMinuteBucket(time, 1);
            case THREE_MINUTES -> MarketTime.alignToMinuteBucket(time, 3);
            case FIVE_MINUTES -> MarketTime.alignToMinuteBucket(time, 5);
            case FIFTEEN_MINUTES -> MarketTime.alignToMinuteBucket(time, 15);
            case ONE_HOUR -> MarketTime.truncate(time, ChronoUnit.HOURS);
            case FOUR_HOURS, TWELVE_HOURS, ONE_DAY, ONE_WEEK, ONE_MONTH ->
                    MarketTime.bucketStart(time, interval);
        };
    }

    private Instant min(Instant first, Instant second) {
        return first.isBefore(second) ? first : second;
    }

    private Instant nextCandleOpenTime(Instant openTime, MarketCandleInterval interval) {
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
            case ONE_MONTH -> ZonedDateTime.ofInstant(openTime, MarketTime.STORAGE_ZONE)
                    .plusMonths(1)
                    .toInstant();
        };
    }

    private List<MarketHistoricalCandleSnapshot> newest(
            int limit,
            List<MarketHistoricalCandleSnapshot> candles
    ) {
        Map<Instant, MarketHistoricalCandleSnapshot> candlesByOpenTime = new LinkedHashMap<>();
        candles.forEach(candle -> candlesByOpenTime.put(candle.openTime(), candle));
        List<MarketHistoricalCandleSnapshot> sorted = new ArrayList<>(candlesByOpenTime.values());
        if (sorted.size() <= limit) {
            return sorted;
        }
        return sorted.subList(sorted.size() - limit, sorted.size());
    }

    private MarketMinuteCandleSnapshot toMinuteCandleSnapshot(List<String> rawCandle) {
        if (rawCandle == null || rawCandle.size() < 7) {
            return null;
        }

        Instant openTime = Instant.ofEpochMilli(Long.parseLong(rawCandle.get(0)));
        return new MarketMinuteCandleSnapshot(
                openTime,
                openTime.plus(1, ChronoUnit.MINUTES),
                parseDouble(rawCandle.get(1)),
                parseDouble(rawCandle.get(2)),
                parseDouble(rawCandle.get(3)),
                parseDouble(rawCandle.get(4)),
                parseDouble(rawCandle.get(5)),
                parseDouble(rawCandle.get(6))
        );
    }

    private MarketHistoricalCandleSnapshot toHistoricalCandleSnapshot(
            List<String> rawCandle,
            MarketCandleInterval interval
    ) {
        if (rawCandle == null || rawCandle.size() < 7) {
            return null;
        }

        Instant openTime = Instant.ofEpochMilli(Long.parseLong(rawCandle.get(0)));
        return new MarketHistoricalCandleSnapshot(
                openTime,
                closeTime(openTime, interval),
                parseDouble(rawCandle.get(1)),
                parseDouble(rawCandle.get(2)),
                parseDouble(rawCandle.get(3)),
                parseDouble(rawCandle.get(4)),
                parseDouble(rawCandle.get(5)),
                parseDouble(rawCandle.get(6))
        );
    }

    private Instant closeTime(Instant openTime, MarketCandleInterval interval) {
        return switch (interval) {
            case ONE_MINUTE -> openTime.plus(1, ChronoUnit.MINUTES);
            case THREE_MINUTES -> openTime.plus(3, ChronoUnit.MINUTES);
            case FIVE_MINUTES -> openTime.plus(5, ChronoUnit.MINUTES);
            case FIFTEEN_MINUTES -> openTime.plus(15, ChronoUnit.MINUTES);
            case ONE_HOUR -> openTime.plus(1, ChronoUnit.HOURS);
            case FOUR_HOURS, TWELVE_HOURS, ONE_DAY, ONE_WEEK, ONE_MONTH ->
                    MarketTime.bucketClose(openTime, interval);
        };
    }

    private double parseDouble(String value) {
        return Double.parseDouble(value);
    }

    private String responseCode(BitgetTickerResponse response) {
        return response == null ? null : response.code();
    }

    private String responseMessage(BitgetTickerResponse response) {
        return response == null ? null : response.msg();
    }

    private String responseCode(BitgetCandleResponse response) {
        return response == null ? null : response.code();
    }

    private String responseMessage(BitgetCandleResponse response) {
        return response == null ? null : response.msg();
    }
}
