package coin.coinzzickmock.feature.market.application.realtime;

import coin.coinzzickmock.feature.market.domain.MarketMinuteCandleSnapshot;
import coin.coinzzickmock.providers.connector.MarketDataGateway;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketHistoryPersistenceCoordinator {
    private final MarketDataGateway marketDataGateway;
    private final MarketHistoryRecorder marketHistoryRecorder;
    private final Map<String, Instant> recordedClosedMinuteOpenTimes = new ConcurrentHashMap<>();

    public List<MarketHistoryPersistenceResult> persistClosedMinuteCandles(
            List<String> symbols,
            Instant openTime,
            Instant closeTime
    ) {
        if (symbols == null || symbols.isEmpty() || openTime == null || closeTime == null) {
            return List.of();
        }

        return distinctSymbols(symbols).stream()
                .map(symbol -> persistClosedMinuteCandle(symbol, openTime, closeTime))
                .toList();
    }

    private MarketHistoryPersistenceResult persistClosedMinuteCandle(
            String symbol,
            Instant openTime,
            Instant closeTime
    ) {
        if (openTime.equals(recordedClosedMinuteOpenTimes.get(symbol))) {
            return new MarketHistoryPersistenceResult(symbol, openTime, closeTime, MarketHistoryPersistenceStatus.SAVED);
        }

        List<MarketMinuteCandleSnapshot> minuteCandles;
        try {
            minuteCandles = marketDataGateway.loadMinuteCandles(symbol, openTime, closeTime);
        } catch (Exception exception) {
            log.warn("Failed to load closed market minute candle history. symbol={} openTime={} closeTime={}",
                    symbol, openTime, closeTime, exception);
            return new MarketHistoryPersistenceResult(symbol, openTime, closeTime, MarketHistoryPersistenceStatus.FAILED);
        }

        if (minuteCandles == null || minuteCandles.isEmpty()) {
            log.debug("Closed market minute candle history is not ready yet. symbol={} openTime={} closeTime={}",
                    symbol, openTime, closeTime);
            return new MarketHistoryPersistenceResult(symbol, openTime, closeTime, MarketHistoryPersistenceStatus.EMPTY);
        }

        Map<String, Boolean> saveResults;
        try {
            saveResults = marketHistoryRecorder.recordHistoricalMinuteCandlesBySymbol(Map.of(symbol, minuteCandles));
        } catch (RuntimeException exception) {
            log.warn("Failed to persist closed market minute candle history. symbol={} openTime={} closeTime={}",
                    symbol, openTime, closeTime, exception);
            return new MarketHistoryPersistenceResult(symbol, openTime, closeTime, MarketHistoryPersistenceStatus.FAILED);
        }
        if (!Boolean.TRUE.equals(saveResults.get(symbol))) {
            log.warn("Closed market minute candle history was not persisted. symbol={} openTime={} closeTime={}",
                    symbol, openTime, closeTime);
            return new MarketHistoryPersistenceResult(symbol, openTime, closeTime, MarketHistoryPersistenceStatus.FAILED);
        }

        recordedClosedMinuteOpenTimes.put(symbol, openTime);
        return new MarketHistoryPersistenceResult(symbol, openTime, closeTime, MarketHistoryPersistenceStatus.SAVED);
    }

    private List<String> distinctSymbols(List<String> symbols) {
        LinkedHashMap<String, String> distinctSymbols = new LinkedHashMap<>();
        symbols.stream()
                .filter(symbol -> symbol != null && !symbol.isBlank())
                .forEach(symbol -> distinctSymbols.putIfAbsent(symbol, symbol));
        return distinctSymbols.values().stream().toList();
    }
}
