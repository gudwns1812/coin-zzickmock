package coin.coinzzickmock.feature.community.web.response;

import coin.coinzzickmock.feature.community.application.result.CommunityPostMutationResult;

public record CommunityPostMutationResponse(Long postId) {
    public static CommunityPostMutationResponse from(CommunityPostMutationResult result) {
        return new CommunityPostMutationResponse(result.postId());
    }
}
