package coin.coinzzickmock.feature.community.web.response;

import coin.coinzzickmock.feature.community.application.result.CommunityPostSummaryResult;
import coin.coinzzickmock.feature.community.domain.CommunityCategory;
import java.time.Instant;

public record CommunityPostSummaryResponse(
        Long id,
        CommunityCategory category,
        String title,
        String authorNickname,
        long viewCount,
        long likeCount,
        long commentCount,
        Instant createdAt
) {
    public static CommunityPostSummaryResponse from(CommunityPostSummaryResult result) {
        return new CommunityPostSummaryResponse(
                result.id(),
                result.category(),
                result.title(),
                result.authorNickname(),
                result.viewCount(),
                result.likeCount(),
                result.commentCount(),
                result.createdAt()
        );
    }
}
