package coin.coinzzickmock.feature.community.application.command;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;

public record DeleteCommunityCommentCommand(Long postId, Long commentId, Long actorMemberId, boolean isActorAdmin) {
    public DeleteCommunityCommentCommand {
        if (postId == null || postId <= 0 || commentId == null || commentId <= 0
                || actorMemberId == null || actorMemberId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
