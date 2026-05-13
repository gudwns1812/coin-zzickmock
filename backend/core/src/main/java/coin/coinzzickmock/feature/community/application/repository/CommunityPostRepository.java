package coin.coinzzickmock.feature.community.application.repository;

import coin.coinzzickmock.feature.community.application.query.CommunityPostListQuery;
import coin.coinzzickmock.feature.community.domain.CommunityPost;
import java.util.List;
import java.util.Optional;

public interface CommunityPostRepository {
    List<CommunityPost> findLatestNotices(int limit);

    CommunityPostPage findNormalPosts(CommunityPostListQuery query);

    Optional<CommunityPost> findActiveById(Long postId);

    Optional<CommunityPost> findActiveByIdForUpdate(Long postId);

    CommunityPost save(CommunityPost post);

    long incrementCommentCount(Long postId);

    long incrementLikeCount(Long postId);

    long decrementLikeCount(Long postId);

    record CommunityPostPage(List<CommunityPost> posts, boolean hasNext) {
        public CommunityPostPage {
            posts = posts == null ? List.of() : List.copyOf(posts);
        }
    }
}
