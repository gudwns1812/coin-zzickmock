package coin.coinzzickmock.feature.market.web;

import coin.coinzzickmock.common.web.SseClientKey;
import coin.coinzzickmock.common.web.SseSubscriptionRegistry.ReservationRejection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Semaphore;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

final class MarketSummarySubscriptionRegistry {
    private final Map<String, ClientSubscription> subscriptionsByClientKey = new LinkedHashMap<>();
    private final Map<String, Set<String>> clientKeysBySymbol = new LinkedHashMap<>();
    private final Map<String, Semaphore> subscriberLimitsBySymbol = new LinkedHashMap<>();
    private final Map<String, Integer> pendingSubscriberCountsBySymbol = new LinkedHashMap<>();
    private final int maxSubscribersPerSymbol;
    private final Semaphore totalSubscriberLimit;

    MarketSummarySubscriptionRegistry(int maxSubscribersPerSymbol, int maxSubscribersTotal) {
        this.maxSubscribersPerSymbol = maxSubscribersPerSymbol;
        this.totalSubscriberLimit = new Semaphore(maxSubscribersTotal, true);
    }

    synchronized Reservation reserve(Set<String> symbols, String clientKey) {
        String resolvedClientKey = SseClientKey.resolve(clientKey).value();
        ClientSubscription previous = subscriptionsByClientKey.get(resolvedClientKey);
        boolean totalCapacityAcquired = previous == null;
        Set<String> acquiredSymbols = new LinkedHashSet<>();

        if (totalCapacityAcquired && !totalSubscriberLimit.tryAcquire()) {
            return Reservation.rejected(ReservationRejection.TOTAL_LIMIT);
        }

        for (String symbol : symbols) {
            if (previous != null && previous.symbols().contains(symbol)) {
                continue;
            }
            if (!acquireSymbolCapacity(symbol)) {
                acquiredSymbols.forEach(this::decrementPendingSubscriberCount);
                releaseSymbolCapacities(acquiredSymbols);
                if (totalCapacityAcquired) {
                    totalSubscriberLimit.release();
                }
                return Reservation.rejected(ReservationRejection.KEY_LIMIT);
            }
            acquiredSymbols.add(symbol);
            incrementPendingSubscriberCount(symbol);
        }

        return Reservation.accepted(new MarketRealtimeSseBroker.SseSubscriptionPermit(
                resolvedClientKey,
                symbols,
                totalCapacityAcquired,
                acquiredSymbols
        ));
    }

    synchronized Registration register(MarketRealtimeSseBroker.SseSubscriptionPermit permit, SseEmitter emitter) {
        if (!permit.markRegistered()) {
            throw new IllegalStateException("SSE subscription permit is not active");
        }
        permit.acquiredSymbols().forEach(this::decrementPendingSubscriberCount);

        ClientSubscription previous = subscriptionsByClientKey.put(
                permit.clientKey(),
                new ClientSubscription(permit.clientKey(), permit.symbols(), emitter)
        );
        if (previous != null) {
            removeClientIndexes(previous.clientKey(), previous.symbols());
            releaseRemovedSymbolCapacities(previous.symbols(), permit.symbols());
        }
        addClientIndexes(permit.clientKey(), permit.symbols());
        return new Registration(previous == null ? null : previous.emitter());
    }

    synchronized boolean unregister(SseEmitter emitter) {
        for (ClientSubscription subscription : List.copyOf(subscriptionsByClientKey.values())) {
            if (subscription.emitter() == emitter) {
                return removeClientSubscription(subscription.clientKey(), emitter);
            }
        }
        return false;
    }

    synchronized boolean unregister(String clientKey, SseEmitter emitter) {
        return removeClientSubscription(clientKey, emitter);
    }

    synchronized boolean release(MarketRealtimeSseBroker.SseSubscriptionPermit permit) {
        if (!permit.markReleasedBeforeRegistration()) {
            return false;
        }
        permit.acquiredSymbols().forEach(this::decrementPendingSubscriberCount);
        releaseSymbolCapacities(permit.acquiredSymbols());
        if (permit.totalCapacityAcquired()) {
            totalSubscriberLimit.release();
        }
        return true;
    }

