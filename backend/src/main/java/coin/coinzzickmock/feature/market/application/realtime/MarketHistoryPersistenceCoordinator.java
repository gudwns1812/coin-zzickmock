package coin.coinzzickmock.feature.market.application.realtime;

import coin.coinzzickmock.feature.market.domain.MarketMinuteCandleSnapshot;
import coin.coinzzickmock.providers.connector.MarketDataGateway;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MarketHistoryPersistenceCoordinator {
    private final MarketDataGateway marketDataGateway;
    private final MarketHistoryRecorder marketHistoryRecorder;
    private final Map<String, Instant> recordedClosedMinuteOpenTimes = new ConcurrentHashMap<>();

    @Transactional
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
        } catch (Exception ignored) {
            return new MarketHistoryPersistenceResult(symbol, openTime, closeTime, MarketHistoryPersistenceStatus.FAILED);
        }

        if (minuteCandles == null || minuteCandles.isEmpty()) {
            return new MarketHistoryPersistenceResult(symbol, openTime, closeTime, MarketHistoryPersistenceStatus.EMPTY);
        }

        Map<String, Boolean> saveResults = marketHistoryRecorder.recordHistoricalMinuteCandlesBySymbol(
                Map.of(symbol, minuteCandles)
        );
        if (!Boolean.TRUE.equals(saveResults.get(symbol))) {
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
