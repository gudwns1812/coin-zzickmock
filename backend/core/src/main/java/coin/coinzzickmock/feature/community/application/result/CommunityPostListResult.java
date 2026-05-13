package coin.coinzzickmock.feature.community.application.result;

import coin.coinzzickmock.feature.community.application.repository.CommunityPostPage;
import coin.coinzzickmock.feature.community.domain.CommunityPost;
import java.util.List;
import java.util.Objects;

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
        Objects.requireNonNull(notices, "notices must not be null");
        Objects.requireNonNull(page, "page must not be null");
        return new CommunityPostListResult(
                notices.stream().map(CommunityPostSummaryResult::from).toList(),
                page.posts().stream().map(CommunityPostSummaryResult::from).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages(), page.hasNext()
        );
    }
}
