package coin.coinzzickmock.feature.market.application.history;

import coin.coinzzickmock.feature.market.application.implement.CompletedCalendarCandleBuilder;
import coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.application.repository.MarketHistoryStartupBackfillCursor;
import coin.coinzzickmock.feature.market.domain.CompletedMarketCandle;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketTime;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MarketCalendarCandleBackfill {
    private static final List<MarketCandleInterval> CALENDAR_INTERVALS = List.of(
            MarketCandleInterval.ONE_DAY,
            MarketCandleInterval.ONE_MONTH
    );

    private final MarketHistoryRepository marketHistoryRepository;
    private final CompletedCalendarCandleBuilder completedCalendarCandleBuilder;
    private final Clock clock = Clock.systemUTC();

    @Transactional
    public int catchUpPersistedCalendarCandles() {
        int saved = 0;
        for (MarketHistoryStartupBackfillCursor cursor : marketHistoryRepository.findStartupBackfillCursors()) {
            for (MarketCandleInterval interval : CALENDAR_INTERVALS) {
                saved += catchUpSymbolInterval(cursor.symbolId(), interval);
            }
        }
        return saved;
    }

    public void rebuildClosedCalendarCandlesForHour(long symbolId, Instant hourlyOpenTime) {
        for (MarketCandleInterval interval : CALENDAR_INTERVALS) {
            Instant bucketOpenTime = MarketTime.bucketStart(hourlyOpenTime, interval);
            Instant bucketCloseTime = MarketTime.bucketClose(bucketOpenTime, interval);
            if (bucketCloseTime.isAfter(Instant.now(clock))) {
                continue;
            }
            buildAndSave(symbolId, interval, bucketOpenTime, bucketCloseTime);
        }
    }

    private int catchUpSymbolInterval(long symbolId, MarketCandleInterval interval) {
        if (!marketHistoryRepository.existsCompletedCandle(symbolId, interval)) {
            return 0;
        }
        Optional<Instant> latestCalendarOpen = marketHistoryRepository.findLatestCompletedCandleOpenTime(
                symbolId,
                interval
        );
        Optional<Instant> latestHourlyOpen = marketHistoryRepository.findLatestCompletedHourlyCandleOpenTime(symbolId);
        if (latestCalendarOpen.isEmpty() || latestHourlyOpen.isEmpty()) {
            return 0;
        }
        int saved = 0;
        Instant candidateOpen = nextBucketOpen(latestCalendarOpen.orElseThrow(), interval);
        while (true) {
            Instant candidateClose = MarketTime.bucketClose(candidateOpen, interval);
            if (candidateClose.isAfter(Instant.now(clock))
                    || latestHourlyOpen.orElseThrow().plusSeconds(3600).isBefore(candidateClose)) {
                return saved;
            }
            if (buildAndSave(symbolId, interval, candidateOpen, candidateClose)) {
                saved++;
            }
            candidateOpen = candidateClose;
        }
    }

    private boolean buildAndSave(
            long symbolId,
            MarketCandleInterval interval,
            Instant bucketOpenTime,
            Instant bucketCloseTime
    ) {
        List<HourlyMarketCandle> hourlyCandles = marketHistoryRepository.findCompletedHourlyCandles(
                symbolId,
                bucketOpenTime,
                bucketCloseTime
        );
        Optional<CompletedMarketCandle> completedCandle = completedCalendarCandleBuilder.build(
                symbolId,
                interval,
                bucketOpenTime,
                bucketCloseTime,
                hourlyCandles
        );
        completedCandle.ifPresent(marketHistoryRepository::saveCompletedCandle);
        return completedCandle.isPresent();
    }

    private Instant nextBucketOpen(Instant currentBucketOpenTime, MarketCandleInterval interval) {
        ZonedDateTime bucket = MarketTime.atStorageZone(currentBucketOpenTime);
        return switch (interval) {
            case ONE_DAY -> bucket.plusDays(1).toInstant();
            case ONE_MONTH -> bucket.plusMonths(1).toInstant();
            default -> throw new IllegalArgumentException("Unsupported calendar interval: " + interval);
        };
    }
}
