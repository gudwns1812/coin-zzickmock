package coin.coinzzickmock.feature.market.application.query;

import coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FinalizedCandleIntervalsReader {
    private static final List<MarketCandleInterval> ALWAYS_VISIBLE_INTERVALS = List.of(
            MarketCandleInterval.ONE_MINUTE,
            MarketCandleInterval.THREE_MINUTES,
            MarketCandleInterval.FIVE_MINUTES,
            MarketCandleInterval.FIFTEEN_MINUTES
    );
    private static final List<MarketCandleInterval> HOURLY_DERIVED_INTERVALS = List.of(
            MarketCandleInterval.ONE_HOUR,
            MarketCandleInterval.FOUR_HOURS,
            MarketCandleInterval.TWELVE_HOURS,
            MarketCandleInterval.ONE_DAY,
            MarketCandleInterval.ONE_WEEK,
            MarketCandleInterval.ONE_MONTH
    );

    private final MarketHistoryRepository marketHistoryRepository;

    public List<MarketCandleInterval> readAffectedIntervals(String symbol, Instant openTime, Instant closeTime) {
        List<MarketCandleInterval> affectedIntervals = new ArrayList<>(ALWAYS_VISIBLE_INTERVALS);
        Long symbolId = symbolId(symbol, openTime, closeTime);
        if (symbolId == null) {
            return List.copyOf(affectedIntervals);
        }

        Map<MarketCandleInterval, BucketRange> bucketRanges = bucketRanges(openTime);
        List<HourlyMarketCandle> completedHourlyCandles = completedHourlyCandles(symbolId, bucketRanges);
        for (MarketCandleInterval interval : HOURLY_DERIVED_INTERVALS) {
            if (isCompletedBucketVisible(completedHourlyCandles, bucketRanges.get(interval))) {
                affectedIntervals.add(interval);
            }
        }
        return List.copyOf(affectedIntervals);
    }

    private Long symbolId(String symbol, Instant openTime, Instant closeTime) {
        Map<String, Long> symbolIds = marketHistoryRepository.findSymbolIdsBySymbols(List.of(symbol));
        Long symbolId = symbolIds == null ? null : symbolIds.get(symbol);
        if (symbolId == null) {
            log.warn("Symbol not found during market history finalization. symbol={} openTime={} closeTime={}",
                    symbol, openTime, closeTime);
        }
        return symbolId;
    }

    private Map<MarketCandleInterval, BucketRange> bucketRanges(Instant eventOpenTime) {
        Map<MarketCandleInterval, BucketRange> bucketRanges = new LinkedHashMap<>();
        for (MarketCandleInterval interval : HOURLY_DERIVED_INTERVALS) {
            Instant bucketOpenTime = bucketOpenTime(eventOpenTime, interval);
            Instant bucketCloseTime = bucketCloseTime(bucketOpenTime, interval);
            bucketRanges.put(interval, new BucketRange(
                    bucketOpenTime,
                    bucketCloseTime,
                    expectedHourlyCandles(bucketOpenTime, bucketCloseTime)
            ));
        }
        return bucketRanges;
    }

    private List<HourlyMarketCandle> completedHourlyCandles(
            long symbolId,
            Map<MarketCandleInterval, BucketRange> bucketRanges
    ) {
        Instant fromInclusive = bucketRanges.values().stream()
                .map(BucketRange::openTime)
                .min(Instant::compareTo)
                .orElseThrow();
        Instant toExclusive = bucketRanges.values().stream()
                .map(BucketRange::closeTime)
                .max(Instant::compareTo)
                .orElseThrow();
        List<HourlyMarketCandle> completedHourlyCandles = marketHistoryRepository.findCompletedHourlyCandles(
                symbolId,
                fromInclusive,
                toExclusive
        );
        return completedHourlyCandles == null ? List.of() : completedHourlyCandles;
    }

    private boolean isCompletedBucketVisible(List<HourlyMarketCandle> completedHourlyCandles, BucketRange bucketRange) {
        long completedCount = completedHourlyCandles.stream()
                .filter(Objects::nonNull)
                .filter(candle -> candle.openTime() != null)
                .filter(candle -> !candle.openTime().isBefore(bucketRange.openTime()))
                .filter(candle -> candle.openTime().isBefore(bucketRange.closeTime()))
                .count();
        return completedCount >= bucketRange.expectedHourlyCandles();
    }

    private Instant bucketOpenTime(Instant eventOpenTime, MarketCandleInterval interval) {
        if (interval == MarketCandleInterval.ONE_HOUR) {
            return MarketTime.truncate(eventOpenTime, ChronoUnit.HOURS);
        }
        return MarketTime.bucketStart(eventOpenTime, interval);
    }

    private Instant bucketCloseTime(Instant bucketOpenTime, MarketCandleInterval interval) {
        if (interval == MarketCandleInterval.ONE_HOUR) {
            return bucketOpenTime.plus(1, ChronoUnit.HOURS);
        }
        return MarketTime.bucketClose(bucketOpenTime, interval);
    }

    private int expectedHourlyCandles(Instant bucketOpenTime, Instant bucketCloseTime) {
        return (int) ChronoUnit.HOURS.between(bucketOpenTime, bucketCloseTime);
    }

    private record BucketRange(Instant openTime, Instant closeTime, int expectedHourlyCandles) {
    }
}
