package coin.coinzzickmock.feature.community.web.response;

import coin.coinzzickmock.feature.community.application.dto.CommunityCommentListResult;
import java.util.List;

public record CommunityCommentListResponse(List<CommunityCommentResponse> comments, CommunityPageResponse page) {
    public static CommunityCommentListResponse from(CommunityCommentListResult result) {
        return new CommunityCommentListResponse(
                result.comments().stream()
                        .map(CommunityCommentResponse::from)
                        .toList(),
                CommunityPageResponse.from(result)
        );
    }
}
