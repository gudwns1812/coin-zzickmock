package coin.coinzzickmock.feature.community.application.result;

import coin.coinzzickmock.feature.community.application.repository.CommunityCommentPage;
import java.util.List;

public record CommunityCommentListResult(
        List<CommunityCommentResult> comments,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public static CommunityCommentListResult from(CommunityCommentPage page, Long actorMemberId, boolean isActorAdmin) {
        return new CommunityCommentListResult(page.comments().stream()
                .map(comment -> CommunityCommentResult.from(comment, actorMemberId, isActorAdmin)).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages(), page.hasNext());
    }
}
