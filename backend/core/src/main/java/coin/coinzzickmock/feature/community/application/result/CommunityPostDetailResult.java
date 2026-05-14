package coin.coinzzickmock.feature.community.application.result;

import coin.coinzzickmock.feature.community.domain.CommunityCategory;
import coin.coinzzickmock.feature.community.domain.CommunityPermissionPolicy;
import coin.coinzzickmock.feature.community.domain.CommunityPost;
import java.time.Instant;
import java.util.Objects;

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
        boolean isLikedByMe,
        Instant createdAt,
        Instant updatedAt
) {
    public static CommunityPostDetailResult from(CommunityPost post, Long actorMemberId, boolean isActorAdmin, boolean isLikedByMe) {
        Objects.requireNonNull(post, "post must not be null");
        boolean isAuthor = Objects.equals(actorMemberId, post.authorMemberId());
        CommunityCategory currentCategory = post.category();
        return new CommunityPostDetailResult(post.id(), post.category(), post.title(), post.authorNickname(), post.content().value(),
                post.viewCount(), post.likeCount(), post.commentCount(),
                CommunityPermissionPolicy.canEditPost(isActorAdmin, isAuthor, currentCategory, currentCategory),
                CommunityPermissionPolicy.canDeletePost(isActorAdmin, isAuthor), isLikedByMe, post.createdAt(), post.updatedAt());
    }
}
