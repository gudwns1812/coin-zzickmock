package coin.coinzzickmock.feature.community.application.result;

import coin.coinzzickmock.feature.community.domain.CommunityComment;
import coin.coinzzickmock.feature.community.domain.CommunityPermissionPolicy;
import java.time.Instant;
import java.util.Objects;

public record CommunityCommentResult(
        Long id,
        Long postId,
        String authorNickname,
        String content,
        boolean canDelete,
        Instant createdAt
) {
    public static CommunityCommentResult from(CommunityComment comment, Long actorMemberId, boolean isActorAdmin) {
        Objects.requireNonNull(comment, "comment must not be null");
        boolean isAuthor = Objects.equals(actorMemberId, comment.authorMemberId());
        return new CommunityCommentResult(comment.id(), comment.postId(), comment.authorNickname(), comment.content(),
                CommunityPermissionPolicy.canDeleteComment(isActorAdmin, isAuthor), comment.createdAt());
    }
}
