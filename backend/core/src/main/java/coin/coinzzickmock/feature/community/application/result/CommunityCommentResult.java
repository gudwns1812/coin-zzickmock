package coin.coinzzickmock.feature.community.application.result;

import coin.coinzzickmock.feature.community.domain.CommunityComment;
import coin.coinzzickmock.feature.community.domain.CommunityPermissionPolicy;
import java.time.Instant;

public record CommunityCommentResult(Long id, Long postId, Long authorMemberId, String authorNickname, String content,
                                     Instant createdAt, Instant updatedAt, boolean canDelete) {
    public static CommunityCommentResult from(CommunityComment comment, Long viewerMemberId, boolean viewerAdmin) {
        boolean author = viewerMemberId != null && viewerMemberId.equals(comment.authorMemberId());
        return new CommunityCommentResult(comment.id(), comment.postId(), comment.authorMemberId(), comment.authorNickname(),
                comment.content(), comment.createdAt(), comment.updatedAt(),
                CommunityPermissionPolicy.canDeleteComment(viewerAdmin, author));
    }
}
