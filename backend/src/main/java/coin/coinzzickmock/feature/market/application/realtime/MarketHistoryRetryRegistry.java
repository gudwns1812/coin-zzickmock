package coin.coinzzickmock.feature.market.application.realtime;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MarketHistoryRetryRegistry {
    private final ConcurrentMap<PendingMinuteCandleRetry, PendingMinuteCandleRetry> pendingRetries =
            new ConcurrentHashMap<>();

    public void markPending(String symbol, Instant openTime, Instant closeTime) {
        if (symbol == null || symbol.isBlank() || openTime == null || closeTime == null) {
            return;
        }

        PendingMinuteCandleRetry pendingRetry = new PendingMinuteCandleRetry(symbol, openTime, closeTime);
        if (pendingRetries.putIfAbsent(pendingRetry, pendingRetry) == null) {
            log.warn("Queued market history candle retry. symbol={} openTime={} closeTime={}",
                    symbol, openTime, closeTime);
        }
    }

    public void markSaved(String symbol, Instant openTime, Instant closeTime) {
        if (pendingRetries.remove(new PendingMinuteCandleRetry(symbol, openTime, closeTime)) != null) {
            log.info("Resolved market history candle retry. symbol={} openTime={} closeTime={}",
                    symbol, openTime, closeTime);
        }
    }

    public List<PendingMinuteCandleRetry> pendingRetries() {
        return List.copyOf(pendingRetries.values());
    }
}
