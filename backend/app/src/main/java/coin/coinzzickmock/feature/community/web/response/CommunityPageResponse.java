package coin.coinzzickmock.feature.community.web.response;

import coin.coinzzickmock.feature.community.application.dto.CommunityCommentListResult;
import coin.coinzzickmock.feature.community.application.dto.CommunityPostListResult;

public record CommunityPageResponse(int page, int size, long totalElements, int totalPages, boolean hasNext) {
    public static CommunityPageResponse from(CommunityPostListResult result) {
        return new CommunityPageResponse(
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages(),
                result.hasNext()
        );
    }

    public static CommunityPageResponse from(CommunityCommentListResult result) {
        return new CommunityPageResponse(
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages(),
                result.hasNext()
        );
    }
}
