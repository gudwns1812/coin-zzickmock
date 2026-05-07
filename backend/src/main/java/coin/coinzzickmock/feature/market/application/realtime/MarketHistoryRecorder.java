package coin.coinzzickmock.feature.market.application.realtime;

import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.application.repair.MarketHistoryRepairRequestRecorder;
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
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class MarketHistoryRecorder {
    private final MarketHistoryRepository marketHistoryRepository;
    private final CompletedHourlyCandleBuilder completedHourlyCandleBuilder;
    private final AfterCommitEventPublisher afterCommitEventPublisher;
    private final MarketHistoryRepairRequestRecorder marketHistoryRepairRequestRecorder;

    @Autowired
    public MarketHistoryRecorder(
            MarketHistoryRepository marketHistoryRepository,
            CompletedHourlyCandleBuilder completedHourlyCandleBuilder,
            AfterCommitEventPublisher afterCommitEventPublisher,
            @Nullable MarketHistoryRepairRequestRecorder marketHistoryRepairRequestRecorder
    ) {
        this.marketHistoryRepository = marketHistoryRepository;
        this.completedHourlyCandleBuilder = completedHourlyCandleBuilder;
        this.afterCommitEventPublisher = afterCommitEventPublisher;
        this.marketHistoryRepairRequestRecorder = marketHistoryRepairRequestRecorder;
    }

    /**
     * Test convenience constructor. CompletedHourlyCandleBuilder is stateless and has no Spring-managed dependencies.
     */
    MarketHistoryRecorder(MarketHistoryRepository marketHistoryRepository) {
        this(marketHistoryRepository, new CompletedHourlyCandleBuilder(), null, null);
    }

    @Transactional
    public Map<String, Boolean> recordHistoricalMinuteCandlesBySymbol(
            Map<String, List<MarketMinuteCandleSnapshot>> minuteCandlesBySymbol
    ) {
        if (minuteCandlesBySymbol == null || minuteCandlesBySymbol.isEmpty()) {
            return Map.of();
        }

        Map<String, List<MarketMinuteCandleSnapshot>> nonEmptyMinuteCandlesBySymbol =
                nonEmptyMinuteCandlesBySymbol(minuteCandlesBySymbol);
        if (nonEmptyMinuteCandlesBySymbol.isEmpty()) {
            return Map.of();
        }

        Map<String, Long> symbolIds = loadSymbolIds(nonEmptyMinuteCandlesBySymbol);
        return recordHistoricalMinuteCandlesByKnownSymbols(nonEmptyMinuteCandlesBySymbol, symbolIds);
    }

    private Map<String, List<MarketMinuteCandleSnapshot>> nonEmptyMinuteCandlesBySymbol(
            Map<String, List<MarketMinuteCandleSnapshot>> minuteCandlesBySymbol
    ) {
        Map<String, List<MarketMinuteCandleSnapshot>> nonEmptyMinuteCandlesBySymbol = new LinkedHashMap<>();
        minuteCandlesBySymbol.forEach((symbol, minuteCandles) -> {
            if (symbol != null && minuteCandles != null && !minuteCandles.isEmpty()) {
                nonEmptyMinuteCandlesBySymbol.put(symbol, minuteCandles);
            }
        });
        return nonEmptyMinuteCandlesBySymbol;
    }

    private Map<String, Long> loadSymbolIds(
            Map<String, List<MarketMinuteCandleSnapshot>> nonEmptyMinuteCandlesBySymbol
    ) {
        return marketHistoryRepository.findSymbolIdsBySymbols(
                nonEmptyMinuteCandlesBySymbol.keySet().stream().toList()
        );
    }

    private Map<String, Boolean> recordHistoricalMinuteCandlesByKnownSymbols(
            Map<String, List<MarketMinuteCandleSnapshot>> nonEmptyMinuteCandlesBySymbol,
            Map<String, Long> symbolIds
    ) {
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
        publishFinalizedEvents(saveResults, openTime, closeTime);
        return saveResults;
    }

    private void publishFinalizedEvents(
            Map<String, Boolean> saveResults,
            Instant openTime,
            Instant closeTime
    ) {
        if (afterCommitEventPublisher == null) {
            return;
        }
        saveResults.forEach((symbol, wasSaved) -> {
            if (Boolean.TRUE.equals(wasSaved)) {
                afterCommitEventPublisher.publish(new MarketHistoryFinalizedEvent(symbol, openTime, closeTime));
            }
        });
    }

    @Transactional
    public void recordHistoricalMinuteCandles(long symbolId, List<MarketMinuteCandleSnapshot> minuteCandles) {
        if (minuteCandles == null || minuteCandles.isEmpty()) {
            return;
        }

        List<MarketHistoryCandle> historicalCandles = toSortedHistoryCandles(symbolId, minuteCandles);
        historicalCandles.forEach(marketHistoryRepository::saveMinuteCandle);
        rebuildAffectedHourlyCandles(symbolId, historicalCandles);
    }

    private List<MarketHistoryCandle> toSortedHistoryCandles(
            long symbolId,
            List<MarketMinuteCandleSnapshot> minuteCandles
    ) {
        return minuteCandles.stream()
                .sorted(Comparator.comparing(MarketMinuteCandleSnapshot::openTime))
                .map(candle -> candle.toHistoryCandle(symbolId))
                .toList();
    }

    private void rebuildAffectedHourlyCandles(long symbolId, List<MarketHistoryCandle> historicalCandles) {
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
        Optional<HourlyMarketCandle> completedHourlyCandle =
                completedHourlyCandleBuilder.build(symbolId, hourlyOpenTime, hourlyCloseTime, hourlyCandles);
        if (completedHourlyCandle.isPresent()) {
            try {
                marketHistoryRepository.saveHourlyCandle(completedHourlyCandle.orElseThrow());
            } catch (RuntimeException exception) {
                recordHourlyRepairRequest(symbolId, hourlyOpenTime, hourlyCloseTime, exception);
            }
            return;
        }

        reportSkippedHourlyRebuild(symbolId, hourlyOpenTime, hourlyCandles);
    }

    @Transactional
    public boolean rebuildCompletedHourlyCandle(long symbolId, Instant hourlyOpenTime) {
        Instant hourlyCloseTime = hourlyOpenTime.plus(1, ChronoUnit.HOURS);
        List<MarketHistoryCandle> hourlyCandles = marketHistoryRepository.findMinuteCandles(
                symbolId,
                hourlyOpenTime,
                hourlyCloseTime
        );
        Optional<HourlyMarketCandle> completedHourlyCandle =
                completedHourlyCandleBuilder.build(symbolId, hourlyOpenTime, hourlyCloseTime, hourlyCandles);
        if (completedHourlyCandle.isEmpty()) {
            return false;
        }
        marketHistoryRepository.saveHourlyCandle(completedHourlyCandle.orElseThrow());
        return true;
    }

    private void recordHourlyRepairRequest(
            long symbolId,
            Instant hourlyOpenTime,
            Instant hourlyCloseTime,
            RuntimeException exception
    ) {
        if (marketHistoryRepairRequestRecorder == null) {
            throw exception;
        }
        String symbol = marketHistoryRepository.findSymbolById(symbolId).orElse(null);
        if (symbol == null) {
            throw exception;
        }
        marketHistoryRepairRequestRecorder.recordOneHourFailureAfterCurrentCommit(
                symbol,
                hourlyOpenTime,
                hourlyCloseTime,
                exception
        );
    }

    private void reportSkippedHourlyRebuild(
            long symbolId,
            Instant hourlyOpenTime,
            List<MarketHistoryCandle> hourlyCandles
    ) {
        if (hourlyCandles.isEmpty()) {
            log.debug("Skip hourly rebuild because source minute coverage is empty. symbolId={} openTime={}",
                    symbolId, hourlyOpenTime);
            return;
        }

        /*
         * Persisted hourly candles are authoritative for completed-hour reads.
         * Partial minute coverage must not repair, overwrite, or delete an existing hourly row.
         */
        log.warn(
                "Incomplete source minute coverage; leaving persisted hourly candle unchanged. "
                        + "symbolId={} openTime={} sourceCount={}",
                symbolId, hourlyOpenTime, hourlyCandles.size());
    }

    private Instant truncate(Instant instant, ChronoUnit unit) {
        return MarketTime.truncate(instant, unit);
    }
}
