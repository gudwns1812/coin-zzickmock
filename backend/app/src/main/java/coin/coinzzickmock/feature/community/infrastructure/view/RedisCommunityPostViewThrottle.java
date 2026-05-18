package coin.coinzzickmock.feature.community.infrastructure.view;

import coin.coinzzickmock.feature.community.application.view.CommunityPostViewThrottle;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class RedisCommunityPostViewThrottle implements CommunityPostViewThrottle {
    private final StringRedisTemplate redisTemplate;

    @Value("${coin.community.view-throttle.key-prefix:community:post:view}")
    private String keyPrefix;

    @Override
    public boolean tryClaim(Long postId, Long actorMemberId, Duration window) {
        if (postId == null || actorMemberId == null || window == null || window.isZero() || window.isNegative()) {
            return false;
        }
        Boolean claimed = redisTemplate.opsForValue().setIfAbsent(key(postId, actorMemberId), "1", window);
        return Boolean.TRUE.equals(claimed);
    }

    private String key(Long postId, Long actorMemberId) {
        return keyPrefix + ":" + postId + ":" + actorMemberId;
    }
}
