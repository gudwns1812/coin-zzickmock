package coin.coinzzickmock.feature.market.application.history;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.market.application.dto.MarketCandleResult;
import coin.coinzzickmock.feature.market.domain.CompletedMarketCandle;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import coin.coinzzickmock.feature.market.domain.MarketTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MarketCandleRollupProjector {
    public List<MarketCandleResult> minuteResults(List<MarketHistoryCandle> candles) {
        return candles.stream()
                .map(this::toMinuteResult)
                .toList();
    }

    public List<MarketCandleResult> hourlyResults(List<HourlyMarketCandle> candles) {
        return candles.stream()
                .map(this::toHourlyResult)
                .toList();
    }

    public List<MarketCandleResult> rollupMinuteResults(
            List<MarketHistoryCandle> rawCandles,
            int bucketMinutes
    ) {
        Map<Instant, List<MarketHistoryCandle>> grouped = new LinkedHashMap<>();
        for (MarketHistoryCandle candle : rawCandles) {
            grouped.computeIfAbsent(
                            MarketTime.alignToMinuteBucket(candle.openTime(), bucketMinutes),
                            key -> new ArrayList<>()
                    )
                    .add(candle);
        }

        return grouped.entrySet().stream()
                .filter(entry -> isCompleteMinuteBucket(entry.getKey(), bucketMinutes, entry.getValue()))
                .map(entry -> rollupMinuteBucket(entry.getKey(), bucketMinutes, entry.getValue()))
                .toList();
    }

    public List<MarketCandleResult> rollupHourlyResults(
            List<HourlyMarketCandle> rawCandles,
            MarketCandleInterval interval
    ) {
        Map<Instant, List<HourlyMarketCandle>> grouped = new LinkedHashMap<>();
        for (HourlyMarketCandle candle : rawCandles) {
            grouped.computeIfAbsent(
                            MarketTime.bucketStart(candle.openTime(), interval),
                            key -> new ArrayList<>()
                    )
                    .add(candle);
        }

        return grouped.entrySet().stream()
                .filter(entry -> isCompleteHourlyBucket(entry.getKey(), interval, entry.getValue()))
                .map(entry -> rollupHourlyBucket(entry.getKey(), interval, entry.getValue()))
                .toList();
    }

    public List<MarketCandleResult> rollupFixedHourlyResults(
            List<HourlyMarketCandle> rawCandles,
            MarketCandleInterval interval,
            Instant earliestBucketStart,
            Instant latestBucketStart
    ) {
        if (rawCandles == null || rawCandles.isEmpty() || latestBucketStart.isBefore(earliestBucketStart)) {
            return List.of();
        }

        Map<Instant, HourlyMarketCandle> byOpenTime = new LinkedHashMap<>();
        for (HourlyMarketCandle candle : rawCandles) {
            byOpenTime.putIfAbsent(candle.openTime(), candle);
        }

        List<MarketCandleResult> results = new ArrayList<>();
        int expectedHours = expectedHourlyBucketSize(earliestBucketStart, interval);
        Instant bucketStart = earliestBucketStart;
        while (!bucketStart.isAfter(latestBucketStart)) {
            List<HourlyMarketCandle> bucketCandles = new ArrayList<>();
            for (int index = 0; index < expectedHours; index++) {
                HourlyMarketCandle candle = byOpenTime.get(bucketStart.plus(index, ChronoUnit.HOURS));
                if (candle == null) {
                    bucketCandles.clear();
                    break;
                }
                bucketCandles.add(candle);
            }
            if (bucketCandles.size() == expectedHours) {
                results.add(rollupHourlyBucket(bucketStart, interval, bucketCandles));
            }
            bucketStart = bucketStart.plus(expectedHours, ChronoUnit.HOURS);
        }
        return results;
    }

    public List<MarketCandleResult> rollupDailyResults(
            List<CompletedMarketCandle> rawCandles,
            MarketCandleInterval interval
    ) {
        Map<Instant, List<CompletedMarketCandle>> grouped = new LinkedHashMap<>();
        for (CompletedMarketCandle candle : rawCandles) {
            grouped.computeIfAbsent(
                            MarketTime.bucketStart(candle.openTime(), interval),
                            key -> new ArrayList<>()
                    )
                    .add(candle);
        }

        return grouped.entrySet().stream()
                .filter(entry -> isCompleteDailyBucket(entry.getKey(), interval, entry.getValue()))
                .map(entry -> rollupDailyBucket(entry.getKey(), interval, entry.getValue()))
                .toList();
    }

    private boolean isCompleteMinuteBucket(
            Instant bucketStart,
            int bucketMinutes,
            List<MarketHistoryCandle> candles
    ) {
        if (candles.size() != bucketMinutes) {
            return false;
        }

        return expectedOpenTimes(bucketStart, bucketMinutes, ChronoUnit.MINUTES).stream()
                .allMatch(expectedOpenTime -> candles.stream()
                        .anyMatch(candle -> expectedOpenTime.equals(candle.openTime())));
    }

    private boolean isCompleteHourlyBucket(
            Instant bucketStart,
            MarketCandleInterval interval,
            List<HourlyMarketCandle> candles
    ) {
        int expectedHours = expectedHourlyBucketSize(bucketStart, interval);
        if (candles.size() != expectedHours) {
            return false;
        }

        return expectedOpenTimes(bucketStart, expectedHours, ChronoUnit.HOURS).stream()
                .allMatch(expectedOpenTime -> candles.stream()
                        .anyMatch(candle -> expectedOpenTime.equals(candle.openTime())));
    }

    private boolean isCompleteDailyBucket(
            Instant bucketStart,
            MarketCandleInterval interval,
            List<CompletedMarketCandle> candles
    ) {
        int expectedDays = expectedDailyBucketSize(bucketStart, interval);
        if (candles.size() != expectedDays) {
            return false;
        }

        return expectedOpenTimes(bucketStart, expectedDays, ChronoUnit.DAYS).stream()
                .allMatch(expectedOpenTime -> candles.stream()
                        .anyMatch(candle -> expectedOpenTime.equals(candle.openTime())));
    }

    private List<Instant> expectedOpenTimes(Instant bucketStart, int count, ChronoUnit unit) {
        List<Instant> openTimes = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            openTimes.add(bucketStart.plus(index, unit));
        }
        return openTimes;
    }

    private MarketCandleResult rollupMinuteBucket(
            Instant bucketStart,
            int bucketMinutes,
            List<MarketHistoryCandle> candles
    ) {
        MarketHistoryCandle first = candles.get(0);
        MarketHistoryCandle last = candles.get(0);
        double high = first.highPrice();
        double low = first.lowPrice();
        double volume = 0.0;

        for (MarketHistoryCandle candle : candles) {
            if (candle.openTime().isBefore(first.openTime())) {
                first = candle;
            }
            if (candle.openTime().isAfter(last.openTime())) {
                last = candle;
            }
            high = Math.max(high, candle.highPrice());
            low = Math.min(low, candle.lowPrice());
            volume += candle.volume();
        }

        return new MarketCandleResult(
                bucketStart,
                bucketStart.plus(bucketMinutes, ChronoUnit.MINUTES),
                first.openPrice(),
                high,
                low,
                last.closePrice(),
                volume
        );
    }

    private MarketCandleResult rollupHourlyBucket(
            Instant bucketStart,
            MarketCandleInterval interval,
            List<HourlyMarketCandle> candles
    ) {
        HourlyMarketCandle first = candles.get(0);
        HourlyMarketCandle last = candles.get(0);
        double high = first.highPrice();
        double low = first.lowPrice();
        double volume = 0.0;

        for (HourlyMarketCandle candle : candles) {
            if (candle.openTime().isBefore(first.openTime())) {
                first = candle;
            }
            if (candle.openTime().isAfter(last.openTime())) {
                last = candle;
            }
            high = Math.max(high, candle.highPrice());
            low = Math.min(low, candle.lowPrice());
            volume += candle.volume();
        }

        return new MarketCandleResult(
                bucketStart,
                MarketTime.bucketClose(bucketStart, interval),
                first.openPrice(),
                high,
                low,
                last.closePrice(),
                volume
        );
    }

    private MarketCandleResult rollupDailyBucket(
            Instant bucketStart,
            MarketCandleInterval interval,
            List<CompletedMarketCandle> candles
    ) {
        CompletedMarketCandle first = candles.get(0);
        CompletedMarketCandle last = candles.get(0);
        double high = first.highPrice();
        double low = first.lowPrice();
        double volume = 0.0;

        for (CompletedMarketCandle candle : candles) {
            if (candle.openTime().isBefore(first.openTime())) {
                first = candle;
            }
            if (candle.openTime().isAfter(last.openTime())) {
                last = candle;
            }
            high = Math.max(high, candle.highPrice());
            low = Math.min(low, candle.lowPrice());
            volume += candle.volume();
        }

        return new MarketCandleResult(
                bucketStart,
                MarketTime.bucketClose(bucketStart, interval),
                first.openPrice(),
                high,
                low,
                last.closePrice(),
                volume
        );
    }

    private int expectedHourlyBucketSize(Instant bucketStart, MarketCandleInterval interval) {
        return switch (interval) {
            case FOUR_HOURS -> 4;
            case TWELVE_HOURS -> 12;
            case ONE_DAY -> 24;
            case ONE_WEEK, ONE_MONTH -> (int) ChronoUnit.HOURS.between(
                    bucketStart,
                    MarketTime.bucketClose(bucketStart, interval)
            );
            default -> throw new CoreException(ErrorCode.INVALID_REQUEST);
        };
    }

    private int expectedDailyBucketSize(Instant bucketStart, MarketCandleInterval interval) {
        return switch (interval) {
            case ONE_WEEK -> 7;
            default -> throw new CoreException(ErrorCode.INVALID_REQUEST);
        };
    }

    private MarketCandleResult toMinuteResult(MarketHistoryCandle candle) {
        return new MarketCandleResult(
                candle.openTime(),
                candle.closeTime(),
                candle.openPrice(),
                candle.highPrice(),
                candle.lowPrice(),
                candle.closePrice(),
                candle.volume()
        );
    }

    private MarketCandleResult toHourlyResult(HourlyMarketCandle candle) {
        return new MarketCandleResult(
                candle.openTime(),
                candle.closeTime(),
                candle.openPrice(),
                candle.highPrice(),
                candle.lowPrice(),
                candle.closePrice(),
                candle.volume()
        );
    }
}
