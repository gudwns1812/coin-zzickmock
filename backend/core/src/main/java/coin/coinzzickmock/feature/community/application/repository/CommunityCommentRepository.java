package coin.coinzzickmock.feature.community.application.repository;

import coin.coinzzickmock.feature.community.domain.CommunityComment;
import java.util.List;
import java.util.Optional;

public interface CommunityCommentRepository {
    CommunityCommentPage findActiveByPostId(Long postId, int page, int size);

    Optional<CommunityComment> findActiveById(Long commentId);

    Optional<CommunityComment> findActiveByIdForUpdate(Long commentId);

    CommunityComment save(CommunityComment comment);

    record CommunityCommentPage(List<CommunityComment> comments, boolean hasNext) {
        public CommunityCommentPage {
            comments = comments == null ? List.of() : List.copyOf(comments);
        }
    }
}
