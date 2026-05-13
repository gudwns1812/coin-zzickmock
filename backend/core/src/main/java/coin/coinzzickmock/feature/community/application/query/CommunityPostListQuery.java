package coin.coinzzickmock.feature.community.application.query;

import coin.coinzzickmock.feature.community.domain.CommunityCategory;

public record CommunityPostListQuery(
        CommunityCategory category,
        int page,
        int size
) {
    public CommunityPostListQuery {
        if (page < 0) {
            page = 0;
        }
        if (size < 1) {
            size = 20;
        }
        if (size > 100) {
            size = 100;
        }
    }
}
