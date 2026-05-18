package coin.coinzzickmock.feature.community.domain;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;

public record CommunityPostLike(Long postId, Long memberId) {
    public CommunityPostLike {
        if (postId == null || postId <= 0 || memberId == null || memberId <= 0) {
            throw invalid();
        }
    }

    private static CoreException invalid() {
        return new CoreException(ErrorCode.INVALID_REQUEST);
    }
}
