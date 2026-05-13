package coin.coinzzickmock.feature.community.application.result;

import coin.coinzzickmock.feature.community.domain.CommunityCategory;
import coin.coinzzickmock.feature.community.domain.CommunityPermissionPolicy;
import coin.coinzzickmock.feature.community.domain.CommunityPost;
import java.time.Instant;

public record CommunityPostResult(
        Long id,
        Long authorMemberId,
        String authorNickname,
        CommunityCategory category,
        String title,
        String contentJson,
        long viewCount,
        long likeCount,
        long commentCount,
        Instant createdAt,
        Instant updatedAt,
        boolean canEdit,
        boolean canDelete,
        boolean likedByMe
) {
    public static CommunityPostResult from(CommunityPost post, Long viewerMemberId, boolean viewerAdmin, boolean likedByMe) {
        boolean author = viewerMemberId != null && viewerMemberId.equals(post.authorMemberId());
        return new CommunityPostResult(
                post.id(), post.authorMemberId(), post.authorNickname(), post.category(), post.title(),
                post.content().value(), post.viewCount(), post.likeCount(), post.commentCount(),
                post.createdAt(), post.updatedAt(),
                CommunityPermissionPolicy.canEditPost(viewerAdmin, author, post.category(), post.category()),
                CommunityPermissionPolicy.canDeletePost(viewerAdmin, author),
                likedByMe
        );
    }
}
