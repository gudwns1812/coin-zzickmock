package coin.coinzzickmock.feature.community.application.result;

import coin.coinzzickmock.feature.community.application.repository.CommunityPostPage;
import coin.coinzzickmock.feature.community.domain.CommunityPost;
import java.util.List;

public record CommunityPostListResult(
        List<CommunityPostSummaryResult> pinnedNotices,
        List<CommunityPostSummaryResult> posts,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public static CommunityPostListResult from(List<CommunityPost> notices, CommunityPostPage page) {
        return new CommunityPostListResult(
                notices.stream().map(CommunityPostSummaryResult::from).toList(),
                page.posts().stream().map(CommunityPostSummaryResult::from).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages(), page.hasNext()
        );
    }
}
