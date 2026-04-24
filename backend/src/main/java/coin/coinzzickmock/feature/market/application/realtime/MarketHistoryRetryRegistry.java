package coin.coinzzickmock.feature.market.application.realtime;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

@Component
public class MarketHistoryRetryRegistry {
    private final ConcurrentMap<PendingMinuteCandleRetry, PendingMinuteCandleRetry> pendingRetries =
            new ConcurrentHashMap<>();

    public void markPending(String symbol, Instant openTime, Instant closeTime) {
        if (symbol == null || symbol.isBlank() || openTime == null || closeTime == null) {
            return;
        }

        PendingMinuteCandleRetry pendingRetry = new PendingMinuteCandleRetry(symbol, openTime, closeTime);
        pendingRetries.putIfAbsent(pendingRetry, pendingRetry);
    }

    public void markSaved(String symbol, Instant openTime, Instant closeTime) {
        pendingRetries.remove(new PendingMinuteCandleRetry(symbol, openTime, closeTime));
    }

    public List<PendingMinuteCandleRetry> pendingRetries() {
        return List.copyOf(pendingRetries.values());
    }
}
