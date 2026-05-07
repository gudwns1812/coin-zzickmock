package coin.coinzzickmock.feature.market.application.realtime;

import coin.coinzzickmock.feature.market.application.repair.MarketClosedMinuteCandlePersistence;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketHistoryPersistenceCoordinator {
    private final MarketClosedMinuteCandlePersistence marketClosedMinuteCandlePersistence;
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

        MarketHistoryPersistenceResult result = marketClosedMinuteCandlePersistence.persist(symbol, openTime, closeTime);
        if (result.persisted()) {
            recordedClosedMinuteOpenTimes.put(symbol, openTime);
        }
        return result;
    }

    private boolean isAlreadyRecorded(String symbol, Instant openTime) {
        return openTime.equals(recordedClosedMinuteOpenTimes.get(symbol));
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
}
