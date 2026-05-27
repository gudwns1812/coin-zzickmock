package coin.coinzzickmock.feature.market.infrastructure.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coin.coinzzickmock.feature.market.application.dto.MarketCandleResult;
import coin.coinzzickmock.feature.market.application.latestwindow.MarketLatestCandleWindowKey;
import coin.coinzzickmock.feature.market.application.latestwindow.MarketLatestCandleWindowPage;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class MarketLatestCandleWindowCacheStoreTest {
    @Test
    void writesReadsAndAppliesTtl() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        ObjectMapper objectMapper = objectMapper();
        MarketLatestCandleWindowCacheStore store = new MarketLatestCandleWindowCacheStore(
                redisTemplate,
                objectMapper,
                "test::"
        );
        MarketLatestCandleWindowKey key = key();
        MarketLatestCandleWindowPage page = page();
        String redisKey = "test::marketLatestCandleWindow::" + key.cacheKey();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(objectMapper.writeValueAsString(page));

        store.write(key, page, Duration.ofSeconds(45));
        var cached = store.read(key);

        verify(valueOperations).set(eq(redisKey), any(String.class), eq(Duration.ofSeconds(45)));
        assertTrue(cached.page().isPresent());
        assertEquals("hit", cached.result());
        assertEquals(page, cached.page().get());
    }

    @Test
    void returnsMissWhenRedisReadFails() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(any())).thenThrow(new IllegalStateException("redis down"));
        MarketLatestCandleWindowCacheStore store = new MarketLatestCandleWindowCacheStore(
                redisTemplate,
                objectMapper(),
                "test::"
        );

        var read = store.read(key());

        assertTrue(read.page().isEmpty());
        assertEquals("unavailable", read.result());
    }

    @Test
    void writeFailureDoesNotEscape() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new IllegalStateException("redis down")).when(valueOperations).set(any(), any(), any(Duration.class));
        MarketLatestCandleWindowCacheStore store = new MarketLatestCandleWindowCacheStore(
                redisTemplate,
                objectMapper(),
                "test::"
        );

        store.write(key(), page(), Duration.ofSeconds(30));
    }

    private static ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        return objectMapper;
    }

    private static MarketLatestCandleWindowKey key() {
        return new MarketLatestCandleWindowKey(
                "BTCUSDT",
                MarketCandleInterval.ONE_MINUTE,
                120,
                Instant.parse("2026-04-21T00:00:00Z")
        );
    }

    private static MarketLatestCandleWindowPage page() {
        return new MarketLatestCandleWindowPage(
                List.of(new MarketCandleResult(
                        Instant.parse("2026-04-21T00:00:00Z"),
                        Instant.parse("2026-04-21T00:01:00Z"),
                        100,
                        101,
                        99,
                        100.5,
                        10
                )),
                MarketCandleInterval.ONE_MINUTE,
                120,
                Instant.parse("2026-04-21T00:00:00Z"),
                Instant.parse("2026-04-21T00:01:00Z")
        );
    }
}
