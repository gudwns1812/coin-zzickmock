package coin.coinzzickmock.feature.community.application.result;

import coin.coinzzickmock.feature.community.domain.CommunityComment;
import coin.coinzzickmock.feature.community.domain.CommunityPermissionPolicy;
import java.time.Instant;

public record CommunityCommentResult(
        Long id,
        Long postId,
        String authorNickname,
        String content,
        boolean canDelete,
        Instant createdAt
) {
    public static CommunityCommentResult from(CommunityComment comment, Long actorMemberId, boolean actorAdmin) {
        boolean author = comment.authorMemberId().equals(actorMemberId);
        return new CommunityCommentResult(comment.id(), comment.postId(), comment.authorNickname(), comment.content(),
                CommunityPermissionPolicy.canDeleteComment(actorAdmin, author), comment.createdAt());
    }
}
