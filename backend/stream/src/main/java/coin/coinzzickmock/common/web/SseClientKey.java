package coin.coinzzickmock.common.web;

import java.util.UUID;

public record SseClientKey(String value) {
    private static final int MAX_LENGTH = 128;
    private static final String FALLBACK_PREFIX = "server:";

    public SseClientKey {
        if (value == null || value.isBlank() || value.length() > MAX_LENGTH) {
            throw new SseClientKeyException("Invalid SSE client key");
        }
    }

    public static SseClientKey resolve(String rawClientKey) {
        if (rawClientKey == null || rawClientKey.isBlank()) {
            return fallback();
        }

        String normalized = rawClientKey.trim();
        if (normalized.length() > MAX_LENGTH) {
            throw new SseClientKeyException("Invalid SSE client key");
        }
        return new SseClientKey(normalized);
    }

    public static SseClientKey fallback() {
        return new SseClientKey(FALLBACK_PREFIX + UUID.randomUUID());
    }
}
