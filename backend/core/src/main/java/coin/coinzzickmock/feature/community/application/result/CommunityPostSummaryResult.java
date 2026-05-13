package coin.coinzzickmock.feature.community.application.result;

import coin.coinzzickmock.feature.community.domain.CommunityCategory;
import coin.coinzzickmock.feature.community.domain.CommunityPost;
import java.time.Instant;
import java.util.Objects;

public record CommunityPostSummaryResult(
        Long id,
        CommunityCategory category,
        String title,
        String authorNickname,
        long viewCount,
        long likeCount,
        long commentCount,
        Instant createdAt
) {
    public static CommunityPostSummaryResult from(CommunityPost post) {
        Objects.requireNonNull(post, "post must not be null");
        return new CommunityPostSummaryResult(post.id(), post.category(), post.title(), post.authorNickname(),
                post.viewCount(), post.likeCount(), post.commentCount(), post.createdAt());
    }
}
