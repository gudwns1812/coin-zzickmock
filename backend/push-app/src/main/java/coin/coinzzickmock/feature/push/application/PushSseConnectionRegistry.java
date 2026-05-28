package coin.coinzzickmock.feature.push.application;

import coin.coinzzickmock.feature.push.application.PushServerProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
@RequiredArgsConstructor
public class PushSseConnectionRegistry {
    private final PushServerProperties properties;
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<PushSseConnection>> byKey = new ConcurrentHashMap<>();
    private final AtomicInteger total = new AtomicInteger();

    public SseEmitter register(String key, String clientKey, long timeoutMs) {
        return register(Set.of(key), clientKey, timeoutMs);
    }

    public SseEmitter register(Set<String> keys, String clientKey, long timeoutMs) {
        if (keys == null || keys.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "at least one key is required");
        }
        if (total.get() + keys.size() > properties.maxTotalSubscribers()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "push_sse_total_limit");
        }
        for (String key : keys) {
            if (byKey.getOrDefault(key, new CopyOnWriteArrayList<>()).size() >= properties.maxSubscribersPerKey()) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "push_sse_key_limit");
            }
        }
        SseEmitter emitter = new SseEmitter(timeoutMs);
        List<PushSseConnection> registered = new ArrayList<>();
        for (String key : keys) {
            CopyOnWriteArrayList<PushSseConnection> connections = byKey.computeIfAbsent(key, ignored -> new CopyOnWriteArrayList<>());
            PushSseConnection connection = new PushSseConnection(key, clientKey, emitter);
            removeExistingClient(connections, clientKey);
            connections.add(connection);
            registered.add(connection);
            total.incrementAndGet();
        }
        emitter.onCompletion(() -> registered.forEach(this::unregister));
        emitter.onTimeout(() -> registered.forEach(this::unregister));
        emitter.onError(error -> registered.forEach(this::unregister));
        return emitter;
    }

    public List<PushSseConnection> connectionsFor(Set<String> keys) {
        List<PushSseConnection> selected = new ArrayList<>();
        for (String key : keys) {
            selected.addAll(byKey.getOrDefault(key, new CopyOnWriteArrayList<>()));
        }
        return selected;
    }

    public void unregister(PushSseConnection connection) {
        CopyOnWriteArrayList<PushSseConnection> connections = byKey.get(connection.key());
        if (connections != null && connections.remove(connection)) {
            total.decrementAndGet();
            if (connections.isEmpty()) {
                byKey.remove(connection.key(), connections);
            }
        }
    }

    public int totalSubscriberCount() {
        return total.get();
    }

    private void removeExistingClient(CopyOnWriteArrayList<PushSseConnection> connections, String clientKey) {
        if (clientKey == null || clientKey.isBlank()) {
            return;
        }
        for (PushSseConnection existing : connections) {
            if (clientKey.equals(existing.clientKey()) && connections.remove(existing)) {
                total.decrementAndGet();
                existing.emitter().complete();
            }
        }
    }
}
