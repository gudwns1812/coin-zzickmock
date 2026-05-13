package coin.coinzzickmock.feature.community.domain;

public record CommunityPostLike(Long postId, Long memberId) {
    public CommunityPostLike {
        if (postId == null || postId <= 0 || memberId == null || memberId <= 0) {
            throw new IllegalArgumentException("postId and memberId must be positive");
        }
    }
}
