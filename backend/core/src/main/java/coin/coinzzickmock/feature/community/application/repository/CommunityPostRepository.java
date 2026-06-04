package coin.coinzzickmock.feature.community.application.repository;

import coin.coinzzickmock.feature.community.application.dto.CommunityPostCountDelta;
import coin.coinzzickmock.feature.community.application.query.ListCommunityPostsQuery;
import coin.coinzzickmock.feature.community.domain.CommunityPost;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CommunityPostRepository {
    List<CommunityPost> findLatestNotices(int limit);

    CommunityPostPage findPosts(ListCommunityPostsQuery query);

    Optional<CommunityPost> findActiveById(Long postId);

    CommunityPost save(CommunityPost post);

    CommunityPost update(CommunityPost post);

    void softDelete(Long postId, Instant deletedAt);

    void incrementViewCount(Long postId);

    void applyCountDeltas(Collection<CommunityPostCountDelta> deltas);
}
