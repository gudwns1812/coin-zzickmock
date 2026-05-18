package coin.coinzzickmock.feature.community.web.response;

import coin.coinzzickmock.feature.community.application.dto.CommunityLikeResult;

public record CommunityLikeResponse(Long postId, boolean likedByMe) {
    public static CommunityLikeResponse from(CommunityLikeResult result) {
        return new CommunityLikeResponse(result.postId(), result.isLikedByMe());
    }
}
