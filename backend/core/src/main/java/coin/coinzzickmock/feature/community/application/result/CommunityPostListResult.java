package coin.coinzzickmock.feature.community.application.result;

import java.util.List;

public record CommunityPostListResult(List<CommunityPostSummaryResult> notices, List<CommunityPostSummaryResult> posts,
                                      int page, int size, boolean hasNext) {
    public CommunityPostListResult {
        notices = notices == null ? List.of() : List.copyOf(notices);
        posts = posts == null ? List.of() : List.copyOf(posts);
    }
}
