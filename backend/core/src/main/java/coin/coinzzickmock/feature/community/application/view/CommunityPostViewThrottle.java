package coin.coinzzickmock.feature.community.application.view;

import java.time.Duration;

@FunctionalInterface
public interface CommunityPostViewThrottle {
    boolean tryClaim(Long postId, Long actorMemberId, Duration window);

    static CommunityPostViewThrottle alwaysClaim() {
        return (postId, actorMemberId, window) -> true;
    }
}
