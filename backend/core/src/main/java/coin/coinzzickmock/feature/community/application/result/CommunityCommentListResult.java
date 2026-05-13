package coin.coinzzickmock.feature.community.application.result;

import coin.coinzzickmock.feature.community.application.repository.CommunityCommentPage;
import java.util.List;

public record CommunityCommentListResult(List<CommunityCommentResult> comments, int page, int size, boolean hasNext) {
    public CommunityCommentListResult {
        comments = comments == null ? List.of() : List.copyOf(comments);
    }

    public static CommunityCommentListResult from(CommunityCommentPage page, Long actorMemberId, boolean actorAdmin) {
        return new CommunityCommentListResult(
                page.comments().stream()
                        .map(comment -> CommunityCommentResult.from(comment, actorMemberId, actorAdmin))
                        .toList(),
                page.page(),
                page.size(),
                page.hasNext()
        );
    }
}
