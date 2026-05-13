package coin.coinzzickmock.feature.community.application.repository;

import coin.coinzzickmock.feature.community.application.result.CommunityLikeResult;

public interface CommunityLikeRepository {
    boolean exists(Long postId, Long memberId);

    CommunityLikeResult addIfAbsent(Long postId, Long memberId);

    CommunityLikeResult removeIfPresent(Long postId, Long memberId);
}
