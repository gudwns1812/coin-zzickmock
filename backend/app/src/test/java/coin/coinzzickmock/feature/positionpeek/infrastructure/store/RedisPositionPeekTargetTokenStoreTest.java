package coin.coinzzickmock.feature.positionpeek.infrastructure.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coin.coinzzickmock.feature.positionpeek.application.dto.PositionPeekTargetTokenPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisPositionPeekTargetTokenStoreTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void storesPayloadByTokenHashWithTtl() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        RedisPositionPeekTargetTokenStore store = new RedisPositionPeekTargetTokenStore(redisTemplate, objectMapper);
        PositionPeekTargetTokenPayload payload = payload();

        store.save("hash-value", payload, Duration.ofMinutes(10));

        verify(valueOperations).set(
                eq("coin:position-peek:target-token:v1:hash-value"),
                eq("""
                        {"targetMemberId":7,"rank":2,"nickname":"target","walletBalance":120000.0,"profitRate":20.0,"leaderboardMode":"profitRate"}""".stripIndent().trim()),
                eq(Duration.ofMinutes(10))
        );
    }

    @Test
    void readsPayloadByTokenHash() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("coin:position-peek:target-token:v1:hash-value"))
                .thenReturn(objectMapper.writeValueAsString(payload()));
        RedisPositionPeekTargetTokenStore store = new RedisPositionPeekTargetTokenStore(redisTemplate, objectMapper);

        Optional<PositionPeekTargetTokenPayload> result = store.findByTokenHash("hash-value");

        assertThat(result).contains(payload());
    }

    @Test
    void treatsMissingPayloadAsInvalidToken() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        RedisPositionPeekTargetTokenStore store = new RedisPositionPeekTargetTokenStore(redisTemplate, objectMapper);

        assertThat(store.findByTokenHash("hash-value")).isEmpty();
    }

    private PositionPeekTargetTokenPayload payload() {
        return new PositionPeekTargetTokenPayload(
                7L,
                2,
                "target",
                120_000.0,
                20.0,
                "profitRate"
        );
    }
}
