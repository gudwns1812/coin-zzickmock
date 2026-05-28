package coin.coinzzickmock.feature.push.application;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public record PushSseConnection(
        String key,
        String clientKey,
        SseEmitter emitter,
        Map<String, Boolean> deliveredDedupeKeys
) {
    public PushSseConnection(String key, String clientKey, SseEmitter emitter) {
        this(key, clientKey, emitter, boundedDedupeMap());
    }

    private static Map<String, Boolean> boundedDedupeMap() {
        return java.util.Collections.synchronizedMap(new LinkedHashMap<>(128, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                return size() > 128;
            }
        });
    }

    public boolean firstDelivery(String dedupeKey) {
        if (dedupeKey == null || dedupeKey.isBlank()) {
            return true;
        }
        synchronized (deliveredDedupeKeys) {
            return deliveredDedupeKeys.putIfAbsent(dedupeKey, Boolean.TRUE) == null;
        }
    }
}
