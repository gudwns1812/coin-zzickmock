package coin.coinzzickmock.feature.market.application.realtime;

import coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import coin.coinzzickmock.feature.market.domain.MarketMinuteCandleSnapshot;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketHistoryRecorder {
    private static final ZoneOffset HISTORY_ZONE = ZoneOffset.UTC;

    private final MarketHistoryRepository marketHistoryRepository;

    public void recordSnapshots(List<MarketSummaryResult> snapshots, Instant observedAt) {
        if (snapshots == null || snapshots.isEmpty()) {
            return;
        }

        Map<String, Long> symbolIds = marketHistoryRepository.findSymbolIdsBySymbols(
                snapshots.stream().map(MarketSummaryResult::symbol).distinct().toList()
        );

        snapshots.forEach(snapshot -> recordSnapshot(symbolIds.get(snapshot.symbol()), snapshot, observedAt));
    }

    public void recordHistoricalMinuteCandles(long symbolId, List<MarketMinuteCandleSnapshot> minuteCandles) {
        if (minuteCandles == null || minuteCandles.isEmpty()) {
            return;
        }

        List<MarketHistoryCandle> historicalCandles = minuteCandles.stream()
                .sorted(Comparator.comparing(MarketMinuteCandleSnapshot::openTime))
                .map(candle -> candle.toHistoryCandle(symbolId))
                .toList();

        historicalCandles.forEach(marketHistoryRepository::saveMinuteCandle);
        historicalCandles.stream()
                .map(MarketHistoryCandle::openTime)
                .map(openTime -> truncate(openTime, ChronoUnit.HOURS))
                .distinct()
                .forEach(hourlyOpenTime -> rebuildHourlyCandle(symbolId, hourlyOpenTime));
    }

    void recordSnapshot(Long symbolId, MarketSummaryResult snapshot, Instant observedAt) {
        if (symbolId == null) {
            return;
        }

        MinuteCandleRevision minuteRevision = recordMinuteCandle(symbolId, snapshot.lastPrice(), observedAt);
        recordHourlyCandle(symbolId, observedAt, minuteRevision);
    }

    private MinuteCandleRevision recordMinuteCandle(long symbolId, double lastPrice, Instant observedAt) {
        Instant minuteOpenTime = truncate(observedAt, ChronoUnit.MINUTES);
        Instant minuteCloseTime = minuteOpenTime.plus(1, ChronoUnit.MINUTES);

        MarketHistoryCandle existingMinuteCandle = marketHistoryRepository
                .findMinuteCandle(symbolId, minuteOpenTime)
                .orElse(null);

        MarketHistoryCandle currentMinuteCandle = existingMinuteCandle == null
                ? MarketHistoryCandle.first(symbolId, minuteOpenTime, minuteCloseTime, lastPrice)
                : existingMinuteCandle.mergeLatestPrice(lastPrice);

        marketHistoryRepository.saveMinuteCandle(currentMinuteCandle);
        return new MinuteCandleRevision(existingMinuteCandle, currentMinuteCandle);
    }

    private void recordHourlyCandle(long symbolId, Instant observedAt, MinuteCandleRevision minuteRevision) {
        Instant hourlyOpenTime = truncate(observedAt, ChronoUnit.HOURS);
        Instant hourlyCloseTime = hourlyOpenTime.plus(1, ChronoUnit.HOURS);
        MarketHistoryCandle currentMinuteCandle = minuteRevision.current();

        HourlyMarketCandle hourlyCandle = marketHistoryRepository.findHourlyCandle(symbolId, hourlyOpenTime)
                .map(existingHourlyCandle -> existingHourlyCandle.reviseMinute(minuteRevision.previous(), currentMinuteCandle))
                .orElseGet(() -> buildHourlyCandle(symbolId, hourlyOpenTime, hourlyCloseTime, currentMinuteCandle));

        if (hourlyCandle == null) {
            return;
        }

        marketHistoryRepository.saveHourlyCandle(hourlyCandle);
    }

    private HourlyMarketCandle buildHourlyCandle(
            long symbolId,
            Instant hourlyOpenTime,
            Instant hourlyCloseTime,
            MarketHistoryCandle currentMinuteCandle
    ) {
        List<MarketHistoryCandle> hourlyCandles = marketHistoryRepository.findMinuteCandles(
                symbolId,
                hourlyOpenTime,
                hourlyCloseTime
        );

        if (hourlyCandles.isEmpty()) {
            return HourlyMarketCandle.first(symbolId, hourlyOpenTime, hourlyCloseTime, currentMinuteCandle);
        }

        return HourlyMarketCandle.rollup(symbolId, hourlyOpenTime, hourlyCloseTime, hourlyCandles);
    }

    private void rebuildHourlyCandle(long symbolId, Instant hourlyOpenTime) {
        Instant hourlyCloseTime = hourlyOpenTime.plus(1, ChronoUnit.HOURS);
        List<MarketHistoryCandle> hourlyCandles = marketHistoryRepository.findMinuteCandles(
                symbolId,
                hourlyOpenTime,
                hourlyCloseTime
        );
        if (hourlyCandles.isEmpty()) {
            return;
        }

        marketHistoryRepository.saveHourlyCandle(
                HourlyMarketCandle.rollup(symbolId, hourlyOpenTime, hourlyCloseTime, hourlyCandles)
        );
    }

    private Instant truncate(Instant instant, ChronoUnit unit) {
        return ZonedDateTime.ofInstant(instant, HISTORY_ZONE)
                .truncatedTo(unit)
                .toInstant();
    }

    private record MinuteCandleRevision(
            MarketHistoryCandle previous,
            MarketHistoryCandle current
    ) {
    }
}