    synchronized List<SseEmitter> subscribers(String symbol) {
        Set<String> clientKeys = clientKeysBySymbol.get(symbol);
        if (clientKeys == null || clientKeys.isEmpty()) {
            return List.of();
        }
        return clientKeys.stream()
                .map(subscriptionsByClientKey::get)
                .filter(Objects::nonNull)
                .map(ClientSubscription::emitter)
                .toList();
    }

    synchronized boolean hasSubscriberLimit(String symbol) {
        return subscriberLimitsBySymbol.containsKey(symbol);
    }

    synchronized int subscriberCount(String symbol) {
        Set<String> clientKeys = clientKeysBySymbol.get(symbol);
        return clientKeys == null ? 0 : clientKeys.size();
    }

    synchronized int totalSubscriberCount() {
        return subscriptionsByClientKey.size();
    }

    private boolean removeClientSubscription(String clientKey, SseEmitter emitter) {
        ClientSubscription current = subscriptionsByClientKey.get(clientKey);
        if (current == null || current.emitter() != emitter) {
            return false;
        }
        subscriptionsByClientKey.remove(clientKey);
        removeClientIndexes(clientKey, current.symbols());
        releaseSymbolCapacities(current.symbols());
        totalSubscriberLimit.release();
        return true;
    }

    private boolean acquireSymbolCapacity(String symbol) {
        Semaphore symbolLimit = subscriberLimitsBySymbol.computeIfAbsent(
                symbol,
                ignored -> new Semaphore(maxSubscribersPerSymbol, true)
        );
        return symbolLimit.tryAcquire();
    }

    private void releaseRemovedSymbolCapacities(Set<String> previousSymbols, Set<String> nextSymbols) {
        previousSymbols.stream()
                .filter(symbol -> !nextSymbols.contains(symbol))
                .forEach(this::releaseSymbolCapacity);
    }

    private void releaseSymbolCapacities(Set<String> symbols) {
        symbols.forEach(this::releaseSymbolCapacity);
    }

    private void releaseSymbolCapacity(String symbol) {
        Semaphore symbolLimit = subscriberLimitsBySymbol.get(symbol);
        if (symbolLimit != null) {
            symbolLimit.release();
            cleanupSymbolLimit(symbol, symbolLimit);
        }
    }

    private void addClientIndexes(String clientKey, Set<String> symbols) {
        symbols.forEach(symbol -> clientKeysBySymbol
                .computeIfAbsent(symbol, ignored -> new LinkedHashSet<>())
                .add(clientKey));
    }

    private void removeClientIndexes(String clientKey, Set<String> symbols) {
        for (String symbol : symbols) {
            Set<String> clientKeys = clientKeysBySymbol.get(symbol);
            if (clientKeys == null) {
                continue;
            }
            clientKeys.remove(clientKey);
            if (clientKeys.isEmpty()) {
                clientKeysBySymbol.remove(symbol);
            }
        }
    }

    private void incrementPendingSubscriberCount(String symbol) {
        pendingSubscriberCountsBySymbol.merge(symbol, 1, Integer::sum);
    }

    private void decrementPendingSubscriberCount(String symbol) {
        Integer current = pendingSubscriberCountsBySymbol.get(symbol);
        if (current == null || current <= 1) {
            pendingSubscriberCountsBySymbol.remove(symbol);
            return;
        }
        pendingSubscriberCountsBySymbol.put(symbol, current - 1);
    }

    private void cleanupSymbolLimit(String symbol, Semaphore symbolLimit) {
        if (symbolLimit.availablePermits() == maxSubscribersPerSymbol
                && !clientKeysBySymbol.containsKey(symbol)
                && !pendingSubscriberCountsBySymbol.containsKey(symbol)) {
            subscriberLimitsBySymbol.remove(symbol, symbolLimit);
        }
    }

    private record ClientSubscription(String clientKey, Set<String> symbols, SseEmitter emitter) {
        private ClientSubscription {
            symbols = Collections.unmodifiableSet(new LinkedHashSet<>(symbols));
        }
    }

    record Reservation(MarketRealtimeSseBroker.SseSubscriptionPermit permit, ReservationRejection rejection) {
        static Reservation accepted(MarketRealtimeSseBroker.SseSubscriptionPermit permit) {
            return new Reservation(permit, null);
        }

        static Reservation rejected(ReservationRejection rejection) {
            return new Reservation(null, rejection);
        }

        boolean accepted() {
            return permit != null;
        }
    }

    record Registration(SseEmitter replacedEmitter) {
    }
}
