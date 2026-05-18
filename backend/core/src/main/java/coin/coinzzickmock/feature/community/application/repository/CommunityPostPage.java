package coin.coinzzickmock.feature.community.application.repository;

import coin.coinzzickmock.feature.community.domain.CommunityPost;
import java.util.List;

public record CommunityPostPage(
        List<CommunityPost> posts,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public CommunityPostPage {
        posts = posts == null ? List.of() : List.copyOf(posts);
    }
}
