package coin.coinzzickmock.common.web;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public final class SseSubscriptionRegistry<K> {
    private final ConcurrentMap<K, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final ConcurrentMap<K, Semaphore> subscriberLimits = new ConcurrentHashMap<>();
    private final int maxSubscribersPerKey;
    private final Semaphore totalSubscriberLimit;

    public SseSubscriptionRegistry(int maxSubscribersPerKey, int maxSubscribersTotal) {
        this.maxSubscribersPerKey = maxSubscribersPerKey;
        this.totalSubscriberLimit = new Semaphore(maxSubscribersTotal, true);
    }

    public Reservation<K> reserve(K key) {
        if (!totalSubscriberLimit.tryAcquire()) {
            return Reservation.rejected(ReservationRejection.TOTAL_LIMIT);
        }

        Semaphore keyLimit = subscriberLimits.computeIfAbsent(
                key,
                ignored -> new Semaphore(maxSubscribersPerKey, true)
        );
        if (!keyLimit.tryAcquire()) {
            totalSubscriberLimit.release();
            return Reservation.rejected(ReservationRejection.KEY_LIMIT);
        }

        return Reservation.accepted(new Permit<>(key));
    }

    public void register(Permit<K> permit, SseEmitter emitter) {
        emitters.computeIfAbsent(permit.key(), ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        permit.markRegistered();
    }

    public boolean unregister(K key, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> keyEmitters = emitters.get(key);
        if (keyEmitters == null) {
            return false;
        }

        boolean removed = keyEmitters.remove(emitter);
        if (keyEmitters.isEmpty()) {
            emitters.remove(key, keyEmitters);
        }
        if (removed) {
            release(key);
        }
        return removed;
    }

    public boolean release(Permit<K> permit) {
        if (!permit.markReleasedBeforeRegistration()) {
            return false;
        }
        release(permit.key());
        return true;
    }

    public CopyOnWriteArrayList<SseEmitter> subscribers(K key) {
        return emitters.get(key);
    }

    public boolean hasSubscriberLimit(K key) {
        return subscriberLimits.containsKey(key);
    }

    private void release(K key) {
        totalSubscriberLimit.release();
        Semaphore keyLimit = subscriberLimits.get(key);
        if (keyLimit != null) {
            keyLimit.release();
            cleanupKeyLimit(key, keyLimit);
        }
    }

    private void cleanupKeyLimit(K key, Semaphore keyLimit) {
        if (keyLimit.availablePermits() == maxSubscribersPerKey && !emitters.containsKey(key)) {
            subscriberLimits.remove(key, keyLimit);
        }
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

    public enum ReservationRejection {
        TOTAL_LIMIT,
        KEY_LIMIT
    }

    public static final class Permit<K> {
        private final K key;
        private final AtomicBoolean released = new AtomicBoolean(false);
        private final AtomicBoolean registered = new AtomicBoolean(false);

        private Permit(K key) {
            this.key = key;
        }

        public K key() {
            return key;
        }

        private void markRegistered() {
            registered.set(true);
        }

        private boolean markReleasedBeforeRegistration() {
            return !registered.get() && released.compareAndSet(false, true);
        }
    }
}
