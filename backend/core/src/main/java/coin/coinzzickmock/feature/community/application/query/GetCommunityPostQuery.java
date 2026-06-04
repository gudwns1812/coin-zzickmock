package coin.coinzzickmock.feature.community.application.query;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.community.application.view.CommunityPostReadIntent;

public record GetCommunityPostQuery(Long postId, Long actorMemberId, boolean isActorAdmin, CommunityPostReadIntent intent) {
    public GetCommunityPostQuery(Long postId, Long actorMemberId, boolean isActorAdmin) {
        this(postId, actorMemberId, isActorAdmin, CommunityPostReadIntent.DETAIL);
    }

    public GetCommunityPostQuery {
        if (postId == null || postId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (intent == null) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
