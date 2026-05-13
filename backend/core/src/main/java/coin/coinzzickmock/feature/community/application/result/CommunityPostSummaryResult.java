package coin.coinzzickmock.feature.community.application.result;

import coin.coinzzickmock.feature.community.domain.CommunityCategory;
import coin.coinzzickmock.feature.community.domain.CommunityPost;
import java.time.Instant;

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
        return new CommunityPostSummaryResult(post.id(), post.category(), post.title(), post.authorNickname(),
                post.viewCount(), post.likeCount(), post.commentCount(), post.createdAt());
    }
}
