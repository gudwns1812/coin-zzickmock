package coin.coinzzickmock.feature.community.web.response;

import coin.coinzzickmock.feature.community.application.dto.CommunityPostDetailResult;
import coin.coinzzickmock.feature.community.domain.CommunityCategory;
import java.time.Instant;

public record CommunityPostDetailResponse(
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
    public static CommunityPostDetailResponse from(CommunityPostDetailResult result) {
        return new CommunityPostDetailResponse(
                result.id(),
                result.category(),
                result.title(),
                result.authorNickname(),
                result.contentJson(),
                result.viewCount(),
                result.likeCount(),
                result.commentCount(),
                result.canEdit(),
                result.canDelete(),
                result.isLikedByMe(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
