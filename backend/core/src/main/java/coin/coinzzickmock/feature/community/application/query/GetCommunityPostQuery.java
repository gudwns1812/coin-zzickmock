package coin.coinzzickmock.feature.community.application.query;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;

public record GetCommunityPostQuery(Long postId, Long actorMemberId, boolean isActorAdmin) {
    public GetCommunityPostQuery {
        if (postId == null || postId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
