package coin.coinzzickmock.feature.market.application.history;

import coin.coinzzickmock.feature.market.application.result.MarketCandleResult;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MarketHistoricalCandleCache {
    private final MarketHistoricalCandleSegmentPolicy segmentPolicy;
    private final MarketHistoricalCandleSegmentStore segmentStore;
    private final MarketHistoricalCandleSegmentFetcher segmentFetcher;

    public MarketHistoricalCandleCache(
            MarketHistoricalCandleSegmentPolicy segmentPolicy,
            MarketHistoricalCandleSegmentStore segmentStore,
            MarketHistoricalCandleSegmentFetcher segmentFetcher
    ) {
        this.segmentPolicy = segmentPolicy;
        this.segmentStore = segmentStore;
        this.segmentFetcher = segmentFetcher;
    }

    public List<MarketCandleResult> loadOlderCandles(
            String symbol,
            MarketCandleInterval interval,
            Instant toExclusive,
            int limit
    ) {
        if (limit <= 0 || toExclusive == null) {
            return List.of();
        }

        List<MarketCandleResult> candles = new ArrayList<>();
        Instant cursor = toExclusive;
        boolean populatedMiss = false;

        while (candles.size() < limit && !populatedMiss) {
            MarketHistoricalCandleSegment segment = segmentPolicy.segmentContainingPreviousCandle(
                    symbol,
                    interval,
                    cursor
            );
            List<MarketCandleResult> segmentCandles = segmentStore.read(segment);

            if (segmentCandles == null) {
                populatedMiss = true;
                segmentCandles = segmentFetcher.fetch(segment);
                if (isClosedSegment(segment)) {
                    segmentStore.write(segment, segmentCandles);
                }
            }

            Instant currentCursor = cursor;
            candles.addAll(segmentCandles.stream()
                    .filter(candle -> candle.openTime().isBefore(currentCursor))
                    .toList());
            cursor = segment.startInclusive();
        }

        return newest(limit, candles);
    }

    private List<MarketCandleResult> newest(int limit, List<MarketCandleResult> candles) {
        Map<Instant, MarketCandleResult> deduped = new LinkedHashMap<>();
        candles.stream()
                .sorted(Comparator.comparing(MarketCandleResult::openTime))
                .forEach(candle -> deduped.put(candle.openTime(), candle));

        List<MarketCandleResult> sorted = new ArrayList<>(deduped.values());
        if (sorted.size() <= limit) {
            return sorted;
        }
        return sorted.subList(sorted.size() - limit, sorted.size());
    }

    private boolean isClosedSegment(MarketHistoricalCandleSegment segment) {
        return !segment.endExclusive().isAfter(currentClosedBoundary(segment.interval()));
    }

    private Instant currentClosedBoundary(MarketCandleInterval interval) {
        Instant now = Instant.now();
        return switch (interval) {
            case ONE_MINUTE -> MarketTime.alignToMinuteBucket(now, 1);
            case THREE_MINUTES -> MarketTime.alignToMinuteBucket(now, 3);
            case FIVE_MINUTES -> MarketTime.alignToMinuteBucket(now, 5);
            case FIFTEEN_MINUTES -> MarketTime.alignToMinuteBucket(now, 15);
            case ONE_HOUR -> MarketTime.truncate(now, ChronoUnit.HOURS);
            case FOUR_HOURS, TWELVE_HOURS, ONE_DAY, ONE_WEEK, ONE_MONTH ->
                    MarketTime.bucketStart(now, interval);
        };
    }
}
