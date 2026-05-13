package coin.coinzzickmock.feature.community.application.result;

import coin.coinzzickmock.feature.community.domain.CommunityCategory;
import coin.coinzzickmock.feature.community.domain.CommunityPermissionPolicy;
import coin.coinzzickmock.feature.community.domain.CommunityPost;
import java.time.Instant;
import java.util.Objects;

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
        boolean isLikedByMe
) {
    public static CommunityPostResult from(CommunityPost post, Long viewerMemberId, boolean isViewerAdmin, boolean isLikedByMe) {
        Objects.requireNonNull(post, "post must not be null");
        boolean isAuthor = Objects.equals(viewerMemberId, post.authorMemberId());
        CommunityCategory currentCategory = post.category();
        return new CommunityPostResult(
                post.id(), post.authorMemberId(), post.authorNickname(), post.category(), post.title(),
                post.content().value(), post.viewCount(), post.likeCount(), post.commentCount(),
                post.createdAt(), post.updatedAt(),
                CommunityPermissionPolicy.canEditPost(isViewerAdmin, isAuthor, currentCategory, currentCategory),
                CommunityPermissionPolicy.canDeletePost(isViewerAdmin, isAuthor),
                isLikedByMe
        );
    }
}
