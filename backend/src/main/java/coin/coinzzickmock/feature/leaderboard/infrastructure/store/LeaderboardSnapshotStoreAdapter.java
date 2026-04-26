package coin.coinzzickmock.feature.leaderboard.infrastructure.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import coin.coinzzickmock.feature.leaderboard.application.port.LeaderboardSnapshotStore;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardEntry;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardMode;
import coin.coinzzickmock.feature.leaderboard.domain.LeaderboardSnapshot;
import coin.coinzzickmock.feature.leaderboard.infrastructure.config.LeaderboardProperties;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "coin.leaderboard.redis", name = "enabled", havingValue = "true")
public class LeaderboardSnapshotStoreAdapter implements LeaderboardSnapshotStore {
    private static final String ACTIVE_KEY_SUFFIX = "active:v2";
    private static final String PROFIT_RATE_ZSET = "realized-profit-rate:zset";
    private static final String WALLET_BALANCE_ZSET = "wallet-balance:zset";
    private static final String MEMBERS_HASH = "members:hash";
    private static final String META_HASH = "meta:hash";
    private static final String REFRESHED_AT_FIELD = "refreshedAt";

    private final StringRedisTemplate redisTemplate;
    private final LeaderboardProperties leaderboardProperties;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<LeaderboardSnapshot> findTop(LeaderboardMode mode, int limit, int tieSlack) {
        String version = redisTemplate.opsForValue().get(activePointerKey());
        if (version == null || version.isBlank()) {
            return Optional.empty();
        }

        int candidateCount = Math.max(limit, limit + tieSlack);
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(zsetKey(version, mode), 0, candidateCount - 1);
        if (tuples == null || tuples.isEmpty()) {
            return Optional.empty();
        }

        List<String> memberIds = tuples.stream()
                .map(ZSetOperations.TypedTuple::getValue)
                .toList();
        List<Object> values = redisTemplate.opsForHash().multiGet(membersKey(version), new ArrayList<>(memberIds));
        Map<String, LeaderboardEntry> entriesByMemberId = new LinkedHashMap<>();
        for (int index = 0; index < memberIds.size(); index++) {
            LeaderboardEntry entry = readEntry(values.get(index));
            if (entry != null) {
                entriesByMemberId.put(memberIds.get(index), entry);
            }
        }

        if (entriesByMemberId.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new LeaderboardSnapshot(
                List.copyOf(entriesByMemberId.values()),
                readRefreshedAt(version)
        ));
    }

    @Override
    public void replace(LeaderboardSnapshot snapshot) {
        String version = "v" + snapshot.refreshedAt().toEpochMilli();
        String profitRateKey = zsetKey(version, LeaderboardMode.PROFIT_RATE);
        String walletBalanceKey = zsetKey(version, LeaderboardMode.WALLET_BALANCE);
        String membersKey = membersKey(version);
        String metaKey = metaKey(version);
        redisTemplate.execute(new SessionCallback<List<Object>>() {
            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            public List<Object> execute(RedisOperations operations) {
                operations.multi();
                operations.delete(List.of(profitRateKey, walletBalanceKey, membersKey, metaKey));
                for (LeaderboardEntry entry : snapshot.entries()) {
                    operations.opsForZSet().add(profitRateKey, entry.memberId(), entry.profitRate());
                    operations.opsForZSet().add(walletBalanceKey, entry.memberId(), entry.walletBalance());
                    operations.opsForHash().put(membersKey, entry.memberId(), writeEntry(entry));
                }
                operations.opsForHash().put(metaKey, REFRESHED_AT_FIELD, snapshot.refreshedAt().toString());
                operations.opsForValue().set(activePointerKey(), version);
                return operations.exec();
            }
        });
    }

    @Override
    public void update(LeaderboardEntry entry) {
        String version = redisTemplate.opsForValue().get(activePointerKey());
        if (version == null || version.isBlank()) {
            return;
        }

        String profitRateKey = zsetKey(version, LeaderboardMode.PROFIT_RATE);
        String walletBalanceKey = zsetKey(version, LeaderboardMode.WALLET_BALANCE);
        String membersKey = membersKey(version);
        redisTemplate.execute(new SessionCallback<List<Object>>() {
            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            public List<Object> execute(RedisOperations operations) {
                operations.multi();
                operations.opsForZSet().add(profitRateKey, entry.memberId(), entry.profitRate());
                operations.opsForZSet().add(walletBalanceKey, entry.memberId(), entry.walletBalance());
                operations.opsForHash().put(membersKey, entry.memberId(), writeEntry(entry));
                return operations.exec();
            }
        });
    }

    private LeaderboardEntry readEntry(Object value) {
        if (!(value instanceof String json)) {
            return null;
        }

        try {
            MemberSnapshot snapshot = objectMapper.readValue(json, MemberSnapshot.class);
            return new LeaderboardEntry(
                    snapshot.memberId(),
                    snapshot.nickname(),
                    snapshot.walletBalance(),
                    snapshot.profitRate(),
                    snapshot.updatedAt()
            );
        } catch (JsonProcessingException exception) {
            log.warn("Failed to parse leaderboard member snapshot.", exception);
            return null;
        }
    }

    private String writeEntry(LeaderboardEntry entry) {
        try {
            return objectMapper.writeValueAsString(new MemberSnapshot(
                    entry.memberId(),
                    entry.nickname(),
                    entry.walletBalance(),
                    entry.profitRate(),
                    entry.updatedAt()
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize leaderboard member snapshot.", exception);
        }
    }

    private Instant readRefreshedAt(String version) {
        Object value = redisTemplate.opsForHash().get(metaKey(version), REFRESHED_AT_FIELD);
        if (!(value instanceof String refreshedAt)) {
            return Instant.now();
        }
        return Instant.parse(refreshedAt);
    }

    private String activePointerKey() {
        return prefix() + ACTIVE_KEY_SUFFIX;
    }

    private String zsetKey(String version, LeaderboardMode mode) {
        return versionPrefix(version) + (mode == LeaderboardMode.PROFIT_RATE ? PROFIT_RATE_ZSET : WALLET_BALANCE_ZSET);
    }

    private String membersKey(String version) {
        return versionPrefix(version) + MEMBERS_HASH;
    }

    private String metaKey(String version) {
        return versionPrefix(version) + META_HASH;
    }

    private String versionPrefix(String version) {
        return prefix() + version + ":";
    }

    private String prefix() {
        return leaderboardProperties.getRedis().getKeyPrefix();
    }

    private record MemberSnapshot(
            String memberId,
            String nickname,
            double walletBalance,
            double profitRate,
            Instant updatedAt
    ) {
    }
}
