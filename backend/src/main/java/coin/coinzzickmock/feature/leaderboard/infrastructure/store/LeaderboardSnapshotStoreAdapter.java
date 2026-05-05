package coin.coinzzickmock.feature.leaderboard.infrastructure.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import coin.coinzzickmock.feature.leaderboard.application.result.LeaderboardMemberRankResult;
import coin.coinzzickmock.feature.leaderboard.application.store.LeaderboardSnapshotStore;
import coin.coinzzickmock.feature.leaderboard.application.store.LeaderboardSnapshotResult;
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
    private static final String ACTIVE_KEY_SUFFIX = "active:v3";
    private static final String PROFIT_RATE_ZSET = "realized-profit-rate:zset";
    private static final String WALLET_BALANCE_ZSET = "wallet-balance:zset";
    private static final String MEMBERS_HASH = "members:hash";
    private static final String META_HASH = "meta:hash";
    private static final String REFRESHED_AT_FIELD = "refreshedAt";

    private final StringRedisTemplate redisTemplate;
    private final LeaderboardProperties leaderboardProperties;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<LeaderboardSnapshotResult> findSnapshot(
            LeaderboardMode mode,
            int limit,
            Long currentMemberId
    ) {
        if (limit <= 0) {
            return Optional.empty();
        }

        String version = redisTemplate.opsForValue().get(activePointerKey());
        if (version == null || version.isBlank()) {
            return Optional.empty();
        }

        Optional<LeaderboardMemberRankResult> myRank = currentMemberId == null
                ? Optional.empty()
                : findMyRank(version, mode, currentMemberId);

        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(zsetKey(version, mode), 0, limit - 1);
        if (tuples == null || tuples.isEmpty()) {
            return Optional.empty();
        }

        return hydrateEntries(version, mode, tuples)
                .filter(entries -> !entries.isEmpty())
                .map(entries -> new LeaderboardSnapshotResult(
                        entries,
                        readRefreshedAt(version),
                        myRank
                ));
    }

    private Optional<LeaderboardMemberRankResult> findMyRank(String version, LeaderboardMode mode, Long memberId) {
        Long zeroBasedRank = redisTemplate.opsForZSet().reverseRank(zsetKey(version, mode), memberKey(memberId));
        if (zeroBasedRank == null) {
            return Optional.empty();
        }
        return Optional.of(new LeaderboardMemberRankResult(Math.toIntExact(zeroBasedRank + 1)));
    }

    private Optional<List<LeaderboardEntry>> hydrateEntries(
            String version,
            LeaderboardMode mode,
            Set<ZSetOperations.TypedTuple<String>> tuples
    ) {
        List<String> memberIds = tuples.stream()
                .map(ZSetOperations.TypedTuple::getValue)
                .toList();
        List<Object> values = redisTemplate.opsForHash().multiGet(membersKey(version), new ArrayList<>(memberIds));
        if (values == null || values.size() != memberIds.size()) {
            log.warn(
                    "Leaderboard snapshot member hash read returned invalid size. version={}, mode={}, expected={}, actual={}",
                    version,
                    mode.value(),
                    memberIds.size(),
                    values == null ? null : values.size()
            );
            return Optional.empty();
        }

        Map<String, LeaderboardEntry> entriesByMemberId = new LinkedHashMap<>();
        for (int index = 0; index < memberIds.size(); index++) {
            String memberKey = memberIds.get(index);
            LeaderboardEntry entry = readEntry(version, memberKey, values.get(index));
            if (entry == null) {
                return Optional.empty();
            }
            entriesByMemberId.put(memberKey, entry);
        }

        return Optional.of(List.copyOf(entriesByMemberId.values()));
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
                    String memberKey = memberKey(entry.memberId());
                    operations.opsForZSet().add(profitRateKey, memberKey, entry.profitRate());
                    operations.opsForZSet().add(walletBalanceKey, memberKey, entry.walletBalance());
                    operations.opsForHash().put(membersKey, memberKey, writeEntry(entry));
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
                String memberKey = memberKey(entry.memberId());
                operations.opsForZSet().add(profitRateKey, memberKey, entry.profitRate());
                operations.opsForZSet().add(walletBalanceKey, memberKey, entry.walletBalance());
                operations.opsForHash().put(membersKey, memberKey, writeEntry(entry));
                return operations.exec();
            }
        });
    }

    @Override
    public void remove(Long memberId) {
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
                String memberKey = memberKey(memberId);
                operations.opsForZSet().remove(profitRateKey, memberKey);
                operations.opsForZSet().remove(walletBalanceKey, memberKey);
                operations.opsForHash().delete(membersKey, memberKey);
                return operations.exec();
            }
        });
    }

    private LeaderboardEntry readEntry(String version, String memberKey, Object value) {
        if (!(value instanceof String json)) {
            log.warn("Leaderboard snapshot member hash is missing. version={}", version);
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
            log.warn(
                    "Failed to parse leaderboard member snapshot. version={}",
                    version,
                    exception
            );
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

    private String memberKey(Long memberId) {
        return String.valueOf(memberId);
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
            Long memberId,
            String nickname,
            double walletBalance,
            double profitRate,
            Instant updatedAt
    ) {
    }
}
