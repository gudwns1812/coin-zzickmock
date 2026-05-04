package coin.coinzzickmock.feature.leaderboard.infrastructure.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import coin.coinzzickmock.feature.leaderboard.application.store.LeaderboardSnapshotResult;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardMode;
import coin.coinzzickmock.feature.leaderboard.infrastructure.config.LeaderboardProperties;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

class LeaderboardSnapshotStoreAdapterTest {
    @Test
    void rejectsNonPositiveLimitBeforeReadingRedisRange() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        LeaderboardSnapshotStoreAdapter adapter = adapter(redisTemplate);

        Optional<LeaderboardSnapshotResult> result = adapter.findSnapshot(LeaderboardMode.PROFIT_RATE, 0, 1L);

        assertTrue(result.isEmpty());
        verify(redisTemplate, never()).opsForValue();
        verify(redisTemplate, never()).opsForZSet();
    }

    @Test
    void returnsEmptySnapshotWhenMemberHashIsMissing() {
        RedisMocks redis = redisMocks();
        stubActiveTopMember(redis, "1", Collections.singletonList(null));
        LeaderboardSnapshotStoreAdapter adapter = adapter(redis.template());

        Optional<LeaderboardSnapshotResult> result = adapter.findSnapshot(LeaderboardMode.PROFIT_RATE, 1, 1L);

        assertTrue(result.isEmpty());
        verify(redis.zSet()).reverseRangeWithScores("test:leaderboard:v1:realized-profit-rate:zset", 0, 0);
    }

    @Test
    void returnsEmptySnapshotWhenMemberHashCannotBeParsed() {
        RedisMocks redis = redisMocks();
        stubActiveTopMember(redis, "1", List.of("not-json"));
        LeaderboardSnapshotStoreAdapter adapter = adapter(redis.template());

        Optional<LeaderboardSnapshotResult> result = adapter.findSnapshot(LeaderboardMode.PROFIT_RATE, 1, 1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void returnsMyRankFromRedisReverseRank() {
        RedisMocks redis = redisMocks();
        when(redis.zSet().reverseRank("test:leaderboard:v1:realized-profit-rate:zset", "7")).thenReturn(6L);
        stubActiveTopMember(redis, "7", List.of("""
                {
                  "memberId": 7,
                  "nickname": "Redis",
                  "walletBalance": 130000.0,
                  "profitRate": 0.3,
                  "updatedAt": "2026-04-26T00:00:00Z"
                }
                """));
        when(redis.hash().get("test:leaderboard:v1:meta:hash", "refreshedAt"))
                .thenReturn("2026-04-26T00:00:01Z");
        LeaderboardSnapshotStoreAdapter adapter = adapter(redis.template());

        LeaderboardSnapshotResult result = adapter.findSnapshot(LeaderboardMode.PROFIT_RATE, 1, 7L)
                .orElseThrow();

        assertEquals(7, result.myRank().orElseThrow().rank());
        assertEquals("Redis", result.entries().get(0).nickname());
    }

    private void stubActiveTopMember(RedisMocks redis, String memberKey, List<Object> hashValues) {
        when(redis.value().get("test:leaderboard:active:v3")).thenReturn("v1");
        when(redis.zSet().reverseRangeWithScores(
                "test:leaderboard:v1:realized-profit-rate:zset",
                0,
                0
        )).thenReturn(tuples(memberKey));
        when(redis.hash().multiGet(
                "test:leaderboard:v1:members:hash",
                List.of(memberKey)
        )).thenReturn(hashValues);
    }

    @SuppressWarnings("unchecked")
    private RedisMocks redisMocks() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        ZSetOperations<String, String> zSetOperations = mock(ZSetOperations.class);
        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        return new RedisMocks(redisTemplate, valueOperations, zSetOperations, hashOperations);
    }

    private LeaderboardSnapshotStoreAdapter adapter(StringRedisTemplate redisTemplate) {
        LeaderboardProperties properties = new LeaderboardProperties();
        properties.getRedis().setKeyPrefix("test:leaderboard:");
        return new LeaderboardSnapshotStoreAdapter(
                redisTemplate,
                properties,
                new ObjectMapper().findAndRegisterModules()
        );
    }

    private Set<ZSetOperations.TypedTuple<String>> tuples(String memberKey) {
        return new LinkedHashSet<>(Set.of(new DefaultTypedTuple<>(memberKey, 0.3)));
    }

    private record RedisMocks(
            StringRedisTemplate template,
            ValueOperations<String, String> value,
            ZSetOperations<String, String> zSet,
            HashOperations<String, Object, Object> hash
    ) {
    }
}
