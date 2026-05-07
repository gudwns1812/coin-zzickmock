package coin.coinzzickmock.common.web;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public final class SseSubscriptionRegistry<K> {
    private final ConcurrentMap<K, ConcurrentMap<String, SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final ConcurrentMap<K, Semaphore> subscriberLimits = new ConcurrentHashMap<>();
    private final ConcurrentMap<SubscriberIdentity<K>, Boolean> pendingCapacity = new ConcurrentHashMap<>();
    private final int maxSubscribersPerKey;
    private final Semaphore totalSubscriberLimit;

    public SseSubscriptionRegistry(int maxSubscribersPerKey, int maxSubscribersTotal) {
        this.maxSubscribersPerKey = maxSubscribersPerKey;
        this.totalSubscriberLimit = new Semaphore(maxSubscribersTotal, true);
    }

    public Reservation<K> reserve(K key) {
        return reserve(key, SseClientKey.fallback().value());
    }

    public synchronized Reservation<K> reserve(K key, String clientKey) {
        SubscriberIdentity<K> identity = new SubscriberIdentity<>(key, clientKey);
        ConcurrentMap<String, SseEmitter> keyEmitters = emitters.get(key);
        if ((keyEmitters != null && keyEmitters.containsKey(clientKey)) || pendingCapacity.containsKey(identity)) {
            return Reservation.accepted(new Permit<>(key, clientKey, false));
        }

        ReservationRejection rejection = tryAcquireCapacity(key);
        if (rejection != null) {
            return Reservation.rejected(rejection);
        }

        pendingCapacity.put(identity, Boolean.TRUE);
        return Reservation.accepted(new Permit<>(key, clientKey, true));
    }

    public synchronized Registration<K> register(Permit<K> permit, SseEmitter emitter) {
        if (!permit.markRegistered()) {
            throw new IllegalStateException("SSE subscription permit is not active");
        }

        SubscriberIdentity<K> identity = new SubscriberIdentity<>(permit.key(), permit.clientKey());
        if (permit.capacityAcquired()) {
            pendingCapacity.remove(identity);
        }

        ConcurrentMap<String, SseEmitter> keyEmitters = emitters.computeIfAbsent(
                permit.key(),
                ignored -> new ConcurrentHashMap<>()
        );
        SseEmitter previous = keyEmitters.get(permit.clientKey());
        if (previous == null && !permit.capacityAcquired()) {
            ReservationRejection rejection = tryAcquireCapacity(permit.key());
            if (rejection != null) {
                cleanupEmptyEmitters(permit.key(), keyEmitters);
                return Registration.rejected(rejection);
            }
        }

        keyEmitters.put(permit.clientKey(), emitter);
        if (previous != null && permit.capacityAcquired()) {
            releaseCapacity(permit.key());
        }
        return Registration.registered(previous);
    }

    public synchronized boolean unregister(K key, SseEmitter emitter) {
        ConcurrentMap<String, SseEmitter> keyEmitters = emitters.get(key);
        if (keyEmitters == null) {
            return false;
        }

        for (String clientKey : List.copyOf(keyEmitters.keySet())) {
            if (keyEmitters.get(clientKey) == emitter) {
                return unregister(key, clientKey, emitter);
            }
        }
        return false;
    }

    public synchronized boolean unregister(K key, String clientKey, SseEmitter emitter) {
        ConcurrentMap<String, SseEmitter> keyEmitters = emitters.get(key);
        if (keyEmitters == null || keyEmitters.get(clientKey) != emitter) {
            return false;
        }

        keyEmitters.remove(clientKey, emitter);
        cleanupEmptyEmitters(key, keyEmitters);
        releaseCapacity(key);
        return true;
    }

    public synchronized boolean release(Permit<K> permit) {
        if (!permit.markReleasedBeforeRegistration()) {
            return false;
        }

        if (permit.capacityAcquired()) {
            pendingCapacity.remove(new SubscriberIdentity<>(permit.key(), permit.clientKey()));
            releaseCapacity(permit.key());
        }
        return true;
    }

    public synchronized List<SseEmitter> subscribers(K key) {
        ConcurrentMap<String, SseEmitter> keyEmitters = emitters.get(key);
        if (keyEmitters == null || keyEmitters.isEmpty()) {
            return List.of();
        }
        return List.copyOf(keyEmitters.values());
    }

    public synchronized int subscriberCount(K key) {
        ConcurrentMap<String, SseEmitter> keyEmitters = emitters.get(key);
        if (keyEmitters == null) {
            return 0;
        }
        return keyEmitters.size();
    }

    public synchronized int totalSubscriberCount() {
        return emitters.values().stream()
                .mapToInt(ConcurrentMap::size)
                .sum();
    }

    public synchronized boolean hasSubscriberLimit(K key) {
        return subscriberLimits.containsKey(key);
    }

    private ReservationRejection tryAcquireCapacity(K key) {
        if (!totalSubscriberLimit.tryAcquire()) {
            return ReservationRejection.TOTAL_LIMIT;
        }

        Semaphore keyLimit = subscriberLimits.computeIfAbsent(
                key,
                ignored -> new Semaphore(maxSubscribersPerKey, true)
        );
        if (!keyLimit.tryAcquire()) {
            totalSubscriberLimit.release();
            return ReservationRejection.KEY_LIMIT;
        }
        return null;
    }

    private void releaseCapacity(K key) {
        totalSubscriberLimit.release();
        Semaphore keyLimit = subscriberLimits.get(key);
        if (keyLimit != null) {
            keyLimit.release();
            cleanupKeyLimit(key, keyLimit);
        }
    }

    private void cleanupEmptyEmitters(K key, ConcurrentMap<String, SseEmitter> keyEmitters) {
        if (keyEmitters.isEmpty()) {
            emitters.remove(key, keyEmitters);
        }
    }

    private void cleanupKeyLimit(K key, Semaphore keyLimit) {
        if (keyLimit.availablePermits() == maxSubscribersPerKey
                && !emitters.containsKey(key)
                && hasNoPendingCapacity(key)) {
            subscriberLimits.remove(key, keyLimit);
        }
    }

    private boolean hasNoPendingCapacity(K key) {
        return pendingCapacity.keySet().stream().noneMatch(identity -> identity.key().equals(key));
    }

    private record SubscriberIdentity<K>(K key, String clientKey) {
    }

    public record Reservation<K>(
            Permit<K> permit,
            ReservationRejection rejection
    ) {
        public static <K> Reservation<K> accepted(Permit<K> permit) {
            return new Reservation<>(permit, null);
        }

        public static <K> Reservation<K> rejected(ReservationRejection rejection) {
            return new Reservation<>(null, rejection);
        }

        public boolean accepted() {
            return permit != null;
        }
    }

    public record Registration<K>(
            SseEmitter replacedEmitter,
            ReservationRejection rejection
    ) {
        public static <K> Registration<K> registered(SseEmitter replacedEmitter) {
            return new Registration<>(replacedEmitter, null);
        }

        public static <K> Registration<K> rejected(ReservationRejection rejection) {
            return new Registration<>(null, rejection);
        }

        public boolean registered() {
            return rejection == null;
        }

        public boolean replaced() {
            return replacedEmitter != null;
        }
    }

    public enum ReservationRejection {
        TOTAL_LIMIT,
        KEY_LIMIT
    }

    public static final class Permit<K> {
        private final K key;
        private final String clientKey;
        private final boolean capacityAcquired;
        private final AtomicBoolean released = new AtomicBoolean(false);
        private final AtomicBoolean registered = new AtomicBoolean(false);

        private Permit(K key, String clientKey, boolean capacityAcquired) {
            this.key = key;
            this.clientKey = clientKey;
            this.capacityAcquired = capacityAcquired;
        }

        public K key() {
            return key;
        }

        public String clientKey() {
            return clientKey;
        }

        private boolean capacityAcquired() {
            return capacityAcquired;
        }

        private boolean markRegistered() {
            return !released.get() && registered.compareAndSet(false, true);
        }

        private boolean markReleasedBeforeRegistration() {
            return !registered.get() && released.compareAndSet(false, true);
        }
    }
}
