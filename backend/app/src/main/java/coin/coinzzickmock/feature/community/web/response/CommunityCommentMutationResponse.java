package coin.coinzzickmock.feature.community.web.response;

import coin.coinzzickmock.feature.community.application.result.CommunityCommentMutationResult;

public record CommunityCommentMutationResponse(Long commentId) {
    public static CommunityCommentMutationResponse from(CommunityCommentMutationResult result) {
        return new CommunityCommentMutationResponse(result.commentId());
    }
}
