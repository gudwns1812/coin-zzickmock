package coin.coinzzickmock.feature.community.domain;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import java.time.Instant;

public record CommunityLike(Long postId, Long memberId, Instant createdAt) {
    public CommunityLike {
        if (postId == null || postId <= 0 || memberId == null || memberId <= 0 || createdAt == null) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
