package coin.coinzzickmock.feature.order.application.realtime;

import coin.coinzzickmock.feature.market.application.realtime.MarketPriceMovementDirection;
import coin.coinzzickmock.feature.order.application.result.PendingOrderCandidate;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class PendingLimitOrderBook {
    private final ConcurrentMap<OrderKey, Entry> candidates = new ConcurrentHashMap<>();

    public void hydrate(Collection<PendingOrderCandidate> pendingOrders) {
        candidates.clear();
        pendingOrders.forEach(this::add);
    }

    public void add(Long memberId, FuturesOrder order) {
        add(new PendingOrderCandidate(memberId, order), order.orderTime());
    }

    public void addAfterCommit(Long memberId, FuturesOrder order) {
        afterCommit(() -> add(new PendingOrderCandidate(memberId, order), Instant.now()));
    }

    public void add(PendingOrderCandidate candidate) {
        add(candidate, candidate.order().orderTime());
    }

    private void add(PendingOrderCandidate candidate, Instant effectiveAt) {
        if (!isIndexable(candidate.order())) {
            return;
        }
        candidates.put(OrderKey.from(candidate), new Entry(candidate, effectiveAt));
    }

    public void remove(Long memberId, String orderId) {
        candidates.remove(new OrderKey(memberId, orderId));
    }

    public void removeAfterCommit(Long memberId, String orderId) {
        afterCommit(() -> remove(memberId, orderId));
    }

    public void replace(Long memberId, FuturesOrder order) {
        remove(memberId, order.orderId());
        add(memberId, order);
    }

    public void replaceAfterCommit(Long memberId, FuturesOrder order) {
        afterCommit(() -> replace(memberId, order, Instant.now()));
    }

    public void replaceObservedNow(Long memberId, FuturesOrder order) {
        replace(memberId, order, Instant.now());
    }

    private void replace(Long memberId, FuturesOrder order, Instant effectiveAt) {
        remove(memberId, order.orderId());
        add(new PendingOrderCandidate(memberId, order), effectiveAt);
    }

    public List<PendingOrderCandidate> executableCandidates(
            String symbol,
            double previousLastPrice,
            double currentLastPrice,
            MarketPriceMovementDirection direction
    ) {
        return executableCandidates(symbol, previousLastPrice, currentLastPrice, direction, null);
    }

    public List<PendingOrderCandidate> executableCandidates(
            String symbol,
            double previousLastPrice,
            double currentLastPrice,
            MarketPriceMovementDirection direction,
            Instant movementReceivedAt
    ) {
        if (direction == MarketPriceMovementDirection.UNCHANGED) {
            return List.of();
        }
        double lowerPrice = Math.min(previousLastPrice, currentLastPrice);
        double upperPrice = Math.max(previousLastPrice, currentLastPrice);
        boolean sellSide = direction == MarketPriceMovementDirection.UP;
        return candidates.values().stream()
                .filter(entry -> entry.candidate().symbol().equalsIgnoreCase(symbol))
                .filter(entry -> sellSide
                        ? entry.candidate().order().isSellSideLimitOrder()
                        : entry.candidate().order().isBuySideLimitOrder())
                .filter(entry -> entry.candidate().order().limitPrice() >= lowerPrice)
                .filter(entry -> entry.candidate().order().limitPrice() <= upperPrice)
                .filter(entry -> movementReceivedAt == null || !entry.effectiveAt().isAfter(movementReceivedAt))
                .map(Entry::candidate)
                .sorted(candidateComparator(direction))
                .toList();
    }

    public int size() {
        return candidates.size();
    }

    private boolean isIndexable(FuturesOrder order) {
        return order.isPending()
                && !order.isConditionalOrder()
                && FuturesOrder.TYPE_LIMIT.equalsIgnoreCase(order.orderType())
                && order.limitPrice() != null;
    }

    private Comparator<PendingOrderCandidate> candidateComparator(MarketPriceMovementDirection direction) {
        Comparator<PendingOrderCandidate> byPrice = Comparator.comparingDouble(
                candidate -> candidate.order().limitPrice()
        );
        if (direction == MarketPriceMovementDirection.DOWN) {
            byPrice = byPrice.reversed();
        }
        return byPrice
                .thenComparing(candidate -> candidate.order().orderTime())
                .thenComparing(PendingOrderCandidate::orderId);
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private record OrderKey(Long memberId, String orderId) {
        private static OrderKey from(PendingOrderCandidate candidate) {
            return new OrderKey(candidate.memberId(), candidate.orderId());
        }
    }

    private record Entry(PendingOrderCandidate candidate, Instant effectiveAt) {
    }
}
