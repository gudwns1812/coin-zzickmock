package coin.coinzzickmock.feature.community.application.result;

import coin.coinzzickmock.feature.community.domain.CommunityPost;

public record CommunityPostMutationResult(Long postId) {
    public static CommunityPostMutationResult from(CommunityPost post) {
        return new CommunityPostMutationResult(post.id());
    }
}
