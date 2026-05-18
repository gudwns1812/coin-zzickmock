package coin.coinzzickmock.feature.community.application.repository;

import coin.coinzzickmock.feature.community.domain.CommunityComment;
import java.util.List;

public record CommunityCommentPage(
        List<CommunityComment> comments,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public CommunityCommentPage {
        comments = comments == null ? List.of() : List.copyOf(comments);
    }
}
