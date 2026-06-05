package coin.coinzzickmock.feature.positionpeek.infrastructure.store;

import coin.coinzzickmock.feature.positionpeek.application.dto.PositionPeekTargetTokenPayload;
import coin.coinzzickmock.feature.positionpeek.application.repository.PositionPeekTargetTokenStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisPositionPeekTargetTokenStore implements PositionPeekTargetTokenStore {
    private static final String KEY_PREFIX = "coin:position-peek:target-token:v1:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void save(String tokenHash, PositionPeekTargetTokenPayload payload, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key(tokenHash), objectMapper.writeValueAsString(payload), ttl);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize position peek target token payload.", exception);
        }
    }

    @Override
    public Optional<PositionPeekTargetTokenPayload> findByTokenHash(String tokenHash) {
        String json = redisTemplate.opsForValue().get(key(tokenHash));
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, PositionPeekTargetTokenPayload.class));
        } catch (JsonProcessingException exception) {
            log.warn("Failed to parse position peek target token payload.");
            return Optional.empty();
        }
    }

    private String key(String tokenHash) {
        return KEY_PREFIX + tokenHash;
    }
}
