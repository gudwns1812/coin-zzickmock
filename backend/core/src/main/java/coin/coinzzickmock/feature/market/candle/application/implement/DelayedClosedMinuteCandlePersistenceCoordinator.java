package coin.coinzzickmock.feature.market.candle.application.implement;

import coin.coinzzickmock.feature.market.history.application.service.MarketHistoryPersistenceCoordinator;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DelayedClosedMinuteCandlePersistenceCoordinator {
    private final MarketHistoryPersistenceCoordinator marketHistoryPersistenceCoordinator;
    private final Set<ClosedMinutePersistenceKey> inFlightClosedMinutes = ConcurrentHashMap.newKeySet();

    public Optional<ClosedMinutePersistenceTask> claimClosedMinutePersistence(
            List<String> symbols,
            Instant openTime,
            Instant closeTime
    ) {
        if (symbols == null || symbols.isEmpty() || openTime == null || closeTime == null) {
            return Optional.empty();
        }

        List<ClosedMinutePersistenceKey> acceptedKeys = claimInFlightKeys(symbols, openTime);
        if (acceptedKeys.isEmpty()) {
            return Optional.empty();
        }

        List<String> acceptedSymbols = acceptedKeys.stream()
                .map(ClosedMinutePersistenceKey::symbol)
                .toList();
        return Optional.of(new ClosedMinutePersistenceTask(acceptedSymbols, openTime, closeTime, acceptedKeys));
    }

    private List<ClosedMinutePersistenceKey> claimInFlightKeys(List<String> symbols, Instant openTime) {
        return distinctKeys(symbols, openTime).stream()
                .filter(inFlightClosedMinutes::add)
                .toList();
    }

    private List<ClosedMinutePersistenceKey> distinctKeys(List<String> symbols, Instant openTime) {
        LinkedHashMap<ClosedMinutePersistenceKey, ClosedMinutePersistenceKey> distinctKeys = new LinkedHashMap<>();
        symbols.stream()
                .filter(symbol -> symbol != null && !symbol.isBlank())
                .map(symbol -> new ClosedMinutePersistenceKey(symbol, openTime))
                .forEach(key -> distinctKeys.putIfAbsent(key, key));
        return distinctKeys.values().stream().toList();
    }

    private void releaseInFlightKeys(List<ClosedMinutePersistenceKey> acceptedKeys) {
        acceptedKeys.forEach(inFlightClosedMinutes::remove);
    }

    public final class ClosedMinutePersistenceTask {
        private final List<String> symbols;
        private final Instant openTime;
        private final Instant closeTime;
        private final List<ClosedMinutePersistenceKey> acceptedKeys;

        private ClosedMinutePersistenceTask(
                List<String> symbols,
                Instant openTime,
                Instant closeTime,
                List<ClosedMinutePersistenceKey> acceptedKeys
        ) {
            this.symbols = symbols;
            this.openTime = openTime;
            this.closeTime = closeTime;
            this.acceptedKeys = acceptedKeys;
        }

        public void persistAndRelease() {
            try {
                marketHistoryPersistenceCoordinator.persistClosedMinuteCandles(symbols, openTime, closeTime);
            } finally {
                release();
            }
        }

        public void release() {
            releaseInFlightKeys(acceptedKeys);
        }
    }

    private record ClosedMinutePersistenceKey(String symbol, Instant openTime) {
    }
}
