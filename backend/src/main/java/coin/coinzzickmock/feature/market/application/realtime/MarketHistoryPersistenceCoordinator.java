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
        if (isAlreadyRecorded(symbol, openTime)) {
            return toPersistenceResult(symbol, openTime, closeTime, MarketHistoryPersistenceStatus.ALREADY_RECORDED);
        }

        LoadedMinuteCandles loadedMinuteCandles = loadClosedMinuteCandlesSafely(symbol, openTime, closeTime);
        if (loadedMinuteCandles.failed()) {
            return toPersistenceResult(symbol, openTime, closeTime, MarketHistoryPersistenceStatus.FAILED);
        }
        List<MarketMinuteCandleSnapshot> minuteCandles = loadedMinuteCandles.minuteCandles();
        if (minuteCandles.isEmpty()) {
            log.debug("Closed market minute candle history is not ready yet. symbol={} openTime={} closeTime={}",
                    symbol, openTime, closeTime);
            return toPersistenceResult(symbol, openTime, closeTime, MarketHistoryPersistenceStatus.EMPTY);
        }

        if (!recordClosedMinuteCandles(symbol, minuteCandles, openTime, closeTime)) {
            log.warn("Closed market minute candle history was not persisted. symbol={} openTime={} closeTime={}",
                    symbol, openTime, closeTime);
            return toPersistenceResult(symbol, openTime, closeTime, MarketHistoryPersistenceStatus.FAILED);
        }

        recordedClosedMinuteOpenTimes.put(symbol, openTime);
        return toPersistenceResult(symbol, openTime, closeTime, MarketHistoryPersistenceStatus.PERSISTED);
    }

    private boolean isAlreadyRecorded(String symbol, Instant openTime) {
        return openTime.equals(recordedClosedMinuteOpenTimes.get(symbol));
    }

    private LoadedMinuteCandles loadClosedMinuteCandlesSafely(
            String symbol,
            Instant openTime,
            Instant closeTime
    ) {
        try {
            List<MarketMinuteCandleSnapshot> minuteCandles = marketDataGateway.loadMinuteCandles(symbol, openTime, closeTime);
            return new LoadedMinuteCandles(
                    minuteCandles == null ? List.of() : minuteCandles,
                    false
            );
        } catch (Exception exception) {
            log.warn("Failed to load closed market minute candle history. symbol={} openTime={} closeTime={}",
                    symbol, openTime, closeTime, exception);
            return new LoadedMinuteCandles(List.of(), true);
        }
    }

    private boolean recordClosedMinuteCandles(
            String symbol,
            List<MarketMinuteCandleSnapshot> minuteCandles,
            Instant openTime,
            Instant closeTime
    ) {
        try {
            Map<String, Boolean> saveResults = marketHistoryRecorder.recordClosedMinuteCandlesBySymbol(
                    Map.of(symbol, minuteCandles),
                    openTime,
                    closeTime
            );
            return Boolean.TRUE.equals(saveResults.get(symbol));
        } catch (RuntimeException exception) {
            log.warn("Failed to persist closed market minute candle history. symbol={} openTime={} closeTime={}",
                    symbol, openTime, closeTime, exception);
            return false;
        }
    }

    private MarketHistoryPersistenceResult toPersistenceResult(
            String symbol,
            Instant openTime,
            Instant closeTime,
            MarketHistoryPersistenceStatus status
    ) {
        return new MarketHistoryPersistenceResult(symbol, openTime, closeTime, status);
    }

    private List<String> distinctSymbols(List<String> symbols) {
        LinkedHashMap<String, String> distinctSymbols = new LinkedHashMap<>();
        symbols.stream()
                .filter(symbol -> symbol != null && !symbol.isBlank())
                .forEach(symbol -> distinctSymbols.putIfAbsent(symbol, symbol));
        return distinctSymbols.values().stream().toList();
    }

    private record LoadedMinuteCandles(List<MarketMinuteCandleSnapshot> minuteCandles, boolean failed) {
    }
}
