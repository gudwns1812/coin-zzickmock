package coin.coinzzickmock.feature.community.application.result;

import coin.coinzzickmock.feature.community.domain.CommunityPost;
import java.util.Objects;

public record CommunityPostMutationResult(Long postId) {
    public static CommunityPostMutationResult from(CommunityPost post) {
        Objects.requireNonNull(post, "post must not be null");
        return new CommunityPostMutationResult(Objects.requireNonNull(post.id(), "post.id must not be null"));
    }
}
