package coin.coinzzickmock.feature.account.infrastructure.lock;

import coin.coinzzickmock.feature.account.application.service.WalletHistoryRolloverLock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class RedisWalletHistoryRolloverLock implements WalletHistoryRolloverLock {
    private static final String LOCK_KEY = "coin:account:wallet-history:rollover";
    private static final Duration LOCK_TTL = Duration.ofMinutes(10);
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) "
                    + "else return 0 end",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean runIfAcquired(Runnable task) {
        String token = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, token, LOCK_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            return false;
        }

        try {
            task.run();
            return true;
        } finally {
            release(token);
        }
    }

    private void release(String token) {
        redisTemplate.execute(RELEASE_SCRIPT, List.of(LOCK_KEY), token);
    }
}
