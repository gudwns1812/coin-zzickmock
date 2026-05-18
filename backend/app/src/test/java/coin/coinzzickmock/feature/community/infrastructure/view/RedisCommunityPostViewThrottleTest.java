package coin.coinzzickmock.feature.community.infrastructure.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

class RedisCommunityPostViewThrottleTest {
    @Test
    void claimsViewWithSetIfAbsentAndExpiryWindow() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Duration window = Duration.ofMinutes(1);
        when(valueOperations.setIfAbsent("test:view:11:7", "1", window)).thenReturn(true);
        RedisCommunityPostViewThrottle throttle = throttle(redisTemplate);

        boolean claimed = throttle.tryClaim(11L, 7L, window);

        assertThat(claimed).isTrue();
        verify(valueOperations).setIfAbsent("test:view:11:7", "1", window);
    }

    @Test
    void skipsInvalidActorWithoutRedisWrite() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        RedisCommunityPostViewThrottle throttle = throttle(redisTemplate);

        assertThat(throttle.tryClaim(11L, null, Duration.ofMinutes(1))).isFalse();

        verifyNoInteractions(redisTemplate);
    }

    private static RedisCommunityPostViewThrottle throttle(StringRedisTemplate redisTemplate) {
        RedisCommunityPostViewThrottle throttle = new RedisCommunityPostViewThrottle(redisTemplate);
        ReflectionTestUtils.setField(throttle, "keyPrefix", "test:view");
        return throttle;
    }
}
