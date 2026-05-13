package coin.coinzzickmock.feature.community.application.result;

import coin.coinzzickmock.feature.community.domain.CommunityCategory;
import coin.coinzzickmock.feature.community.domain.CommunityPermissionPolicy;
import coin.coinzzickmock.feature.community.domain.CommunityPost;
import java.time.Instant;

public record CommunityPostDetailResult(
        Long id,
        CommunityCategory category,
        String title,
        String authorNickname,
        String contentJson,
        long viewCount,
        long likeCount,
        long commentCount,
        boolean canEdit,
        boolean canDelete,
        boolean likedByMe,
        Instant createdAt,
        Instant updatedAt
) {
    public static CommunityPostDetailResult from(CommunityPost post, Long actorMemberId, boolean actorAdmin, boolean likedByMe) {
        boolean author = post.authorMemberId().equals(actorMemberId);
        return new CommunityPostDetailResult(post.id(), post.category(), post.title(), post.authorNickname(), post.content().value(),
                post.viewCount(), post.likeCount(), post.commentCount(),
                CommunityPermissionPolicy.canEditPost(actorAdmin, author, post.category(), post.category()),
                CommunityPermissionPolicy.canDeletePost(actorAdmin, author), likedByMe, post.createdAt(), post.updatedAt());
    }
}
