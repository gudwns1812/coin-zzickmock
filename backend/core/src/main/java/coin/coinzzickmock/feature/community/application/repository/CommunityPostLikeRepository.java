package coin.coinzzickmock.feature.community.application.repository;

public interface CommunityPostLikeRepository {
    boolean exists(Long postId, Long memberId);

    boolean addIfAbsent(Long postId, Long memberId);

    boolean removeIfPresent(Long postId, Long memberId);
}
