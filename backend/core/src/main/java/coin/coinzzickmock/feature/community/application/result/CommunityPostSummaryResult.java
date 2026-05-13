package coin.coinzzickmock.feature.community.application.result;

import coin.coinzzickmock.feature.community.domain.CommunityCategory;
import coin.coinzzickmock.feature.community.domain.CommunityPost;
import java.time.Instant;

public record CommunityPostSummaryResult(
        Long id,
        Long authorMemberId,
        String authorNickname,
        CommunityCategory category,
        String title,
        long viewCount,
        long likeCount,
        long commentCount,
        Instant createdAt
) {
    public static CommunityPostSummaryResult from(CommunityPost post) {
        return new CommunityPostSummaryResult(
                post.id(), post.authorMemberId(), post.authorNickname(), post.category(), post.title(),
                post.viewCount(), post.likeCount(), post.commentCount(), post.createdAt()
        );
    }
}
