package coin.coinzzickmock.feature.market.application.realtime;

import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import coin.coinzzickmock.feature.market.domain.MarketMinuteCandleSnapshot;
import coin.coinzzickmock.feature.market.domain.MarketTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MarketHistoryRecorder {
    private final MarketHistoryRepository marketHistoryRepository;
    private final AfterCommitEventPublisher afterCommitEventPublisher;

    @Autowired
    public MarketHistoryRecorder(
            MarketHistoryRepository marketHistoryRepository,
            AfterCommitEventPublisher afterCommitEventPublisher
    ) {
        this.marketHistoryRepository = marketHistoryRepository;
        this.afterCommitEventPublisher = afterCommitEventPublisher;
    }

    public MarketHistoryRecorder(MarketHistoryRepository marketHistoryRepository) {
        this(marketHistoryRepository, null);
    }

    @Transactional
    public Map<String, Boolean> recordHistoricalMinuteCandlesBySymbol(
            Map<String, List<MarketMinuteCandleSnapshot>> minuteCandlesBySymbol
    ) {
        if (minuteCandlesBySymbol == null || minuteCandlesBySymbol.isEmpty()) {
            return Map.of();
        }

        Map<String, List<MarketMinuteCandleSnapshot>> nonEmptyMinuteCandlesBySymbol = new LinkedHashMap<>();
        minuteCandlesBySymbol.forEach((symbol, minuteCandles) -> {
            if (symbol != null && minuteCandles != null && !minuteCandles.isEmpty()) {
                nonEmptyMinuteCandlesBySymbol.put(symbol, minuteCandles);
            }
        });
        if (nonEmptyMinuteCandlesBySymbol.isEmpty()) {
            return Map.of();
        }

        Map<String, Long> symbolIds = marketHistoryRepository.findSymbolIdsBySymbols(
                nonEmptyMinuteCandlesBySymbol.keySet().stream().toList()
        );

        Map<String, Boolean> saveResults = new LinkedHashMap<>();
        nonEmptyMinuteCandlesBySymbol.forEach((symbol, minuteCandles) -> {
            Long symbolId = symbolIds.get(symbol);
            if (symbolId != null) {
                recordHistoricalMinuteCandles(symbolId, minuteCandles);
                saveResults.put(symbol, true);
                return;
            }
            saveResults.put(symbol, false);
        });
        return saveResults;
    }

    @Transactional
    public Map<String, Boolean> recordClosedMinuteCandlesBySymbol(
            Map<String, List<MarketMinuteCandleSnapshot>> minuteCandlesBySymbol,
            Instant openTime,
            Instant closeTime
    ) {
        Map<String, Boolean> saveResults = recordHistoricalMinuteCandlesBySymbol(minuteCandlesBySymbol);
        if (afterCommitEventPublisher == null) {
            return saveResults;
        }

        saveResults.forEach((symbol, saved) -> {
            if (Boolean.TRUE.equals(saved)) {
                afterCommitEventPublisher.publish(new MarketHistoryFinalizedEvent(symbol, openTime, closeTime));
            }
        });
        return saveResults;
    }

    @Transactional
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
        return MarketTime.truncate(instant, unit);
    }
}
