package coin.coinzzickmock.feature.market.application.realtime;

import coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MarketHistoryRecorder {
    private static final ZoneOffset HISTORY_ZONE = ZoneOffset.UTC;

    private final MarketHistoryRepository marketHistoryRepository;

    public MarketHistoryRecorder(MarketHistoryRepository marketHistoryRepository) {
        this.marketHistoryRepository = marketHistoryRepository;
    }

    @Transactional
    public void recordSnapshots(List<MarketSummaryResult> snapshots, Instant observedAt) {
        if (snapshots == null || snapshots.isEmpty()) {
            return;
        }

        Map<String, Long> symbolIds = marketHistoryRepository.findSymbolIdsBySymbols(
                snapshots.stream().map(MarketSummaryResult::symbol).distinct().toList()
        );

        snapshots.forEach(snapshot -> recordSnapshot(symbolIds.get(snapshot.symbol()), snapshot, observedAt));
    }

    void recordSnapshot(Long symbolId, MarketSummaryResult snapshot, Instant observedAt) {
        if (symbolId == null) {
            return;
        }

        Instant minuteOpenTime = truncate(observedAt, ChronoUnit.MINUTES);
        Instant minuteCloseTime = minuteOpenTime.plus(1, ChronoUnit.MINUTES);
        double lastPrice = snapshot.lastPrice();

        MarketHistoryCandle currentMinuteCandle = marketHistoryRepository
                .findMinuteCandles(symbolId, minuteOpenTime, minuteCloseTime)
                .stream()
                .findFirst()
                .map(existing -> mergeMinute(existing, lastPrice))
                .orElseGet(() -> firstMinute(symbolId, minuteOpenTime, minuteCloseTime, lastPrice));

        marketHistoryRepository.saveMinuteCandle(currentMinuteCandle);

        Instant hourlyOpenTime = truncate(observedAt, ChronoUnit.HOURS);
        Instant hourlyCloseTime = hourlyOpenTime.plus(1, ChronoUnit.HOURS);
        List<MarketHistoryCandle> hourlyCandles = marketHistoryRepository.findMinuteCandles(
                symbolId,
                hourlyOpenTime,
                hourlyCloseTime
        );

        if (hourlyCandles.isEmpty()) {
            return;
        }

        marketHistoryRepository.saveHourlyCandle(rollupHourly(symbolId, hourlyOpenTime, hourlyCloseTime, hourlyCandles));
    }

    private MarketHistoryCandle firstMinute(long symbolId, Instant openTime, Instant closeTime, double price) {
        return new MarketHistoryCandle(
                symbolId,
                openTime,
                closeTime,
                price,
                price,
                price,
                price,
                0.0,
                0.0,
                0
        );
    }

    private MarketHistoryCandle mergeMinute(MarketHistoryCandle existing, double latestPrice) {
        return new MarketHistoryCandle(
                existing.symbolId(),
                existing.openTime(),
                existing.closeTime(),
                existing.openPrice(),
                Math.max(existing.highPrice(), latestPrice),
                Math.min(existing.lowPrice(), latestPrice),
                latestPrice,
                existing.volume(),
                existing.quoteVolume(),
                existing.tradeCount()
        );
    }

    private HourlyMarketCandle rollupHourly(
            long symbolId,
            Instant hourlyOpenTime,
            Instant hourlyCloseTime,
            List<MarketHistoryCandle> hourlyCandles
    ) {
        List<MarketHistoryCandle> sortedCandles = hourlyCandles.stream()
                .sorted(Comparator.comparing(MarketHistoryCandle::openTime))
                .toList();
        MarketHistoryCandle first = sortedCandles.get(0);
        MarketHistoryCandle last = sortedCandles.get(sortedCandles.size() - 1);

        double highPrice = sortedCandles.stream()
                .mapToDouble(MarketHistoryCandle::highPrice)
                .max()
                .orElse(first.highPrice());
        double lowPrice = sortedCandles.stream()
                .mapToDouble(MarketHistoryCandle::lowPrice)
                .min()
                .orElse(first.lowPrice());
        double volume = sortedCandles.stream()
                .mapToDouble(MarketHistoryCandle::volume)
                .sum();
        double quoteVolume = sortedCandles.stream()
                .mapToDouble(MarketHistoryCandle::quoteVolume)
                .sum();
        int tradeCount = sortedCandles.stream()
                .mapToInt(MarketHistoryCandle::tradeCount)
                .sum();

        return new HourlyMarketCandle(
                symbolId,
                hourlyOpenTime,
                hourlyCloseTime,
                first.openPrice(),
                highPrice,
                lowPrice,
                last.closePrice(),
                volume,
                quoteVolume,
                tradeCount,
                first.openTime(),
                last.closeTime()
        );
    }

    private Instant truncate(Instant instant, ChronoUnit unit) {
        return ZonedDateTime.ofInstant(instant, HISTORY_ZONE)
                .truncatedTo(unit)
                .toInstant();
    }
}
