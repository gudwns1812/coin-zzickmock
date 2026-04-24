package coin.coinzzickmock.feature.order.application.realtime;

import coin.coinzzickmock.feature.order.application.result.PendingOrderCandidate;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

@Component
public class PendingOrderExecutionCache {
    private final ConcurrentMap<String, List<PendingOrderCandidate>> pendingBySymbol = new ConcurrentHashMap<>();

    public List<PendingOrderCandidate> refresh(String symbol, List<PendingOrderCandidate> candidates) {
        List<PendingOrderCandidate> snapshot = List.copyOf(candidates);
        if (snapshot.isEmpty()) {
            pendingBySymbol.remove(symbol);
            return List.of();
        }
        pendingBySymbol.put(symbol, snapshot);
        return snapshot;
    }

    public void evict(String symbol, String memberId, String orderId) {
        pendingBySymbol.computeIfPresent(symbol, (key, candidates) -> {
            List<PendingOrderCandidate> remaining = candidates.stream()
                    .filter(candidate -> !candidate.memberId().equals(memberId)
                            || !candidate.orderId().equals(orderId))
                    .toList();
            return remaining.isEmpty() ? null : remaining;
        });
    }
}
