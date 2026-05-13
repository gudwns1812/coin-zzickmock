package coin.coinzzickmock.feature.community.application.result;

import java.util.List;

public record CommunityCommentListResult(List<CommunityCommentResult> comments, int page, int size, boolean hasNext) {
    public CommunityCommentListResult {
        comments = comments == null ? List.of() : List.copyOf(comments);
    }
}
