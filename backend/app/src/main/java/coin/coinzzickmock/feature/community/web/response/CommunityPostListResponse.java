package coin.coinzzickmock.feature.community.web.response;

import coin.coinzzickmock.feature.community.application.result.CommunityPostListResult;
import java.util.List;

public record CommunityPostListResponse(
        List<CommunityPostSummaryResponse> pinnedNotices,
        List<CommunityPostSummaryResponse> posts,
        CommunityPageResponse page
) {
    public static CommunityPostListResponse from(CommunityPostListResult result) {
        return new CommunityPostListResponse(
                result.pinnedNotices().stream()
                        .map(CommunityPostSummaryResponse::from)
                        .toList(),
                result.posts().stream()
                        .map(CommunityPostSummaryResponse::from)
                        .toList(),
                CommunityPageResponse.from(result)
        );
    }
}
