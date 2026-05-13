package coin.coinzzickmock.feature.community.application.result;

import coin.coinzzickmock.feature.community.domain.CommunityComment;
import java.util.Objects;

public record CommunityCommentMutationResult(Long commentId) {
    public static CommunityCommentMutationResult from(CommunityComment comment) {
        Objects.requireNonNull(comment, "comment must not be null");
        return new CommunityCommentMutationResult(Objects.requireNonNull(comment.id(), "comment.id must not be null"));
    }
}
