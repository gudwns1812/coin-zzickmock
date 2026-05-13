package coin.coinzzickmock.feature.community.application.result;

import coin.coinzzickmock.feature.community.domain.CommunityComment;

public record CommunityCommentMutationResult(Long commentId) {
    public static CommunityCommentMutationResult from(CommunityComment comment) {
        return new CommunityCommentMutationResult(comment.id());
    }
}
